package pe.losmatus.itsmevaluator.assignment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.losmatus.itsmevaluator.domain.ResultStatus;
import pe.losmatus.itsmevaluator.persistence.*;

@Service
public class AssignmentSimulationService {
    public static final String DEFAULT_POLICY = "controlled-simulation-v2";

    private final EvaluationRunRepository evaluationRuns;
    private final EvaluationResultRepository evaluationResults;
    private final TechnicianRepository technicians;
    private final TechnicianSkillRepository skills;
    private final AssignmentPolicyWeightRepository policyWeights;
    private final AssignmentAffinityThresholdRepository affinityThresholds;
    private final AssignmentExperimentRepository experiments;
    private final AssignmentRunRepository assignmentRuns;
    private final AssignmentCandidateScoreRepository candidateScores;
    private final ObjectMapper objectMapper;
    private final AdaptiveAssignmentEngine engine = new AdaptiveAssignmentEngine();

    public AssignmentSimulationService(EvaluationRunRepository evaluationRuns,
                                       EvaluationResultRepository evaluationResults,
                                       TechnicianRepository technicians,
                                       TechnicianSkillRepository skills,
                                       AssignmentPolicyWeightRepository policyWeights,
                                       AssignmentAffinityThresholdRepository affinityThresholds,
                                       AssignmentExperimentRepository experiments,
                                       AssignmentRunRepository assignmentRuns,
                                       AssignmentCandidateScoreRepository candidateScores,
                                       ObjectMapper objectMapper) {
        this.evaluationRuns = evaluationRuns;
        this.evaluationResults = evaluationResults;
        this.technicians = technicians;
        this.skills = skills;
        this.policyWeights = policyWeights;
        this.affinityThresholds = affinityThresholds;
        this.experiments = experiments;
        this.assignmentRuns = assignmentRuns;
        this.candidateScores = candidateScores;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SimulationResponse simulate(SimulationRequest request) {
        if (request == null || request.evaluationRunId() == null) {
            throw new IllegalArgumentException("evaluationRunId es obligatorio");
        }
        evaluationRuns.findById(request.evaluationRunId())
                .orElseThrow(() -> new NoSuchElementException("Ejecución de clasificación no encontrada"));
        String policyVersion = valueOrDefault(request.policyVersion(), DEFAULT_POLICY);
        AssignmentStrategy strategy = request.strategy() == null
                ? AssignmentStrategy.MULTICRITERIA : request.strategy();
        String scenario = valueOrDefault(request.scenario(), "E1_CARGA_CERO_TODOS_DISPONIBLES");

        var sourceResults = evaluationResults.findAllByRunIdOrderByTicketId(request.evaluationRunId()).stream()
                .filter(result -> result.getStatus() == ResultStatus.SUCCESS)
                .filter(result -> result.getPredictedCategoryId() != null && result.getPredictedSeverityId() != null)
                .toList();
        if (sourceResults.isEmpty()) {
            throw new IllegalArgumentException("La ejecución no contiene clasificaciones válidas");
        }
        var activeTechnicians = technicians.findAllByActiveTrueOrderById();
        if (activeTechnicians.isEmpty()) throw new IllegalStateException("No existen técnicos activos");

        Map<Long, Map<Integer, Double>> affinityMatrix = affinityMatrix(activeTechnicians);
        Map<Integer, AssignmentWeights> weightMatrix = weightMatrix(policyVersion);
        Map<Integer, Double> thresholdMatrix = thresholdMatrix(policyVersion);
        Map<Long, Double> workload = initialWorkload(activeTechnicians, request.initialLoads());
        Map<Long, Boolean> availability = initialAvailability(activeTechnicians,
                request.availabilityOverrides());

        var experiment = experiments.save(new AssignmentExperimentEntity(request.evaluationRunId(),
                policyVersion, strategy, scenario, sourceResults.size()));
        Map<Long, Integer> assignmentCounts = new LinkedHashMap<>();
        activeTechnicians.forEach(technician -> assignmentCounts.put(technician.getId(), 0));
        double suitabilitySum = 0;
        double affinitySum = 0;
        double availabilitySum = 0;
        double timeSum = 0;
        List<Double> assignmentTimes = new ArrayList<>();
        int assigned = 0;
        int validAssigned = 0;
        int highCriticalTotal = 0;
        int highCriticalSpecialized = 0;
        long roundRobinPosition = 0;

        for (var result : sourceResults) {
            if (result.getPredictedSeverityId() >= 3) highCriticalTotal++;
            long started = System.nanoTime();
            var profiles = activeTechnicians.stream().map(technician -> new TechnicianProfile(
                    technician.getId(), technician.getCode(), technician.getDisplayName(),
                    affinityMatrix.getOrDefault(technician.getId(), Map.of())
                            .getOrDefault(result.getPredictedCategoryId(), 0.0),
                    availability.getOrDefault(technician.getId(), false),
                    workload.getOrDefault(technician.getId(), 0.0))).toList();
            AssignmentWeights weights = Optional.ofNullable(weightMatrix.get(result.getPredictedSeverityId()))
                    .orElseThrow(() -> new IllegalStateException(
                            "No existen pesos activos para severidad " + result.getPredictedSeverityId()));
            double minimumAffinity = Optional.ofNullable(thresholdMatrix.get(result.getPredictedSeverityId()))
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe afinidad mínima para severidad " + result.getPredictedSeverityId()));
            AssignmentDecision decision = engine.assign(profiles, weights, strategy,
                    roundRobinPosition++, minimumAffinity);
            double elapsedMs = (System.nanoTime() - started) / 1_000_000.0;
            var assignment = assignmentRuns.save(new AssignmentRunEntity(experiment.getId(), result.getId(),
                    result.getTicketId(), decision.selectedTechnicianId(), policyVersion,
                    decision.selectedTechnicianId() == null ? "UNASSIGNED" : "ASSIGNED",
                    Math.round(elapsedMs)));
            candidateScores.saveAll(decision.candidates().stream()
                    .map(score -> new AssignmentCandidateScoreEntity(assignment.getId(), score)).toList());

            if (decision.selectedTechnicianId() != null) {
                assigned++;
                timeSum += elapsedMs;
                assignmentTimes.add(elapsedMs);
                assignmentCounts.compute(decision.selectedTechnicianId(), (id, count) -> count + 1);
                double loadUnits = severityLoad(result.getPredictedSeverityId());
                workload.merge(decision.selectedTechnicianId(), loadUnits, Double::sum);
                var selected = decision.candidates().stream().filter(CandidateScore::selected).findFirst().orElseThrow();
                suitabilitySum += selected.suitabilityIndex();
                affinitySum += selected.affinity();
                availabilitySum += selected.availability();
                if (selected.eligible() && selected.rejectionReason() == null) validAssigned++;
                if (result.getPredictedSeverityId() >= 3 && selected.affinity() >= 0.75) {
                    highCriticalSpecialized++;
                }
            }
        }

        int unassigned = sourceResults.size() - assigned;
        double assignmentRate = sourceResults.isEmpty() ? 0 : (double) assigned / sourceResults.size();
        double validRate = assigned == 0 ? 0 : (double) validAssigned / assigned;
        double meanSuitability = assigned == 0 ? 0 : suitabilitySum / assigned;
        double meanAffinity = assigned == 0 ? 0 : affinitySum / assigned;
        double meanAvailability = assigned == 0 ? 0 : availabilitySum / assigned;
        double meanTime = assigned == 0 ? 0 : timeSum / assigned;
        double p95Time = AssignmentMetrics.percentile(assignmentTimes, 0.95);
        List<Double> availableWorkloads = activeTechnicians.stream()
        .filter(technician ->
                availability.getOrDefault(technician.getId(), false))
        .map(technician ->
                workload.getOrDefault(technician.getId(), 0.0))
        .toList();

double standardDeviation =
        AssignmentMetrics.standardDeviation(availableWorkloads);

double workloadRange =
        AssignmentMetrics.range(availableWorkloads);

double jainIndex =
        AssignmentMetrics.jainFairnessIndex(availableWorkloads);
        double highCriticalRate = highCriticalTotal == 0 ? 0
                : (double) highCriticalSpecialized / highCriticalTotal;
        Map<String, Object> distribution = distribution(activeTechnicians, assignmentCounts, workload);
        experiment.complete(assigned, unassigned, assignmentRate, validRate,
                meanSuitability, meanAffinity, meanAvailability, meanTime, p95Time,
                standardDeviation, workloadRange, jainIndex, highCriticalRate,
                toJson(distribution));
        experiments.save(experiment);
        return new SimulationResponse(experiment.getId(), request.evaluationRunId(), strategy,
                policyVersion, scenario, sourceResults.size(), assigned, unassigned,
                assignmentRate, validRate, meanSuitability, meanAffinity, meanAvailability,
                meanTime, p95Time, standardDeviation, workloadRange, jainIndex,
                highCriticalRate, distribution);
    }

    private Map<Long, Map<Integer, Double>> affinityMatrix(List<TechnicianEntity> active) {
        var matrix = new HashMap<Long, Map<Integer, Double>>();
        skills.findAllByTechnicianIdIn(active.stream().map(TechnicianEntity::getId).toList())
                .forEach(skill -> matrix.computeIfAbsent(skill.getTechnicianId(), ignored -> new HashMap<>())
                        .put(skill.getCategoryId(), skill.getAffinityScore().doubleValue()));
        return matrix;
    }

    private Map<Integer, AssignmentWeights> weightMatrix(String version) {
        var matrix = new HashMap<Integer, AssignmentWeights>();
        policyWeights.findAllByPolicyVersionOrderBySeverityId(version)
                .stream().filter(AssignmentPolicyWeightEntity::isActive)
                .forEach(weight -> matrix.put(weight.getSeverityId(), weight.toWeights()));
        if (matrix.size() != 4) {
            throw new IllegalStateException("La política debe contener pesos activos para las cuatro severidades");
        }
        return matrix;
    }

    private Map<Integer, Double> thresholdMatrix(String version) {
        var matrix = new HashMap<Integer, Double>();
        affinityThresholds.findAllByPolicyVersionOrderBySeverityId(version).stream()
                .filter(AssignmentAffinityThresholdEntity::isActive)
                .forEach(threshold -> matrix.put(threshold.getSeverityId(),
                        threshold.getMinimumAffinity().doubleValue()));
        if (matrix.size() != 4) {
            throw new IllegalStateException("La política debe contener umbrales para las cuatro severidades");
        }
        return matrix;
    }

    private Map<Long, Double> initialWorkload(List<TechnicianEntity> active,
                                              Map<String, Double> overrides) {
        var result = new LinkedHashMap<Long, Double>();
        for (var technician : active) {
            double value = overrides == null ? 0 : overrides.getOrDefault(technician.getCode(), 0.0);
            if (value < 0) throw new IllegalArgumentException("La carga inicial no puede ser negativa");
            result.put(technician.getId(), value);
        }
        return result;
    }

    private Map<Long, Boolean> initialAvailability(List<TechnicianEntity> active,
                                                   Map<String, Boolean> overrides) {
        var result = new LinkedHashMap<Long, Boolean>();
        for (var technician : active) {
            boolean defaultValue = technician.isAvailable();
            boolean value = overrides == null ? defaultValue
                    : overrides.getOrDefault(technician.getCode(), defaultValue);
            result.put(technician.getId(), value);
        }
        return result;
    }

    private double severityLoad(Integer severityId) {
        return switch (severityId) {
            case 1 -> 1.0;
            case 2 -> 2.0;
            case 3 -> 3.0;
            case 4 -> 4.0;
            default -> throw new IllegalArgumentException("Severidad fuera de rango");
        };
    }

    private Map<String, Object> distribution(List<TechnicianEntity> active,
                                             Map<Long, Integer> counts,
                                             Map<Long, Double> workload) {
        var distribution = new LinkedHashMap<String, Object>();
        for (var technician : active) {
            distribution.put(technician.getCode(), Map.of(
                    "technicianName", technician.getDisplayName(),
                    "assignedTickets", counts.getOrDefault(technician.getId(), 0),
                    "finalWeightedLoad", workload.getOrDefault(technician.getId(), 0.0)));
        }
        return distribution;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el resumen de distribución", ex);
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record SimulationRequest(Long evaluationRunId, String policyVersion,
                                    AssignmentStrategy strategy, String scenario,
                                    Map<String, Boolean> availabilityOverrides,
                                    Map<String, Double> initialLoads) {}

    public record SimulationResponse(Long experimentId, Long sourceEvaluationRunId,
                                     AssignmentStrategy strategy, String policyVersion,
                                     String scenario, int totalTickets, int assignedTickets,
                                     int unassignedTickets, double assignmentRate,
                                     double validAssignmentRate, double meanSuitability,
                                     double meanAffinity, double meanAvailability,
                                     double meanAssignmentTimeMs, double assignmentTimeP95Ms,
                                     double workloadStdDeviation, double workloadRange,
                                     double jainFairnessIndex,
                                     double highCriticalSpecializedRate,
                                     Map<String, Object> distribution) {}
}

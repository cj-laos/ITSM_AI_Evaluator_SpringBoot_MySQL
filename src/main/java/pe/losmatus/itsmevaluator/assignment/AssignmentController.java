package pe.losmatus.itsmevaluator.assignment;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.losmatus.itsmevaluator.persistence.AssignmentExperimentEntity;
import pe.losmatus.itsmevaluator.persistence.AssignmentExperimentRepository;
import pe.losmatus.itsmevaluator.persistence.AssignmentCandidateScoreEntity;
import pe.losmatus.itsmevaluator.persistence.AssignmentCandidateScoreRepository;
import pe.losmatus.itsmevaluator.persistence.AssignmentRunEntity;
import pe.losmatus.itsmevaluator.persistence.AssignmentRunRepository;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
    private final AssignmentSimulationService simulations;
    private final AssignmentConfigurationService configuration;
    private final AssignmentExperimentRepository experiments;
    private final AssignmentRunRepository assignmentRuns;
    private final AssignmentCandidateScoreRepository candidateScores;

    public AssignmentController(AssignmentSimulationService simulations,
                                AssignmentConfigurationService configuration,
                                AssignmentExperimentRepository experiments,
                                AssignmentRunRepository assignmentRuns,
                                AssignmentCandidateScoreRepository candidateScores) {
        this.simulations = simulations;
        this.configuration = configuration;
        this.experiments = experiments;
        this.assignmentRuns = assignmentRuns;
        this.candidateScores = candidateScores;
    }

    @PostMapping("/simulations")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentSimulationService.SimulationResponse simulate(
            @RequestBody AssignmentSimulationService.SimulationRequest request) {
        return simulations.simulate(request);
    }

    @GetMapping("/experiments/{experimentId}")
    public AssignmentExperimentEntity experiment(@PathVariable Long experimentId) {
        return experiments.findById(experimentId).orElseThrow();
    }

    @GetMapping("/experiments")
    public List<AssignmentExperimentEntity> experiments(@RequestParam Long evaluationRunId) {
        return experiments.findAllBySourceEvaluationRunIdOrderByIdDesc(evaluationRunId);
    }

    @GetMapping("/experiments/{experimentId}/trace")
    public List<AssignmentTrace> trace(@PathVariable Long experimentId) {
        experiments.findById(experimentId).orElseThrow();
        return assignmentRuns.findAllByExperimentIdOrderById(experimentId).stream()
                .map(run -> new AssignmentTrace(run,
                        candidateScores.findAllByAssignmentRunIdOrderByTechnicianId(run.getId())))
                .toList();
    }

    @GetMapping("/configuration/technicians")
    public List<AssignmentConfigurationService.TechnicianConfiguration> technicians() {
        return configuration.technicians();
    }

    @PutMapping("/configuration/technicians/{technicianId}/skills/{categoryId}")
    public AssignmentConfigurationService.TechnicianConfiguration updateSkill(
            @PathVariable Long technicianId, @PathVariable Integer categoryId,
            @RequestBody SkillUpdateRequest request) {
        configuration.updateSkill(technicianId, categoryId, request.level(), request.validatedBy());
        return configuration.technicians().stream()
                .filter(technician -> technician.id().equals(technicianId)).findFirst().orElseThrow();
    }

    @PatchMapping("/configuration/technicians/{technicianId}/availability")
    public AssignmentConfigurationService.TechnicianConfiguration updateAvailability(
            @PathVariable Long technicianId, @RequestBody AvailabilityUpdateRequest request) {
        configuration.updateAvailability(technicianId, request.available());
        return configuration.technicians().stream()
                .filter(technician -> technician.id().equals(technicianId)).findFirst().orElseThrow();
    }

    @GetMapping("/configuration/policies/{policyVersion}")
    public List<AssignmentConfigurationService.WeightConfiguration> weights(
            @PathVariable String policyVersion) {
        return configuration.weights(policyVersion);
    }

    @PutMapping("/configuration/policies/{policyVersion}/severities/{severityId}")
    public AssignmentConfigurationService.WeightConfiguration updateWeights(
            @PathVariable String policyVersion, @PathVariable Integer severityId,
            @RequestBody WeightUpdateRequest request) {
        return configuration.updateWeights(policyVersion, severityId, request.affinity(),
                request.load());
    }

    @GetMapping("/configuration/policies/{policyVersion}/thresholds")
    public List<AssignmentConfigurationService.ThresholdConfiguration> thresholds(
            @PathVariable String policyVersion) {
        return configuration.thresholds(policyVersion);
    }

    @PutMapping("/configuration/policies/{policyVersion}/thresholds/{severityId}")
    public AssignmentConfigurationService.ThresholdConfiguration updateThreshold(
            @PathVariable String policyVersion, @PathVariable Integer severityId,
            @RequestBody ThresholdUpdateRequest request) {
        return configuration.updateThreshold(policyVersion, severityId, request.minimumAffinity());
    }

    public record SkillUpdateRequest(int level, String validatedBy) {}
    public record AvailabilityUpdateRequest(boolean available) {}
    public record WeightUpdateRequest(double affinity, double load) {}
    public record ThresholdUpdateRequest(double minimumAffinity) {}
    public record AssignmentTrace(AssignmentRunEntity assignment,
                                  List<AssignmentCandidateScoreEntity> candidates) {}
}

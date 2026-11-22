package pe.losmatus.itsmevaluator.assignment;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.losmatus.itsmevaluator.persistence.*;

@Service
public class AssignmentConfigurationService {
    private final TechnicianRepository technicians;
    private final TechnicianSkillRepository skills;
    private final AssignmentPolicyWeightRepository weights;
    private final AssignmentAffinityThresholdRepository thresholds;

    public AssignmentConfigurationService(TechnicianRepository technicians,
                                          TechnicianSkillRepository skills,
                                          AssignmentPolicyWeightRepository weights,
                                          AssignmentAffinityThresholdRepository thresholds) {
        this.technicians = technicians;
        this.skills = skills;
        this.weights = weights;
        this.thresholds = thresholds;
    }

    @Transactional(readOnly = true)
    public List<TechnicianConfiguration> technicians() {
        var active = technicians.findAllByActiveTrueOrderById();
        var byTechnician = new HashMap<Long, Map<Integer, Integer>>();
        skills.findAllByTechnicianIdIn(active.stream().map(TechnicianEntity::getId).toList())
                .forEach(skill -> byTechnician
                        .computeIfAbsent(skill.getTechnicianId(), ignored -> new TreeMap<>())
                        .put(skill.getCategoryId(),
                                (int) Math.round(skill.getAffinityScore().doubleValue() * 4)));
        return active.stream().map(technician -> new TechnicianConfiguration(
                technician.getId(), technician.getCode(), technician.getDisplayName(),
                technician.isAvailable(), technician.getCurrentLoad(), technician.getMaxCapacity(),
                byTechnician.getOrDefault(technician.getId(), Map.of()))).toList();
    }

    @Transactional
    public TechnicianSkillEntity updateSkill(Long technicianId, Integer categoryId,
                                             int level, String validatedBy) {
        technicians.findById(technicianId)
                .orElseThrow(() -> new NoSuchElementException("Técnico no encontrado"));
        if (categoryId == null || categoryId < 1 || categoryId > 5) {
            throw new IllegalArgumentException("La categoría debe estar entre 1 y 5");
        }
        var skill = skills.findByTechnicianIdAndCategoryId(technicianId, categoryId)
                .orElseGet(() -> new TechnicianSkillEntity(technicianId, categoryId));
        skill.updateLevel(level, validatedBy == null || validatedBy.isBlank()
                ? "Pendiente de validación experta" : validatedBy.trim());
        return skills.save(skill);
    }

    @Transactional
    public TechnicianEntity updateAvailability(Long technicianId, boolean available) {
        var technician = technicians.findById(technicianId)
                .orElseThrow(() -> new NoSuchElementException("Técnico no encontrado"));
        technician.updateAvailability(available);
        return technicians.save(technician);
    }

    @Transactional(readOnly = true)
    public List<WeightConfiguration> weights(String policyVersion) {
        return weights.findAllByPolicyVersionOrderBySeverityId(policyVersion).stream()
                .map(entity -> new WeightConfiguration(entity.getPolicyVersion(), entity.getSeverityId(),
                        entity.getAlphaAffinity().doubleValue(),
                        entity.getGammaLoad().doubleValue())).toList();
    }

    @Transactional
    public WeightConfiguration updateWeights(String policyVersion, Integer severityId,
                                             double affinity, double load) {
        var entity = weights.findByPolicyVersionAndSeverityIdAndActiveTrue(policyVersion, severityId)
                .orElseThrow(() -> new NoSuchElementException("Pesos no encontrados para política y severidad"));
        entity.update(affinity, load);
        weights.save(entity);
        return new WeightConfiguration(entity.getPolicyVersion(), entity.getSeverityId(), affinity, load);
    }

    @Transactional(readOnly = true)
    public List<ThresholdConfiguration> thresholds(String policyVersion) {
        return thresholds.findAllByPolicyVersionOrderBySeverityId(policyVersion).stream()
                .map(entity -> new ThresholdConfiguration(entity.getPolicyVersion(),
                        entity.getSeverityId(), entity.getMinimumAffinity().doubleValue()))
                .toList();
    }

    @Transactional
    public ThresholdConfiguration updateThreshold(String policyVersion, Integer severityId,
                                                  double minimumAffinity) {
        var entity = thresholds.findByPolicyVersionAndSeverityIdAndActiveTrue(policyVersion, severityId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Umbral no encontrado para política y severidad"));
        entity.update(minimumAffinity);
        thresholds.save(entity);
        return new ThresholdConfiguration(policyVersion, severityId, minimumAffinity);
    }

    public record TechnicianConfiguration(Long id, String code, String name, boolean available,
                                          int currentLoad, int maximumCapacity,
                                          Map<Integer, Integer> skillLevelByCategory) {}
    public record WeightConfiguration(String policyVersion, Integer severityId,
                                      double affinity, double load) {}
    public record ThresholdConfiguration(String policyVersion, Integer severityId,
                                         double minimumAffinity) {}
}

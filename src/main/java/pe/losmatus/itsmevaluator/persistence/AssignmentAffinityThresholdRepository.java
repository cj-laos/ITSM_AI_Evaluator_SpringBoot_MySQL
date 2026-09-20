package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentAffinityThresholdRepository
        extends JpaRepository<AssignmentAffinityThresholdEntity, Long> {
    List<AssignmentAffinityThresholdEntity> findAllByPolicyVersionOrderBySeverityId(String policyVersion);
    Optional<AssignmentAffinityThresholdEntity> findByPolicyVersionAndSeverityIdAndActiveTrue(
            String policyVersion, Integer severityId);
}

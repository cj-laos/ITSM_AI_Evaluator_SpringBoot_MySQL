package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentPolicyWeightRepository extends JpaRepository<AssignmentPolicyWeightEntity, Long> {
    Optional<AssignmentPolicyWeightEntity> findByPolicyVersionAndSeverityIdAndActiveTrue(
            String policyVersion, Integer severityId);
    List<AssignmentPolicyWeightEntity> findAllByPolicyVersionOrderBySeverityId(String policyVersion);
}

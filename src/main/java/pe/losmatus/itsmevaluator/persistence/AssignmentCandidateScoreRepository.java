package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentCandidateScoreRepository extends JpaRepository<AssignmentCandidateScoreEntity, Long> {
    List<AssignmentCandidateScoreEntity> findAllByAssignmentRunIdOrderByTechnicianId(Long assignmentRunId);
}

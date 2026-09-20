package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentExperimentRepository extends JpaRepository<AssignmentExperimentEntity, Long> {
    List<AssignmentExperimentEntity> findAllBySourceEvaluationRunIdOrderByIdDesc(Long sourceEvaluationRunId);
}

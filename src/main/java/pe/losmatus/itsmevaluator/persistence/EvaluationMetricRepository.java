package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationMetricRepository extends JpaRepository<EvaluationMetricEntity, Long> {
    List<EvaluationMetricEntity> findAllByRunIdOrderByTargetAscAverageTypeAscClassLabelAsc(Long runId);
    void deleteAllByRunId(Long runId);
}

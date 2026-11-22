package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRunRepository extends JpaRepository<EvaluationRunEntity, Long> {
    List<EvaluationRunEntity> findAllByDatasetIdOrderByIdDesc(Long datasetId);
}

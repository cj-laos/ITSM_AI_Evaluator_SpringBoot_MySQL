package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRunRepository extends JpaRepository<AssignmentRunEntity, Long> {
    List<AssignmentRunEntity> findAllByExperimentIdOrderById(Long experimentId);
}

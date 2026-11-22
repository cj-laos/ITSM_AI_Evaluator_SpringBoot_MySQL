package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResultEntity, Long> {
    List<EvaluationResultEntity> findAllByRunIdOrderByTicketId(Long runId);
    boolean existsByRunIdAndTicketId(Long runId, Long ticketId);
    long countByRunId(Long runId);
}

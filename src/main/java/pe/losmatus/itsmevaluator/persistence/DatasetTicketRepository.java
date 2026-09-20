package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.losmatus.itsmevaluator.domain.DatasetSplit;

public interface DatasetTicketRepository extends JpaRepository<DatasetTicketEntity, Long> {
    List<DatasetTicketEntity> findAllByDatasetIdOrderById(Long datasetId);
    List<DatasetTicketEntity> findAllByDatasetIdAndSplitOrderById(Long datasetId, DatasetSplit split);
    long countByDatasetIdAndSplit(Long datasetId, DatasetSplit split);
    boolean existsByDatasetIdAndExternalTicketId(Long datasetId, String externalTicketId);
}

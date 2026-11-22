package pe.losmatus.itsmevaluator.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Long> {
    Optional<DatasetEntity> findByNameAndVersion(String name, String version);
}

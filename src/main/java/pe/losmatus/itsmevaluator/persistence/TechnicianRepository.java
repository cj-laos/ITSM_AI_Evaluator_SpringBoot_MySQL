package pe.losmatus.itsmevaluator.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository extends JpaRepository<TechnicianEntity, Long> {
    List<TechnicianEntity> findAllByActiveTrueOrderById();
}

package pe.losmatus.itsmevaluator.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianSkillRepository extends JpaRepository<TechnicianSkillEntity, Long> {
    List<TechnicianSkillEntity> findAllByTechnicianIdIn(Collection<Long> technicianIds);
    Optional<TechnicianSkillEntity> findByTechnicianIdAndCategoryId(Long technicianId, Integer categoryId);
}

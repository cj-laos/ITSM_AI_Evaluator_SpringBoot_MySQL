package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "technician_skills", uniqueConstraints =
        @UniqueConstraint(name = "uk_technician_category", columnNames = {"technician_id", "category_id"}))
public class TechnicianSkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "technician_id", nullable = false)
    private Long technicianId;
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;
    @Column(name = "affinity_score", nullable = false, precision = 6, scale = 5)
    private BigDecimal affinityScore;
    @Column(name = "validated_by", length = 100)
    private String validatedBy;
    @Column(name = "validated_at")
    private Instant validatedAt;

    protected TechnicianSkillEntity() {}

    public TechnicianSkillEntity(Long technicianId, Integer categoryId) {
        this.technicianId = technicianId;
        this.categoryId = categoryId;
        this.affinityScore = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public Long getTechnicianId() { return technicianId; }
    public Integer getCategoryId() { return categoryId; }
    public BigDecimal getAffinityScore() { return affinityScore; }
    public String getValidatedBy() { return validatedBy; }
    public Instant getValidatedAt() { return validatedAt; }

    public void updateLevel(int level, String validator) {
        if (level < 0 || level > 4) throw new IllegalArgumentException("El nivel debe estar entre 0 y 4");
        this.affinityScore = BigDecimal.valueOf(level).divide(BigDecimal.valueOf(4));
        this.validatedBy = validator;
        this.validatedAt = Instant.now();
    }
}

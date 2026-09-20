package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "assignment_affinity_thresholds", uniqueConstraints =
        @UniqueConstraint(name = "uk_threshold_policy_severity",
                columnNames = {"policy_version", "severity_id"}))
public class AssignmentAffinityThresholdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "policy_version", nullable = false, length = 40)
    private String policyVersion;
    @Column(name = "severity_id", nullable = false)
    private Integer severityId;
    @Column(name = "minimum_affinity", nullable = false, precision = 6, scale = 5)
    private BigDecimal minimumAffinity;
    private boolean active;

    protected AssignmentAffinityThresholdEntity() {}

    public Long getId() { return id; }
    public String getPolicyVersion() { return policyVersion; }
    public Integer getSeverityId() { return severityId; }
    public BigDecimal getMinimumAffinity() { return minimumAffinity; }
    public boolean isActive() { return active; }

    public void update(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("La afinidad mínima debe estar entre 0 y 1");
        }
        this.minimumAffinity = BigDecimal.valueOf(value);
    }
}

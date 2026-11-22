package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import pe.losmatus.itsmevaluator.assignment.AssignmentWeights;

@Entity
@Table(name = "assignment_policy_weights", uniqueConstraints =
        @UniqueConstraint(name = "uk_policy_version_severity", columnNames = {"policy_version", "severity_id"}))
public class AssignmentPolicyWeightEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "policy_version", nullable = false, length = 40)
    private String policyVersion;
    @Column(name = "severity_id", nullable = false)
    private Integer severityId;
    @Column(name = "alpha_affinity", nullable = false, precision = 6, scale = 5)
    private BigDecimal alphaAffinity;
    @Column(name = "beta_availability", nullable = false, precision = 6, scale = 5)
    private BigDecimal betaAvailability;
    @Column(name = "gamma_load", nullable = false, precision = 6, scale = 5)
    private BigDecimal gammaLoad;
    private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AssignmentPolicyWeightEntity() {}

    public Long getId() { return id; }
    public String getPolicyVersion() { return policyVersion; }
    public Integer getSeverityId() { return severityId; }
    public BigDecimal getAlphaAffinity() { return alphaAffinity; }
    public BigDecimal getBetaAvailability() { return betaAvailability; }
    public BigDecimal getGammaLoad() { return gammaLoad; }
    public boolean isActive() { return active; }

    public AssignmentWeights toWeights() {
        double affinity = alphaAffinity.doubleValue();
        double load = gammaLoad.doubleValue();
        double total = affinity + load;
        if (total <= 0) throw new IllegalStateException("La afinidad y la carga no pueden sumar cero");
        return new AssignmentWeights(affinity / total, load / total);
    }

    public void update(double affinity, double load) {
        new AssignmentWeights(affinity, load);
        this.alphaAffinity = BigDecimal.valueOf(affinity);
        this.betaAvailability = BigDecimal.ZERO;
        this.gammaLoad = BigDecimal.valueOf(load);
    }
}

package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import pe.losmatus.itsmevaluator.assignment.CandidateScore;

@Entity
@Table(name = "assignment_candidate_scores", uniqueConstraints =
        @UniqueConstraint(name = "uk_assignment_candidate", columnNames = {"assignment_run_id", "technician_id"}))
public class AssignmentCandidateScoreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "assignment_run_id", nullable = false)
    private Long assignmentRunId;
    @Column(name = "technician_id", nullable = false)
    private Long technicianId;
    @Column(name = "affinity_normalized", nullable = false, precision = 8, scale = 7)
    private BigDecimal affinityNormalized;
    @Column(name = "availability_normalized", nullable = false, precision = 8, scale = 7)
    private BigDecimal availabilityNormalized;
    @Column(name = "load_normalized", nullable = false, precision = 8, scale = 7)
    private BigDecimal loadNormalized;
    @Column(name = "suitability_index", nullable = false, precision = 10, scale = 8)
    private BigDecimal suitabilityIndex;
    private boolean selected;
    @Column(name = "rejection_reason", length = 250)
    private String rejectionReason;

    protected AssignmentCandidateScoreEntity() {}

    public AssignmentCandidateScoreEntity(Long assignmentRunId, CandidateScore score) {
        this.assignmentRunId = assignmentRunId;
        this.technicianId = score.technicianId();
        this.affinityNormalized = BigDecimal.valueOf(score.affinity());
        this.availabilityNormalized = BigDecimal.valueOf(score.availability());
        this.loadNormalized = BigDecimal.valueOf(score.loadNormalized());
        this.suitabilityIndex = BigDecimal.valueOf(score.suitabilityIndex());
        this.selected = score.selected();
        this.rejectionReason = score.rejectionReason();
    }

    public Long getId() { return id; }
    public Long getAssignmentRunId() { return assignmentRunId; }
    public Long getTechnicianId() { return technicianId; }
    public BigDecimal getAffinityNormalized() { return affinityNormalized; }
    public BigDecimal getAvailabilityNormalized() { return availabilityNormalized; }
    public BigDecimal getLoadNormalized() { return loadNormalized; }
    public BigDecimal getSuitabilityIndex() { return suitabilityIndex; }
    public boolean isSelected() { return selected; }
    public String getRejectionReason() { return rejectionReason; }
}

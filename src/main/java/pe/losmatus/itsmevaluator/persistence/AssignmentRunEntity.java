package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assignment_runs")
public class AssignmentRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "experiment_id")
    private Long experimentId;
    @Column(name = "evaluation_result_id")
    private Long evaluationResultId;
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    @Column(name = "selected_technician_id")
    private Long selectedTechnicianId;
    @Column(name = "policy_version", nullable = false, length = 40)
    private String policyVersion;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "assigned_at")
    private Instant assignedAt;
    @Column(name = "assignment_time_ms")
    private Long assignmentTimeMs;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AssignmentRunEntity() {}

    public AssignmentRunEntity(Long experimentId, Long evaluationResultId, Long ticketId,
                               Long selectedTechnicianId, String policyVersion, String status,
                               long assignmentTimeMs) {
        this.experimentId = experimentId;
        this.evaluationResultId = evaluationResultId;
        this.ticketId = ticketId;
        this.selectedTechnicianId = selectedTechnicianId;
        this.policyVersion = policyVersion;
        this.status = status;
        this.assignmentTimeMs = assignmentTimeMs;
        this.assignedAt = selectedTechnicianId == null ? null : Instant.now();
    }

    public Long getId() { return id; }
    public Long getExperimentId() { return experimentId; }
    public Long getEvaluationResultId() { return evaluationResultId; }
    public Long getTicketId() { return ticketId; }
    public Long getSelectedTechnicianId() { return selectedTechnicianId; }
    public String getStatus() { return status; }
    public Long getAssignmentTimeMs() { return assignmentTimeMs; }
}

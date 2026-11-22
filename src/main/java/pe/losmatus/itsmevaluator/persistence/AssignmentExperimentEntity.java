package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import pe.losmatus.itsmevaluator.assignment.AssignmentStrategy;

@Entity
@Table(name = "assignment_experiments")
public class AssignmentExperimentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "source_evaluation_run_id", nullable = false)
    private Long sourceEvaluationRunId;
    @Column(name = "policy_version", nullable = false, length = 40)
    private String policyVersion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentStrategy strategy;
    @Column(nullable = false, length = 80)
    private String scenario;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "total_tickets")
    private int totalTickets;
    @Column(name = "assigned_tickets")
    private int assignedTickets;
    @Column(name = "unassigned_tickets")
    private int unassignedTickets;
    @Column(name = "valid_assignment_rate")
    private Double validAssignmentRate;
    @Column(name = "assignment_rate")
    private Double assignmentRate;
    @Column(name = "mean_suitability")
    private Double meanSuitability;
    @Column(name = "mean_affinity")
    private Double meanAffinity;
    @Column(name = "mean_availability")
    private Double meanAvailability;
    @Column(name = "mean_assignment_time_ms")
    private Double meanAssignmentTimeMs;
    @Column(name = "workload_std_deviation")
    private Double workloadStdDeviation;
    @Column(name = "workload_range")
    private Double workloadRange;
    @Column(name = "jain_fairness_index")
    private Double jainFairnessIndex;
    @Column(name = "assignment_time_p95_ms")
    private Double assignmentTimeP95Ms;
    @Column(name = "high_critical_specialized_rate")
    private Double highCriticalSpecializedRate;
    @Column(name = "distribution_json", columnDefinition = "TEXT")
    private String distributionJson;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;

    protected AssignmentExperimentEntity() {}

    public AssignmentExperimentEntity(Long sourceEvaluationRunId, String policyVersion,
                                      AssignmentStrategy strategy, String scenario, int totalTickets) {
        this.sourceEvaluationRunId = sourceEvaluationRunId;
        this.policyVersion = policyVersion;
        this.strategy = strategy;
        this.scenario = scenario;
        this.totalTickets = totalTickets;
        this.status = "RUNNING";
        this.startedAt = Instant.now();
    }

    public void complete(int assigned, int unassigned, double assignmentRate, double validRate,
                         double meanSuitability, double meanAffinity, double meanAvailability,
                         double meanTime, double assignmentTimeP95Ms,
                         double workloadStdDeviation, double workloadRange,
                         double jainFairnessIndex, double highCriticalSpecializedRate,
                         String distributionJson) {
        this.assignedTickets = assigned;
        this.unassignedTickets = unassigned;
        this.assignmentRate = assignmentRate;
        this.validAssignmentRate = validRate;
        this.meanSuitability = meanSuitability;
        this.meanAffinity = meanAffinity;
        this.meanAvailability = meanAvailability;
        this.meanAssignmentTimeMs = meanTime;
        this.assignmentTimeP95Ms = assignmentTimeP95Ms;
        this.workloadStdDeviation = workloadStdDeviation;
        this.workloadRange = workloadRange;
        this.jainFairnessIndex = jainFairnessIndex;
        this.highCriticalSpecializedRate = highCriticalSpecializedRate;
        this.distributionJson = distributionJson;
        this.status = "COMPLETED";
        this.finishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getSourceEvaluationRunId() { return sourceEvaluationRunId; }
    public String getPolicyVersion() { return policyVersion; }
    public AssignmentStrategy getStrategy() { return strategy; }
    public String getScenario() { return scenario; }
    public String getStatus() { return status; }
    public int getTotalTickets() { return totalTickets; }
    public int getAssignedTickets() { return assignedTickets; }
    public int getUnassignedTickets() { return unassignedTickets; }
    public Double getValidAssignmentRate() { return validAssignmentRate; }
    public Double getAssignmentRate() { return assignmentRate; }
    public Double getMeanSuitability() { return meanSuitability; }
    public Double getMeanAffinity() { return meanAffinity; }
    public Double getMeanAvailability() { return meanAvailability; }
    public Double getMeanAssignmentTimeMs() { return meanAssignmentTimeMs; }
    public Double getWorkloadStdDeviation() { return workloadStdDeviation; }
    public Double getWorkloadRange() { return workloadRange; }
    public Double getJainFairnessIndex() { return jainFairnessIndex; }
    public Double getAssignmentTimeP95Ms() { return assignmentTimeP95Ms; }
    public Double getHighCriticalSpecializedRate() { return highCriticalSpecializedRate; }
    public String getDistributionJson() { return distributionJson; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}

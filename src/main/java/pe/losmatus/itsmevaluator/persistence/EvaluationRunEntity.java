package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import pe.losmatus.itsmevaluator.domain.*;

@Entity
@Table(name = "evaluation_runs", indexes = @Index(name = "idx_run_dataset_status", columnList = "dataset_id,status"))
public class EvaluationRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiProvider provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DatasetSplit split;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RunStatus status = RunStatus.PENDING;

    private int totalTickets;
    private int processedTickets;
    private int successfulTickets;
    private int failedTickets;
    private Instant startedAt;
    private Instant finishedAt;
    private Long durationMs;
    private Double coverage;
    private Double latencyMeanMs;
    @Column(name = "latency_p50_ms")
    private Long latencyP50Ms;
    @Column(name = "latency_p95_ms")
    private Long latencyP95Ms;
    @Column(name = "latency_p99_ms")
    private Long latencyP99Ms;

    @Column(precision = 18, scale = 8)
    private BigDecimal totalCostUsd = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EvaluationRunEntity() {}

    public EvaluationRunEntity(Long datasetId, AiProvider provider, String model,
                               String promptVersion, DatasetSplit split, int totalTickets) {
        this.datasetId = datasetId;
        this.provider = provider;
        this.model = model;
        this.promptVersion = promptVersion;
        this.split = split;
        this.totalTickets = totalTickets;
    }

    public void start() { status = RunStatus.RUNNING; startedAt = Instant.now(); }

    public void updateProgress(int processed, int successful, int failed) {
        this.processedTickets = processed;
        this.successfulTickets = successful;
        this.failedTickets = failed;
    }

    public void complete(boolean withErrors, double coverage, double latencyMeanMs,
                         long p50, long p95, long p99, BigDecimal totalCostUsd) {
        this.status = withErrors ? RunStatus.COMPLETED_WITH_ERRORS : RunStatus.COMPLETED;
        this.finishedAt = Instant.now();
        this.durationMs = startedAt == null ? null : finishedAt.toEpochMilli() - startedAt.toEpochMilli();
        this.coverage = coverage;
        this.latencyMeanMs = latencyMeanMs;
        this.latencyP50Ms = p50;
        this.latencyP95Ms = p95;
        this.latencyP99Ms = p99;
        this.totalCostUsd = totalCostUsd;
    }

    public void fail(String message) {
        this.status = RunStatus.FAILED;
        this.finishedAt = Instant.now();
        this.durationMs = startedAt == null ? null : finishedAt.toEpochMilli() - startedAt.toEpochMilli();
        this.errorMessage = message;
    }

    public Long getId() { return id; }
    public Long getDatasetId() { return datasetId; }
    public AiProvider getProvider() { return provider; }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public DatasetSplit getSplit() { return split; }
    public RunStatus getStatus() { return status; }
    public int getTotalTickets() { return totalTickets; }
    public int getProcessedTickets() { return processedTickets; }
    public int getSuccessfulTickets() { return successfulTickets; }
    public int getFailedTickets() { return failedTickets; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public Double getCoverage() { return coverage; }
    public Double getLatencyMeanMs() { return latencyMeanMs; }
    public Long getLatencyP50Ms() { return latencyP50Ms; }
    public Long getLatencyP95Ms() { return latencyP95Ms; }
    public Long getLatencyP99Ms() { return latencyP99Ms; }
    public BigDecimal getTotalCostUsd() { return totalCostUsd; }
    public String getErrorMessage() { return errorMessage; }
}

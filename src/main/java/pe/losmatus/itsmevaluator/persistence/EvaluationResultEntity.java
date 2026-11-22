package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import pe.losmatus.itsmevaluator.domain.ResultStatus;

@Entity
@Table(name = "evaluation_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_result_run_ticket", columnNames = {"run_id", "ticket_id"}),
        indexes = @Index(name = "idx_result_run_status", columnList = "run_id,status"))
public class EvaluationResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false)
    private Long runId;
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    private Integer predictedCategoryId;
    private Integer predictedSeverityId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResultStatus status;
    @Column(columnDefinition = "TEXT")
    private String rawResponse;
    private long latencyMs;
    private int inputTokens;
    private int outputTokens;
    private int totalTokens;
    private int cacheReadTokens;
    private int cacheWriteTokens;
    @Column(precision = 18, scale = 8)
    private BigDecimal estimatedCostUsd;
    private int retryCount;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EvaluationResultEntity() {}

    public EvaluationResultEntity(Long runId, Long ticketId, Integer predictedCategoryId,
                                  Integer predictedSeverityId, ResultStatus status,
                                  String rawResponse, long latencyMs, int inputTokens,
                                  int outputTokens, int totalTokens, int cacheReadTokens,
                                  int cacheWriteTokens, BigDecimal estimatedCostUsd,
                                  int retryCount, String errorMessage) {
        this.runId = runId;
        this.ticketId = ticketId;
        this.predictedCategoryId = predictedCategoryId;
        this.predictedSeverityId = predictedSeverityId;
        this.status = status;
        this.rawResponse = rawResponse;
        this.latencyMs = latencyMs;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.cacheReadTokens = cacheReadTokens;
        this.cacheWriteTokens = cacheWriteTokens;
        this.estimatedCostUsd = estimatedCostUsd;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public Long getTicketId() { return ticketId; }
    public Integer getPredictedCategoryId() { return predictedCategoryId; }
    public Integer getPredictedSeverityId() { return predictedSeverityId; }
    public ResultStatus getStatus() { return status; }
    public String getRawResponse() { return rawResponse; }
    public long getLatencyMs() { return latencyMs; }
    public int getInputTokens() { return inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public int getTotalTokens() { return totalTokens; }
    public int getCacheReadTokens() { return cacheReadTokens; }
    public int getCacheWriteTokens() { return cacheWriteTokens; }
    public BigDecimal getEstimatedCostUsd() { return estimatedCostUsd; }
    public int getRetryCount() { return retryCount; }
    public String getErrorMessage() { return errorMessage; }
}

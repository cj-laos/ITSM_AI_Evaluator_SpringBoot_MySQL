package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import pe.losmatus.itsmevaluator.domain.AverageType;
import pe.losmatus.itsmevaluator.domain.MetricTarget;

@Entity
@Table(name = "evaluation_metrics",
        uniqueConstraints = @UniqueConstraint(name = "uk_metric_run_target_average_class",
                columnNames = {"run_id", "target", "average_type", "class_label"}))
public class EvaluationMetricEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false)
    private Long runId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetricTarget target;
    @Enumerated(EnumType.STRING)
    @Column(name = "average_type", nullable = false, length = 20)
    private AverageType averageType;
    @Column(name = "class_label", length = 30)
    private String classLabel;
    private Double accuracy;
    @Column(name = "precision_value")
    private Double precision;
    @Column(name = "recall_value")
private Double recallValue;

@Column(name = "f1_score")
private Double f1Score;

@Column(name = "support_count", nullable = false)
private int supportCount;

@Column(name = "confusion_matrix_json", columnDefinition = "TEXT")
private String confusionMatrixJson;

    protected EvaluationMetricEntity() {}

    public EvaluationMetricEntity(Long runId, MetricTarget target, AverageType averageType,
                                  String classLabel, Double accuracy, Double precision,
                                  Double recall, Double f1, int support, String confusionMatrixJson) {
        this.runId = runId;
        this.target = target;
        this.averageType = averageType;
        this.classLabel = classLabel;
        this.accuracy = accuracy;
        this.precision = precision;
        this.recallValue = recall;
        this.f1Score = f1;
        this.supportCount = support;
        this.confusionMatrixJson = confusionMatrixJson;
    }

    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public MetricTarget getTarget() { return target; }
    public AverageType getAverageType() { return averageType; }
    public String getClassLabel() { return classLabel; }
    public Double getAccuracy() { return accuracy; }
    public Double getPrecision() { return precision; }
    public Double getRecallValue() { return recallValue; }
    public Double getF1Score() { return f1Score; }
    public int getSupportCount() { return supportCount; }
    public String getConfusionMatrixJson() { return confusionMatrixJson; }
}

package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import pe.losmatus.itsmevaluator.domain.DatasetSplit;

@Entity
@Table(name = "dataset_tickets",
        uniqueConstraints = @UniqueConstraint(name = "uk_dataset_external_ticket", columnNames = {"dataset_id", "external_ticket_id"}),
        indexes = {
                @Index(name = "idx_ticket_dataset_split", columnList = "dataset_id,split"),
                @Index(name = "idx_ticket_actual_labels", columnList = "actual_category_id,actual_severity_id")
        })
public class DatasetTicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "external_ticket_id", nullable = false, length = 80)
    private String externalTicketId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "actual_category_id", nullable = false)
    private Integer actualCategoryId;

    @Column(name = "actual_severity_id", nullable = false)
    private Integer actualSeverityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DatasetSplit split = DatasetSplit.UNASSIGNED;

    @Column(
    name = "description_hash",
    nullable = false,
    length = 64,
    columnDefinition = "CHAR(64)"
)
private String descriptionHash;

    @Column(name = "source_group", length = 100)
    private String sourceGroup;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DatasetTicketEntity() {}

    public DatasetTicketEntity(Long datasetId, String externalTicketId, String description,
                               Integer actualCategoryId, Integer actualSeverityId,
                               String descriptionHash, String sourceGroup) {
        this.datasetId = datasetId;
        this.externalTicketId = externalTicketId;
        this.description = description;
        this.actualCategoryId = actualCategoryId;
        this.actualSeverityId = actualSeverityId;
        this.descriptionHash = descriptionHash;
        this.sourceGroup = sourceGroup;
    }

    public Long getId() { return id; }
    public Long getDatasetId() { return datasetId; }
    public String getExternalTicketId() { return externalTicketId; }
    public String getDescription() { return description; }
    public Integer getActualCategoryId() { return actualCategoryId; }
    public Integer getActualSeverityId() { return actualSeverityId; }
    public DatasetSplit getSplit() { return split; }
    public String getDescriptionHash() { return descriptionHash; }
    public String getSourceGroup() { return sourceGroup; }
    public void setSplit(DatasetSplit split) { this.split = split; }
}

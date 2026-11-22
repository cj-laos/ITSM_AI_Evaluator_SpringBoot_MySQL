package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "datasets", uniqueConstraints = @UniqueConstraint(name = "uk_dataset_name_version", columnNames = {"name", "version"}))
public class DatasetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 40)
    private String version;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DatasetEntity() {}

    public DatasetEntity(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}

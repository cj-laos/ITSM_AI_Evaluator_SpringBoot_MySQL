package pe.losmatus.itsmevaluator.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "technicians")
public class TechnicianEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    private boolean active;
    private boolean available;
    @Column(name = "current_load")
    private int currentLoad;
    @Column(name = "max_capacity")
    private int maxCapacity;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TechnicianEntity() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public boolean isActive() { return active; }
    public boolean isAvailable() { return available; }
    public int getCurrentLoad() { return currentLoad; }
    public int getMaxCapacity() { return maxCapacity; }

    public void updateAvailability(boolean available) {
        this.available = available;
        this.updatedAt = Instant.now();
    }
}

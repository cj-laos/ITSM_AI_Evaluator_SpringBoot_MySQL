CREATE TABLE assignment_affinity_thresholds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_version VARCHAR(40) NOT NULL,
    severity_id INT NOT NULL,
    minimum_affinity DECIMAL(6,5) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_threshold_policy_severity UNIQUE (policy_version, severity_id),
    CONSTRAINT fk_threshold_severity FOREIGN KEY (severity_id) REFERENCES severities(id),
    CONSTRAINT chk_minimum_affinity CHECK (minimum_affinity BETWEEN 0 AND 1)
) ENGINE=InnoDB;

-- Disponibilidad se mantiene como filtro booleano; beta se fija en cero.
-- Afinidad y baja carga se renormalizan para sumar 1.
INSERT INTO assignment_policy_weights
    (policy_version, severity_id, alpha_affinity, beta_availability, gamma_load)
VALUES
    ('controlled-simulation-v2', 1, 0.41176, 0.00000, 0.58824),
    ('controlled-simulation-v2', 2, 0.60000, 0.00000, 0.40000),
    ('controlled-simulation-v2', 3, 0.78571, 0.00000, 0.21429),
    ('controlled-simulation-v2', 4, 0.92308, 0.00000, 0.07692);

INSERT INTO assignment_affinity_thresholds
    (policy_version, severity_id, minimum_affinity)
VALUES
    ('controlled-simulation-v2', 1, 0.25000),
    ('controlled-simulation-v2', 2, 0.50000),
    ('controlled-simulation-v2', 3, 0.75000),
    ('controlled-simulation-v2', 4, 0.75000);

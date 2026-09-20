CREATE TABLE assignment_experiments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_evaluation_run_id BIGINT NOT NULL,
    policy_version VARCHAR(40) NOT NULL,
    strategy VARCHAR(30) NOT NULL,
    scenario VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_tickets INT NOT NULL DEFAULT 0,
    assigned_tickets INT NOT NULL DEFAULT 0,
    unassigned_tickets INT NOT NULL DEFAULT 0,
    valid_assignment_rate DOUBLE NULL,
    mean_suitability DOUBLE NULL,
    mean_affinity DOUBLE NULL,
    mean_assignment_time_ms DOUBLE NULL,
    workload_std_deviation DOUBLE NULL,
    distribution_json TEXT NULL,
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    finished_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_assignment_experiment_source
        FOREIGN KEY (source_evaluation_run_id) REFERENCES evaluation_runs(id),
    CONSTRAINT chk_assignment_strategy
        CHECK (strategy IN ('MULTICRITERIA', 'AFFINITY_ONLY', 'ROUND_ROBIN')),
    INDEX idx_assignment_experiment_source (source_evaluation_run_id, strategy)
) ENGINE=InnoDB;

ALTER TABLE assignment_runs
    ADD COLUMN experiment_id BIGINT NULL AFTER id,
    ADD CONSTRAINT fk_assignment_experiment
        FOREIGN KEY (experiment_id) REFERENCES assignment_experiments(id),
    ADD INDEX idx_assignment_experiment (experiment_id);

INSERT IGNORE INTO assignment_policy_weights
    (policy_version, severity_id, alpha_affinity, beta_availability, gamma_load)
VALUES
    ('controlled-simulation-v1', 1, 0.35000, 0.15000, 0.50000),
    ('controlled-simulation-v1', 2, 0.45000, 0.25000, 0.30000),
    ('controlled-simulation-v1', 3, 0.55000, 0.30000, 0.15000),
    ('controlled-simulation-v1', 4, 0.60000, 0.35000, 0.05000);

INSERT IGNORE INTO technicians (code, display_name, active, available, current_load, max_capacity)
VALUES
    ('TEC-001', 'Cristhoper Hilario', TRUE, TRUE, 0, 100),
    ('TEC-002', 'Gianfranco Souza', TRUE, TRUE, 0, 100),
    ('TEC-003', 'Lino Salcedo', TRUE, TRUE, 0, 100);

INSERT IGNORE INTO technician_skills (technician_id, category_id, affinity_score, validated_by, validated_at)
SELECT t.id, provisional.category_id, provisional.affinity_score,
       'Configuración provisional; requiere validación del experto', CURRENT_TIMESTAMP(6)
FROM technicians t
JOIN (
    SELECT 'TEC-001' code, 1 category_id, 1.00000 affinity_score UNION ALL
    SELECT 'TEC-001', 2, 0.75000 UNION ALL
    SELECT 'TEC-001', 3, 0.75000 UNION ALL
    SELECT 'TEC-001', 4, 0.50000 UNION ALL
    SELECT 'TEC-001', 5, 0.75000 UNION ALL
    SELECT 'TEC-002', 1, 0.75000 UNION ALL
    SELECT 'TEC-002', 2, 1.00000 UNION ALL
    SELECT 'TEC-002', 3, 0.75000 UNION ALL
    SELECT 'TEC-002', 4, 0.50000 UNION ALL
    SELECT 'TEC-002', 5, 0.50000 UNION ALL
    SELECT 'TEC-003', 1, 0.50000 UNION ALL
    SELECT 'TEC-003', 2, 0.75000 UNION ALL
    SELECT 'TEC-003', 3, 0.75000 UNION ALL
    SELECT 'TEC-003', 4, 0.75000 UNION ALL
    SELECT 'TEC-003', 5, 1.00000
) provisional ON provisional.code = t.code;

INSERT INTO technician_availability_history
    (technician_id, availability_score, available_from, reason)
SELECT id, 1.00000, CURRENT_TIMESTAMP(6),
       'Condición inicial de la simulación: todos los técnicos disponibles'
FROM technicians
WHERE code IN ('TEC-001', 'TEC-002', 'TEC-003');

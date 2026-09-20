CREATE TABLE categories (
    id INT PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

INSERT INTO categories (id, name) VALUES
    (1, 'Redes'),
    (2, 'Hardware'),
    (3, 'Software'),
    (4, 'Seguridad'),
    (5, 'CCTV');

CREATE TABLE severities (
    id INT PRIMARY KEY,
    level_name VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB;

INSERT INTO severities (id, level_name) VALUES
    (1, 'Baja'),
    (2, 'Media'),
    (3, 'Alta'),
    (4, 'Crítica');

CREATE TABLE datasets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    version VARCHAR(40) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_dataset_name_version UNIQUE (name, version)
) ENGINE=InnoDB;

CREATE TABLE dataset_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    external_ticket_id VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    actual_category_id INT NOT NULL,
    actual_severity_id INT NOT NULL,
    split VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED',
    description_hash CHAR(64) NOT NULL,
    source_group VARCHAR(100),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_dataset_external_ticket UNIQUE (dataset_id, external_ticket_id),
    CONSTRAINT fk_ticket_dataset FOREIGN KEY (dataset_id) REFERENCES datasets(id),
    CONSTRAINT fk_ticket_category FOREIGN KEY (actual_category_id) REFERENCES categories(id),
    CONSTRAINT fk_ticket_severity FOREIGN KEY (actual_severity_id) REFERENCES severities(id),
    CONSTRAINT chk_ticket_split CHECK (split IN ('UNASSIGNED', 'TRAIN', 'VALIDATION', 'TEST')),
    INDEX idx_ticket_dataset_split (dataset_id, split),
    INDEX idx_ticket_actual_labels (actual_category_id, actual_severity_id),
    INDEX idx_ticket_description_hash (description_hash)
) ENGINE=InnoDB;

CREATE TABLE evaluation_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    split VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_tickets INT NOT NULL DEFAULT 0,
    processed_tickets INT NOT NULL DEFAULT 0,
    successful_tickets INT NOT NULL DEFAULT 0,
    failed_tickets INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP(6) NULL,
    finished_at TIMESTAMP(6) NULL,
    duration_ms BIGINT NULL,
    coverage DOUBLE NULL,
    latency_mean_ms DOUBLE NULL,
    latency_p50_ms BIGINT NULL,
    latency_p95_ms BIGINT NULL,
    latency_p99_ms BIGINT NULL,
    total_cost_usd DECIMAL(18,8) NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_run_dataset FOREIGN KEY (dataset_id) REFERENCES datasets(id),
    INDEX idx_run_dataset_status (dataset_id, status)
) ENGINE=InnoDB;

CREATE TABLE evaluation_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    predicted_category_id INT NULL,
    predicted_severity_id INT NULL,
    status VARCHAR(30) NOT NULL,
    raw_response TEXT NULL,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    cache_read_tokens INT NOT NULL DEFAULT 0,
    cache_write_tokens INT NOT NULL DEFAULT 0,
    estimated_cost_usd DECIMAL(18,8) NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_result_run_ticket UNIQUE (run_id, ticket_id),
    CONSTRAINT fk_result_run FOREIGN KEY (run_id) REFERENCES evaluation_runs(id),
    CONSTRAINT fk_result_ticket FOREIGN KEY (ticket_id) REFERENCES dataset_tickets(id),
    CONSTRAINT fk_result_category FOREIGN KEY (predicted_category_id) REFERENCES categories(id),
    CONSTRAINT fk_result_severity FOREIGN KEY (predicted_severity_id) REFERENCES severities(id),
    INDEX idx_result_run_status (run_id, status)
) ENGINE=InnoDB;

CREATE TABLE evaluation_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    target VARCHAR(20) NOT NULL,
    average_type VARCHAR(20) NOT NULL,
    class_label VARCHAR(30) NULL,
    accuracy DOUBLE NULL,
    precision_value DOUBLE NULL,
    recall_value DOUBLE NULL,
    f1_score DOUBLE NULL,
    support_count INT NOT NULL,
    confusion_matrix_json TEXT NULL,
    generated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_metric_run FOREIGN KEY (run_id) REFERENCES evaluation_runs(id),
    INDEX idx_metric_run (run_id),
    INDEX idx_metric_lookup (run_id, target, average_type, class_label)
) ENGINE=InnoDB;

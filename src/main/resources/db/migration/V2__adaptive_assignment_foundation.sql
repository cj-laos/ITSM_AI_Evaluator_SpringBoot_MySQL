CREATE TABLE technicians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    current_load INT NOT NULL DEFAULT 0,
    max_capacity INT NOT NULL DEFAULT 10,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_technician_load CHECK (current_load >= 0),
    CONSTRAINT chk_technician_capacity CHECK (max_capacity > 0)
) ENGINE=InnoDB;

CREATE TABLE technician_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    technician_id BIGINT NOT NULL,
    category_id INT NOT NULL,
    affinity_score DECIMAL(6,5) NOT NULL,
    validated_by VARCHAR(100),
    validated_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_technician_category UNIQUE (technician_id, category_id),
    CONSTRAINT fk_skill_technician FOREIGN KEY (technician_id) REFERENCES technicians(id),
    CONSTRAINT fk_skill_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_affinity_score CHECK (affinity_score BETWEEN 0 AND 1)
) ENGINE=InnoDB;

CREATE TABLE technician_availability_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    technician_id BIGINT NOT NULL,
    availability_score DECIMAL(6,5) NOT NULL,
    available_from TIMESTAMP(6) NOT NULL,
    available_until TIMESTAMP(6) NULL,
    reason VARCHAR(250),
    CONSTRAINT fk_availability_technician FOREIGN KEY (technician_id) REFERENCES technicians(id),
    CONSTRAINT chk_availability_score CHECK (availability_score BETWEEN 0 AND 1),
    INDEX idx_availability_current (technician_id, available_from, available_until)
) ENGINE=InnoDB;

CREATE TABLE assignment_policy_weights (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_version VARCHAR(40) NOT NULL,
    severity_id INT NOT NULL,
    alpha_affinity DECIMAL(6,5) NOT NULL,
    beta_availability DECIMAL(6,5) NOT NULL,
    gamma_load DECIMAL(6,5) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_policy_version_severity UNIQUE (policy_version, severity_id),
    CONSTRAINT fk_policy_severity FOREIGN KEY (severity_id) REFERENCES severities(id),
    CONSTRAINT chk_alpha CHECK (alpha_affinity BETWEEN 0 AND 1),
    CONSTRAINT chk_beta CHECK (beta_availability BETWEEN 0 AND 1),
    CONSTRAINT chk_gamma CHECK (gamma_load BETWEEN 0 AND 1)
) ENGINE=InnoDB;

INSERT INTO assignment_policy_weights
    (policy_version, severity_id, alpha_affinity, beta_availability, gamma_load)
VALUES
    ('initial-expert-v1', 1, 0.35000, 0.20000, 0.45000),
    ('initial-expert-v1', 2, 0.40000, 0.25000, 0.35000),
    ('initial-expert-v1', 3, 0.50000, 0.30000, 0.20000),
    ('initial-expert-v1', 4, 0.60000, 0.35000, 0.05000);

CREATE TABLE assignment_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_result_id BIGINT NULL,
    ticket_id BIGINT NOT NULL,
    selected_technician_id BIGINT NULL,
    policy_version VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    assigned_at TIMESTAMP(6) NULL,
    assignment_time_ms BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_assignment_result FOREIGN KEY (evaluation_result_id) REFERENCES evaluation_results(id),
    CONSTRAINT fk_assignment_ticket FOREIGN KEY (ticket_id) REFERENCES dataset_tickets(id),
    CONSTRAINT fk_assignment_technician FOREIGN KEY (selected_technician_id) REFERENCES technicians(id),
    INDEX idx_assignment_ticket (ticket_id),
    INDEX idx_assignment_selected (selected_technician_id, assigned_at)
) ENGINE=InnoDB;

CREATE TABLE assignment_candidate_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_run_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    affinity_normalized DECIMAL(8,7) NOT NULL,
    availability_normalized DECIMAL(8,7) NOT NULL,
    load_normalized DECIMAL(8,7) NOT NULL,
    suitability_index DECIMAL(10,8) NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    rejection_reason VARCHAR(250),
    CONSTRAINT uk_assignment_candidate UNIQUE (assignment_run_id, technician_id),
    CONSTRAINT fk_candidate_assignment FOREIGN KEY (assignment_run_id) REFERENCES assignment_runs(id),
    CONSTRAINT fk_candidate_technician FOREIGN KEY (technician_id) REFERENCES technicians(id)
) ENGINE=InnoDB;

ALTER TABLE assignment_experiments
    ADD COLUMN assignment_rate DOUBLE NULL AFTER unassigned_tickets,
    ADD COLUMN mean_availability DOUBLE NULL AFTER mean_affinity,
    ADD COLUMN assignment_time_p95_ms DOUBLE NULL AFTER mean_assignment_time_ms,
    ADD COLUMN workload_range DOUBLE NULL AFTER workload_std_deviation,
    ADD COLUMN jain_fairness_index DOUBLE NULL AFTER workload_range,
    ADD COLUMN high_critical_specialized_rate DOUBLE NULL AFTER jain_fairness_index;

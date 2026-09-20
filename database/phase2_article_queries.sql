-- Configuración congelada para el experimento
SELECT policy_version, severity_id, alpha_affinity, gamma_load
FROM assignment_policy_weights
WHERE policy_version = 'controlled-simulation-v2'
ORDER BY severity_id;

SELECT policy_version, severity_id, minimum_affinity
FROM assignment_affinity_thresholds
WHERE policy_version = 'controlled-simulation-v2'
ORDER BY severity_id;

SELECT t.code, t.display_name, s.category_id,
       s.affinity_score, ROUND(s.affinity_score * 4) AS skill_level,
       s.validated_by, s.validated_at
FROM technicians t
JOIN technician_skills s ON s.technician_id = t.id
WHERE t.active = TRUE
ORDER BY t.id, s.category_id;

-- Resumen comparable de estrategias y escenarios
SELECT id AS experiment_id, source_evaluation_run_id, policy_version,
       strategy, scenario, total_tickets, assigned_tickets, unassigned_tickets,
       assignment_rate, valid_assignment_rate, mean_suitability, mean_affinity,
       mean_availability, mean_assignment_time_ms, assignment_time_p95_ms,
       workload_std_deviation, workload_range, jain_fairness_index,
       high_critical_specialized_rate,
       distribution_json, started_at, finished_at
FROM assignment_experiments
WHERE source_evaluation_run_id = :gpt_test_run_id
ORDER BY id;

-- Distribución por técnico
SELECT e.id AS experiment_id, e.strategy, e.scenario,
       t.code, t.display_name, COUNT(*) AS assigned_tickets,
       SUM(er.predicted_severity_id) AS final_weighted_load
FROM assignment_experiments e
JOIN assignment_runs ar ON ar.experiment_id = e.id
JOIN technicians t ON t.id = ar.selected_technician_id
JOIN evaluation_results er ON er.id = ar.evaluation_result_id
WHERE e.source_evaluation_run_id = :gpt_test_run_id
  AND ar.status = 'ASSIGNED'
GROUP BY e.id, e.strategy, e.scenario, t.id, t.code, t.display_name
ORDER BY e.id, t.id;

-- Trazabilidad completa de una ejecución
SELECT ar.id AS assignment_id, dt.external_ticket_id,
       er.predicted_category_id, er.predicted_severity_id,
       t.code, t.display_name,
       cs.affinity_normalized, cs.availability_normalized,
       cs.load_normalized, cs.suitability_index,
       cs.selected, cs.rejection_reason, ar.assignment_time_ms
FROM assignment_runs ar
JOIN assignment_experiments e ON e.id = ar.experiment_id
JOIN evaluation_results er ON er.id = ar.evaluation_result_id
JOIN dataset_tickets dt ON dt.id = ar.ticket_id
JOIN assignment_candidate_scores cs ON cs.assignment_run_id = ar.id
JOIN technicians t ON t.id = cs.technician_id
WHERE e.id = :experiment_id
ORDER BY ar.id, cs.suitability_index DESC;

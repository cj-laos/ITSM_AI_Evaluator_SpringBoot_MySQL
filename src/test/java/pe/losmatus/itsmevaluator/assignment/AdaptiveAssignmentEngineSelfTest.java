package pe.losmatus.itsmevaluator.assignment;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class AdaptiveAssignmentEngineSelfTest {
    private AdaptiveAssignmentEngineSelfTest() {}

    public static void main(String[] args) {
        var engine = new AdaptiveAssignmentEngine();
        var high = new AssignmentWeights(0.78571, 0.21429);
        var profiles = List.of(
                new TechnicianProfile(1L, "TEC-001", "Cristhoper", 1.00, true, 0),
                new TechnicianProfile(2L, "TEC-002", "Gianfranco", 0.75, true, 0),
                new TechnicianProfile(3L, "TEC-003", "Lino", 0.50, true, 0));
        require(engine.assign(profiles, high, AssignmentStrategy.MULTICRITERIA, 0, 0.75)
                .selectedTechnicianId() == 1L, "Debe seleccionar la mayor afinidad al inicio");

        var low = new AssignmentWeights(0.41176, 0.58824);
        var loaded = List.of(
                new TechnicianProfile(1L, "TEC-001", "Cristhoper", 1.00, true, 4),
                new TechnicianProfile(2L, "TEC-002", "Gianfranco", 0.75, true, 0),
                new TechnicianProfile(3L, "TEC-003", "Lino", 0.50, true, 0));
        require(engine.assign(loaded, low, AssignmentStrategy.MULTICRITERIA, 0, 0.25)
                .selectedTechnicianId() == 2L, "Debe adaptarse a la carga acumulada");

        var unavailable = List.of(
                new TechnicianProfile(1L, "TEC-001", "Cristhoper", 1.00, false, 0),
                new TechnicianProfile(2L, "TEC-002", "Gianfranco", 0.75, true, 0));
        require(engine.assign(unavailable, high, AssignmentStrategy.MULTICRITERIA, 0, 0.75)
                .selectedTechnicianId() == 2L, "Debe excluir técnicos no disponibles");

        double[][] skills = {
                {1.00, 0.75, 0.75, 0.50, 0.75},
                {0.75, 1.00, 0.75, 0.50, 0.50},
                {0.50, 0.75, 0.75, 0.75, 1.00}
        };
        AssignmentWeights[] bySeverity = {
                new AssignmentWeights(0.41176, 0.58824),
                new AssignmentWeights(0.60000, 0.40000),
                new AssignmentWeights(0.78571, 0.21429),
                new AssignmentWeights(0.92308, 0.07692)
        };
        double[] thresholds = {0.25, 0.50, 0.75, 0.75};
        Map<Long, Double> workload = new HashMap<>(Map.of(1L, 0.0, 2L, 0.0, 3L, 0.0));
        Map<Long, Integer> counts = new HashMap<>(Map.of(1L, 0, 2L, 0, 3L, 0));
        for (int ticket = 0; ticket < 300; ticket++) {
            int category = ticket % 5;
            int severity = ticket % 4;
            var candidates = List.of(
                    new TechnicianProfile(1L, "TEC-001", "Cristhoper", skills[0][category], true, workload.get(1L)),
                    new TechnicianProfile(2L, "TEC-002", "Gianfranco", skills[1][category], true, workload.get(2L)),
                    new TechnicianProfile(3L, "TEC-003", "Lino", skills[2][category], true, workload.get(3L)));
            Long selected = engine.assign(candidates, bySeverity[severity],
                    AssignmentStrategy.MULTICRITERIA, ticket, thresholds[severity]).selectedTechnicianId();
            require(selected != null, "Los 300 tickets controlados deben ser asignables");
            workload.merge(selected, (double) severity + 1, Double::sum);
            counts.compute(selected, (id, count) -> count + 1);
        }
        require(counts.values().stream().mapToInt(Integer::intValue).sum() == 300,
                "La simulación debe conservar los 300 tickets");
        require(Math.abs(AssignmentMetrics.jainFairnessIndex(List.of(100.0, 100.0, 100.0)) - 1.0)
                        < 0.000001,
                "El índice de Jain debe ser 1 para cargas iguales");
        System.out.println("AdaptiveAssignmentEngineSelfTest: 5/5 pruebas correctas; distribución=" + counts);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

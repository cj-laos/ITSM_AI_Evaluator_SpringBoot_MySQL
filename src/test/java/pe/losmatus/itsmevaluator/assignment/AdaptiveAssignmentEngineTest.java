package pe.losmatus.itsmevaluator.assignment;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdaptiveAssignmentEngineTest {
    private final AdaptiveAssignmentEngine engine = new AdaptiveAssignmentEngine();

    @Test
    void selectsHighestAffinityWhenAllStartEqual() {
        var decision = engine.assign(List.of(
                profile(1L, 1.00, 1.00, 0),
                profile(2L, 0.75, 1.00, 0),
                profile(3L, 0.50, 1.00, 0)),
                new AssignmentWeights(0.78571, 0.21429),
                AssignmentStrategy.MULTICRITERIA, 0, 0.75);

        assertThat(decision.selectedTechnicianId()).isEqualTo(1L);
    }

    @Test
    void adaptsToAccumulatedLoad() {
        var decision = engine.assign(List.of(
                profile(1L, 1.00, 1.00, 4),
                profile(2L, 0.75, 1.00, 0),
                profile(3L, 0.50, 1.00, 0)),
                new AssignmentWeights(0.41176, 0.58824),
                AssignmentStrategy.MULTICRITERIA, 0, 0.25);

        assertThat(decision.selectedTechnicianId()).isEqualTo(2L);
    }

    @Test
    void excludesUnavailableTechnicianEvenWithBestAffinity() {
        var decision = engine.assign(List.of(
                profile(1L, 1.00, false, 0),
                profile(2L, 0.75, 1.00, 0)),
                new AssignmentWeights(0.92308, 0.07692),
                AssignmentStrategy.MULTICRITERIA, 0, 0.75);

        assertThat(decision.selectedTechnicianId()).isEqualTo(2L);
        assertThat(decision.candidates()).filteredOn(c -> c.technicianId().equals(1L))
                .first().extracting(CandidateScore::rejectionReason).isEqualTo("NO_DISPONIBLE");
    }

    @Test
    void excludesTechnicianBelowMinimumAffinity() {
        var decision = engine.assign(List.of(
                profile(1L, 0.00, 1.00, 0),
                profile(2L, 0.50, 1.00, 0)),
                new AssignmentWeights(0.60, 0.40),
                AssignmentStrategy.MULTICRITERIA, 0, 0.50);

        assertThat(decision.selectedTechnicianId()).isEqualTo(2L);
        assertThat(decision.candidates()).filteredOn(c -> c.technicianId().equals(1L))
                .first().extracting(CandidateScore::rejectionReason).isEqualTo("AFINIDAD_INSUFICIENTE");
    }

    @Test
    void roundRobinIsDeterministic() {
        var profiles = List.of(profile(1L, 1, 1, 0), profile(2L, 1, 1, 0), profile(3L, 1, 1, 0));
        var weights = new AssignmentWeights(0.41176, 0.58824);

        assertThat(engine.assign(profiles, weights, AssignmentStrategy.ROUND_ROBIN, 0, 0.25)
                .selectedTechnicianId()).isEqualTo(1L);
        assertThat(engine.assign(profiles, weights, AssignmentStrategy.ROUND_ROBIN, 1, 0.25)
                .selectedTechnicianId()).isEqualTo(2L);
        assertThat(engine.assign(profiles, weights, AssignmentStrategy.ROUND_ROBIN, 2, 0.25)
                .selectedTechnicianId()).isEqualTo(3L);
    }

    @Test
    void rejectsWeightsThatDoNotSumOne() {
        assertThatThrownBy(() -> new AssignmentWeights(0.50, 0.30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sumar 1");
    }

    @Test
    void assignsAControlledSequenceOfThreeHundredTickets() {
        Map<Long, Double> workload = new HashMap<>(Map.of(1L, 0.0, 2L, 0.0, 3L, 0.0));
        int assigned = 0;
        for (int ticket = 0; ticket < 300; ticket++) {
            var decision = engine.assign(List.of(
                    profile(1L, 1.00, 1.00, workload.get(1L)),
                    profile(2L, 0.75, 1.00, workload.get(2L)),
                    profile(3L, 0.50, 1.00, workload.get(3L))),
                    new AssignmentWeights(0.60, 0.40),
                    AssignmentStrategy.MULTICRITERIA, ticket, 0.50);
            assertThat(decision.selectedTechnicianId()).isNotNull();
            workload.merge(decision.selectedTechnicianId(), 1.0, Double::sum);
            assigned++;
        }
        assertThat(assigned).isEqualTo(300);
    }

    private TechnicianProfile profile(Long id, double affinity, double availability, double load) {
        return profile(id, affinity, availability > 0, load);
    }

    private TechnicianProfile profile(Long id, double affinity, boolean available, double load) {
        return new TechnicianProfile(id, "TEC-00" + id, "Técnico " + id,
                affinity, available, load);
    }
}

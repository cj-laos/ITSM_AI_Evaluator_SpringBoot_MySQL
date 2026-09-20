package pe.losmatus.itsmevaluator.assignment;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AssignmentMetricsTest {
    @Test
    void reportsPerfectFairnessForEqualLoads() {
        var loads = List.of(100.0, 100.0, 100.0);
        assertThat(AssignmentMetrics.standardDeviation(loads)).isZero();
        assertThat(AssignmentMetrics.range(loads)).isZero();
        assertThat(AssignmentMetrics.jainFairnessIndex(loads)).isEqualTo(1.0);
    }

    @Test
    void calculatesFairnessForUnequalLoads() {
        var loads = List.of(150.0, 100.0, 50.0);
        assertThat(AssignmentMetrics.jainFairnessIndex(loads))
                .isCloseTo(0.857142857, within(0.0000001));
        assertThat(AssignmentMetrics.range(loads)).isEqualTo(100.0);
    }

    @Test
    void calculatesNearestRankP95() {
        var values = IntStream.rangeClosed(1, 100).asDoubleStream().boxed().toList();
        assertThat(AssignmentMetrics.percentile(values, 0.95)).isEqualTo(95.0);
    }
}

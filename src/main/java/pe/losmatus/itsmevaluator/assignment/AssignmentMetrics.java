package pe.losmatus.itsmevaluator.assignment;

import java.util.Collection;
import java.util.List;

public final class AssignmentMetrics {
    private AssignmentMetrics() {}

    public static double mean(Collection<Double> values) {
        return values == null || values.isEmpty()
                ? 0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public static double standardDeviation(Collection<Double> values) {
        if (values == null || values.isEmpty()) return 0;
        double mean = mean(values);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    public static double range(Collection<Double> values) {
        if (values == null || values.isEmpty()) return 0;
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return max - min;
    }

    public static double jainFairnessIndex(Collection<Double> values) {
        if (values == null || values.isEmpty()) return 0;
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double sumSquares = values.stream().mapToDouble(value -> value * value).sum();
        if (sumSquares == 0) return 1.0;
        return (sum * sum) / (values.size() * sumSquares);
    }

    public static double percentile(List<Double> values, double percentile) {
        if (values == null || values.isEmpty()) return 0;
        if (percentile <= 0 || percentile > 1) {
            throw new IllegalArgumentException("El percentil debe estar en el intervalo (0, 1]");
        }
        List<Double> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}

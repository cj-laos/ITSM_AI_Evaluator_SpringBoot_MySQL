package pe.losmatus.itsmevaluator.assignment;

public record AssignmentWeights(double affinity, double load) {
    private static final double EPSILON = 0.000_001;

    public AssignmentWeights {
        if (!inRange(affinity) || !inRange(load)) {
            throw new IllegalArgumentException("Cada peso debe estar entre 0 y 1");
        }
        if (Math.abs((affinity + load) - 1.0) > EPSILON) {
            throw new IllegalArgumentException("Los pesos de afinidad y carga deben sumar 1");
        }
    }

    private static boolean inRange(double value) {
        return value >= 0 && value <= 1;
    }
}

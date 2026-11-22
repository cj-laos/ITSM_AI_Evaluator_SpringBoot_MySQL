package pe.losmatus.itsmevaluator.assignment;

public record TechnicianProfile(
        Long technicianId,
        String code,
        String displayName,
        double affinity,
        boolean available,
        double currentLoad) {
}

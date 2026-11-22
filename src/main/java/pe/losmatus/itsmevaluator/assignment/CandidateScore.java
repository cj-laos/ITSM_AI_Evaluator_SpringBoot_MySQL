package pe.losmatus.itsmevaluator.assignment;

public record CandidateScore(
        Long technicianId,
        String technicianCode,
        String technicianName,
        double affinity,
        double availability,
        double loadNormalized,
        double suitabilityIndex,
        boolean eligible,
        boolean selected,
        String rejectionReason) {

    CandidateScore select(boolean value) {
        return new CandidateScore(technicianId, technicianCode, technicianName, affinity,
                availability, loadNormalized, suitabilityIndex, eligible, value, rejectionReason);
    }
}

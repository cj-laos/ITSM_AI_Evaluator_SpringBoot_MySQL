package pe.losmatus.itsmevaluator.assignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AdaptiveAssignmentEngine {

    public AssignmentDecision assign(List<TechnicianProfile> profiles, AssignmentWeights weights,
                                     AssignmentStrategy strategy, long roundRobinPosition,
                                     double minimumAffinity) {
        if (profiles == null || profiles.isEmpty()) {
            return new AssignmentDecision(null, List.of());
        }
        double maximumLoad = profiles.stream()
                .filter(profile -> profile.affinity() >= minimumAffinity)
                .filter(TechnicianProfile::available)
                .mapToDouble(TechnicianProfile::currentLoad)
                .max().orElse(0);

        List<CandidateScore> calculated = profiles.stream()
                .sorted(Comparator.comparing(TechnicianProfile::technicianId))
                .map(profile -> score(profile, weights, maximumLoad, minimumAffinity))
                .toList();
        List<CandidateScore> eligible = calculated.stream().filter(CandidateScore::eligible).toList();
        if (eligible.isEmpty()) {
            return new AssignmentDecision(null, calculated);
        }

        CandidateScore selected = switch (strategy) {
            case ROUND_ROBIN -> eligible.get(Math.floorMod(roundRobinPosition, eligible.size()));
            case AFFINITY_ONLY -> eligible.stream().min(affinityComparator()).orElseThrow();
            case MULTICRITERIA -> eligible.stream().min(multicriteriaComparator()).orElseThrow();
        };

        List<CandidateScore> marked = new ArrayList<>(calculated.size());
        for (CandidateScore candidate : calculated) {
            marked.add(candidate.select(candidate.technicianId().equals(selected.technicianId())));
        }
        return new AssignmentDecision(selected.technicianId(), List.copyOf(marked));
    }

    private CandidateScore score(TechnicianProfile profile, AssignmentWeights weights,
                                 double maximumLoad, double minimumAffinity) {
        double affinity = clamp(profile.affinity());
        double availability = profile.available() ? 1.0 : 0.0;
        double load = maximumLoad <= 0 ? 0 : clamp(profile.currentLoad() / maximumLoad);
        String rejection = null;
        if (!profile.available()) rejection = "NO_DISPONIBLE";
        else if (affinity < minimumAffinity) rejection = "AFINIDAD_INSUFICIENTE";
        boolean eligible = rejection == null;
        double index = weights.affinity() * affinity + weights.load() * (1.0 - load);
        return new CandidateScore(profile.technicianId(), profile.code(), profile.displayName(),
                affinity, availability, load, index, eligible, false, rejection);
    }

    private Comparator<CandidateScore> multicriteriaComparator() {
        return Comparator.comparingDouble(CandidateScore::suitabilityIndex).reversed()
                .thenComparing(Comparator.comparingDouble(CandidateScore::affinity).reversed())
                .thenComparingDouble(CandidateScore::loadNormalized)
                .thenComparing(CandidateScore::technicianId);
    }

    private Comparator<CandidateScore> affinityComparator() {
    return Comparator.comparingDouble(CandidateScore::affinity).reversed()
            .thenComparing(CandidateScore::technicianId);
}

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}

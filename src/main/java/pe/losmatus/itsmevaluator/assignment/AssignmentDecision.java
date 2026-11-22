package pe.losmatus.itsmevaluator.assignment;

import java.util.List;

public record AssignmentDecision(Long selectedTechnicianId, List<CandidateScore> candidates) {
}

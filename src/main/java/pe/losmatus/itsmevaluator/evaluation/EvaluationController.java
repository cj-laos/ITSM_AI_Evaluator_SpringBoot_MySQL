package pe.losmatus.itsmevaluator.evaluation;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.losmatus.itsmevaluator.ai.AiClassifierRegistry;
import pe.losmatus.itsmevaluator.persistence.*;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final EvaluationService evaluationService;
    private final EvaluationRunRepository runs;
    private final EvaluationResultRepository results;
    private final EvaluationMetricRepository metrics;
    private final AiClassifierRegistry classifiers;

    public EvaluationController(EvaluationService evaluationService, EvaluationRunRepository runs,
                                EvaluationResultRepository results, EvaluationMetricRepository metrics,
                                AiClassifierRegistry classifiers) {
        this.evaluationService = evaluationService;
        this.runs = runs;
        this.results = results;
        this.metrics = metrics;
        this.classifiers = classifiers;
    }

    @GetMapping("/providers")
    public Map<?, ?> providers() {
        return classifiers.availableModels();
    }

    @PostMapping("/comparisons")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EvaluationService.ComparisonResponse compare(
            @RequestBody EvaluationService.ComparisonRequest request) {
        return evaluationService.createComparison(request);
    }

    @GetMapping("/{runId}")
    public EvaluationRunEntity run(@PathVariable Long runId) {
        return runs.findById(runId).orElseThrow();
    }

    @GetMapping("/{runId}/results")
    public List<EvaluationResultEntity> results(@PathVariable Long runId) {
        return results.findAllByRunIdOrderByTicketId(runId);
    }

    @GetMapping("/{runId}/metrics")
    public List<EvaluationMetricEntity> metrics(@PathVariable Long runId) {
        return metrics.findAllByRunIdOrderByTargetAscAverageTypeAscClassLabelAsc(runId);
    }

    @PostMapping("/{runId}/resume")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resume(@PathVariable Long runId) {
        runs.findById(runId).orElseThrow();
        evaluationService.submit(runId);
    }
}

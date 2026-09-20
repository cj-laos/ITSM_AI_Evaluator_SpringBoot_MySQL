package pe.losmatus.itsmevaluator.evaluation;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import pe.losmatus.itsmevaluator.ai.*;
import pe.losmatus.itsmevaluator.config.AppProperties;
import pe.losmatus.itsmevaluator.domain.*;
import pe.losmatus.itsmevaluator.persistence.*;

@Service
public class EvaluationService {
    private final DatasetRepository datasets;
    private final DatasetTicketRepository tickets;
    private final EvaluationRunRepository runs;
    private final EvaluationResultRepository results;
    private final AiClassifierRegistry classifiers;
    private final MetricsService metricsService;
    private final PromptCatalog promptCatalog;
    private final AppProperties properties;
    private final Executor evaluationExecutor;

    public EvaluationService(DatasetRepository datasets, DatasetTicketRepository tickets,
                             EvaluationRunRepository runs, EvaluationResultRepository results,
                             AiClassifierRegistry classifiers,
                             MetricsService metricsService, PromptCatalog promptCatalog,
                             AppProperties properties,
                             @Qualifier("evaluationExecutor") Executor evaluationExecutor) {
        this.datasets = datasets;
        this.tickets = tickets;
        this.runs = runs;
        this.results = results;
        this.classifiers = classifiers;
        this.metricsService = metricsService;
        this.promptCatalog = promptCatalog;
        this.properties = properties;
        this.evaluationExecutor = evaluationExecutor;
    }

    public ComparisonResponse createComparison(ComparisonRequest request) {
        datasets.findById(request.datasetId())
                .orElseThrow(() -> new NoSuchElementException("Dataset no encontrado"));
        DatasetSplit split = request.split() == null ? DatasetSplit.TEST : request.split();
        if (split == DatasetSplit.UNASSIGNED) {
            throw new IllegalArgumentException("UNASSIGNED no es un conjunto evaluable");
        }
        var requestedProviders = request.providers() == null || request.providers().isEmpty()
                ? List.of(AiProvider.OPENAI, AiProvider.GEMINI, AiProvider.CLAUDE)
                : request.providers();
        requestedProviders.forEach(classifiers::require);
        int total = Math.toIntExact(tickets.countByDatasetIdAndSplit(request.datasetId(), split));
        if (total == 0) throw new IllegalArgumentException("El conjunto " + split + " está vacío");

        var created = new ArrayList<EvaluationRunEntity>();
        for (var provider : requestedProviders) {
            var classifier = classifiers.require(provider);
            created.add(runs.save(new EvaluationRunEntity(request.datasetId(), provider,
                    classifier.model(), promptCatalog.version(), split, total)));
        }
        created.forEach(run -> submit(run.getId()));
        return new ComparisonResponse(created.stream().map(EvaluationRunEntity::getId).toList(),
                request.datasetId(), split, total);
    }

    public void submit(Long runId) {
        evaluationExecutor.execute(() -> execute(runId));
    }

    void execute(Long runId) {
        EvaluationRunEntity run = runs.findById(runId).orElseThrow();
        try {
            run.start();
            runs.save(run);
            var classifier = classifiers.require(run.getProvider());
            var datasetTickets = tickets.findAllByDatasetIdAndSplitOrderById(run.getDatasetId(), run.getSplit());
            int processed = 0;
            int successful = 0;
            int failed = 0;

            for (var ticket : datasetTickets) {
                if (results.existsByRunIdAndTicketId(runId, ticket.getId())) {
                    processed++;
                    continue;
                }
                AttemptedClassification attempt = classifyWithRetry(classifier, ticket.getDescription());
                AiClassification outcome = attempt.result();
                if (outcome.status() == ResultStatus.SUCCESS) successful++; else failed++;
                processed++;
                results.save(new EvaluationResultEntity(runId, ticket.getId(),
                        outcome.categoryId(), outcome.severityId(), outcome.status(),
                        outcome.rawResponse(), outcome.latencyMs(), outcome.inputTokens(),
                        outcome.outputTokens(), outcome.totalTokens(), outcome.cacheReadTokens(),
                        outcome.cacheWriteTokens(), outcome.estimatedCostUsd(),
                        attempt.retries(), outcome.errorMessage()));
                run.updateProgress(processed, successful, failed);
                runs.save(run);
            }

            var stored = results.findAllByRunIdOrderByTicketId(runId);
            var valid = stored.stream().filter(result -> result.getStatus() == ResultStatus.SUCCESS).toList();
            int finalFailed = stored.size() - valid.size();
            run.updateProgress(stored.size(), valid.size(), finalFailed);
            var latencies = valid.stream().map(EvaluationResultEntity::getLatencyMs).sorted().toList();
            BigDecimal totalCost = stored.stream().map(EvaluationResultEntity::getEstimatedCostUsd)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            double coverage = run.getTotalTickets() == 0 ? 0 : (double) valid.size() / run.getTotalTickets();
            double mean = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
            run.complete(finalFailed > 0, coverage, mean, percentile(latencies, 0.50),
                    percentile(latencies, 0.95), percentile(latencies, 0.99), totalCost);
            runs.save(run);
            metricsService.calculate(run);
        } catch (Exception ex) {
            run.fail(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            runs.save(run);
        }
    }

    private AttemptedClassification classifyWithRetry(AiClassifier classifier, String description) {
        AiClassification accumulated = null;
        for (int attempt = 1; attempt <= properties.execution().maxAttempts(); attempt++) {
            AiClassification current = classifier.classify(description);
            accumulated = aggregate(accumulated, current);
            if (current.status() == ResultStatus.SUCCESS) {
                return new AttemptedClassification(accumulated, attempt - 1);
            }
            if (attempt < properties.execution().maxAttempts()) {
                try {
                    Thread.sleep(properties.execution().retryBackoffMs() * attempt);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return new AttemptedClassification(accumulated, attempt - 1);
                }
            }
        }
        return new AttemptedClassification(accumulated,
                Math.max(0, properties.execution().maxAttempts() - 1));
    }

    private AiClassification aggregate(AiClassification previous, AiClassification current) {
        if (previous == null) return current;
        return new AiClassification(current.categoryId(), current.severityId(), current.status(),
                current.rawResponse(), previous.latencyMs() + current.latencyMs(),
                previous.inputTokens() + current.inputTokens(),
                previous.outputTokens() + current.outputTokens(),
                previous.totalTokens() + current.totalTokens(),
                previous.cacheReadTokens() + current.cacheReadTokens(),
                previous.cacheWriteTokens() + current.cacheWriteTokens(),
                previous.estimatedCostUsd().add(current.estimatedCostUsd()),
                current.errorMessage());
    }

    private long percentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    public record ComparisonRequest(Long datasetId, DatasetSplit split, List<AiProvider> providers) {}
    public record ComparisonResponse(List<Long> runIds, Long datasetId, DatasetSplit split, int ticketsPerRun) {}
    private record AttemptedClassification(AiClassification result, int retries) {}
}

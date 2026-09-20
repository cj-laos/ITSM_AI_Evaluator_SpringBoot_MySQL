package pe.losmatus.itsmevaluator.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.losmatus.itsmevaluator.domain.*;
import pe.losmatus.itsmevaluator.persistence.*;

@Service
public class MetricsService {
    private static final Map<Integer, String> CATEGORY_LABELS = Map.of(
            1, "Redes", 2, "Hardware", 3, "Software", 4, "Seguridad", 5, "CCTV");
    private static final Map<Integer, String> SEVERITY_LABELS = Map.of(
            1, "Baja", 2, "Media", 3, "Alta", 4, "Crítica");

    private final DatasetTicketRepository tickets;
    private final EvaluationResultRepository results;
    private final EvaluationMetricRepository metrics;
    private final ObjectMapper objectMapper;

    public MetricsService(DatasetTicketRepository tickets, EvaluationResultRepository results,
                          EvaluationMetricRepository metrics, ObjectMapper objectMapper) {
        this.tickets = tickets;
        this.results = results;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<EvaluationMetricEntity> calculate(EvaluationRunEntity run) {
        metrics.deleteAllByRunId(run.getId());
        var ticketMap = new HashMap<Long, DatasetTicketEntity>();
        tickets.findAllByDatasetIdAndSplitOrderById(run.getDatasetId(), run.getSplit())
                .forEach(ticket -> ticketMap.put(ticket.getId(), ticket));
        var predictions = new HashMap<Long, EvaluationResultEntity>();
        results.findAllByRunIdOrderByTicketId(run.getId())
                .forEach(result -> predictions.put(result.getTicketId(), result));

        var rows = new ArrayList<EvaluationMetricEntity>();
        rows.addAll(classificationMetrics(run.getId(), MetricTarget.CATEGORY, CATEGORY_LABELS,
                ticketMap.values(), predictions, true));
        rows.addAll(classificationMetrics(run.getId(), MetricTarget.SEVERITY, SEVERITY_LABELS,
                ticketMap.values(), predictions, false));

        long jointCorrect = ticketMap.values().stream().filter(ticket -> {
            var result = predictions.get(ticket.getId());
            return result != null && result.getStatus() == ResultStatus.SUCCESS
                    && Objects.equals(ticket.getActualCategoryId(), result.getPredictedCategoryId())
                    && Objects.equals(ticket.getActualSeverityId(), result.getPredictedSeverityId());
        }).count();
        int support = ticketMap.size();
        double jointAccuracy = safeDivide(jointCorrect, support);
        rows.add(new EvaluationMetricEntity(run.getId(), MetricTarget.JOINT, AverageType.GLOBAL,
                null, jointAccuracy, null, null, null, support, null));
        return metrics.saveAll(rows);
    }

    private List<EvaluationMetricEntity> classificationMetrics(
            Long runId, MetricTarget target, Map<Integer, String> labels,
            Collection<DatasetTicketEntity> ticketRows,
            Map<Long, EvaluationResultEntity> predictions, boolean category) {

        var output = new ArrayList<EvaluationMetricEntity>();
        int classes = labels.size();
        int[][] matrix = new int[classes][classes + 1];
        long correct = 0;

        var stats = new LinkedHashMap<Integer, ClassStats>();
        labels.keySet().stream().sorted().forEach(label -> stats.put(label, new ClassStats()));

        for (var ticket : ticketRows) {
            int actual = category ? ticket.getActualCategoryId() : ticket.getActualSeverityId();
            var result = predictions.get(ticket.getId());
            Integer predicted = result == null || result.getStatus() != ResultStatus.SUCCESS
                    ? null
                    : category ? result.getPredictedCategoryId() : result.getPredictedSeverityId();

            int predictionColumn = predicted == null || !labels.containsKey(predicted)
                    ? classes : predicted - 1;
            matrix[actual - 1][predictionColumn]++;
            stats.get(actual).support++;
            if (Objects.equals(actual, predicted)) correct++;
        }

        for (int label : stats.keySet()) {
            var current = stats.get(label);
            current.tp = matrix[label - 1][label - 1];
            for (int actual = 0; actual < classes; actual++) {
                if (actual != label - 1) current.fp += matrix[actual][label - 1];
            }
            current.fn = current.support - current.tp;
            current.precision = safeDivide(current.tp, current.tp + current.fp);
            current.recall = safeDivide(current.tp, current.tp + current.fn);
            current.f1 = current.precision + current.recall == 0 ? 0
                    : 2 * current.precision * current.recall / (current.precision + current.recall);
        }

        double accuracy = safeDivide(correct, ticketRows.size());
        int totalSupport = stats.values().stream().mapToInt(value -> value.support).sum();
        double macroPrecision = stats.values().stream().mapToDouble(value -> value.precision).average().orElse(0);
        double macroRecall = stats.values().stream().mapToDouble(value -> value.recall).average().orElse(0);
        double macroF1 = stats.values().stream().mapToDouble(value -> value.f1).average().orElse(0);
        double weightedPrecision = weighted(stats.values(), totalSupport, value -> value.precision);
        double weightedRecall = weighted(stats.values(), totalSupport, value -> value.recall);
        double weightedF1 = weighted(stats.values(), totalSupport, value -> value.f1);
        String matrixJson = matrixJson(labels, matrix);

        for (var entry : stats.entrySet()) {
            var value = entry.getValue();
            output.add(new EvaluationMetricEntity(runId, target, AverageType.CLASS,
                    entry.getKey() + "-" + labels.get(entry.getKey()), accuracy,
                    value.precision, value.recall, value.f1, value.support, null));
        }
        output.add(new EvaluationMetricEntity(runId, target, AverageType.MACRO, null,
                accuracy, macroPrecision, macroRecall, macroF1, totalSupport, matrixJson));
        output.add(new EvaluationMetricEntity(runId, target, AverageType.WEIGHTED, null,
                accuracy, weightedPrecision, weightedRecall, weightedF1, totalSupport, null));
        return output;
    }

    private double weighted(Collection<ClassStats> stats, int total,
                            java.util.function.ToDoubleFunction<ClassStats> getter) {
        if (total == 0) return 0;
        return stats.stream().mapToDouble(value -> getter.applyAsDouble(value) * value.support).sum() / total;
    }

    private String matrixJson(Map<Integer, String> labels, int[][] matrix) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("actual_labels", labels.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).toList());
        var predictedLabels = new ArrayList<>(labels.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).toList());
        predictedLabels.add("SIN_PREDICCION");
        payload.put("predicted_labels", predictedLabels);
        payload.put("matrix", matrix);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la matriz de confusión", ex);
        }
    }

    private double safeDivide(long numerator, long denominator) {
        return denominator == 0 ? 0 : (double) numerator / denominator;
    }

    private static final class ClassStats {
        int tp;
        int fp;
        int fn;
        int support;
        double precision;
        double recall;
        double f1;
    }
}

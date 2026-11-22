package pe.losmatus.itsmevaluator.dataset;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.losmatus.itsmevaluator.domain.DatasetSplit;
import pe.losmatus.itsmevaluator.persistence.*;

@Service
public class DatasetSplitService {
    private final DatasetRepository datasets;
    private final DatasetTicketRepository tickets;

    public DatasetSplitService(DatasetRepository datasets, DatasetTicketRepository tickets) {
        this.datasets = datasets;
        this.tickets = tickets;
    }

    @Transactional
    public SplitResponse split(Long datasetId, SplitRequest request) {
        datasets.findById(datasetId).orElseThrow(() -> new NoSuchElementException("Dataset no encontrado"));
        validateRatios(request);
        var all = tickets.findAllByDatasetIdOrderById(datasetId);
        if (all.isEmpty()) throw new IllegalArgumentException("El dataset no contiene tickets");

        var strata = all.stream().collect(Collectors.groupingBy(
                t -> t.getActualCategoryId() + "-" + t.getActualSeverityId(),
                TreeMap::new,
                Collectors.toCollection(ArrayList::new)));

        for (var entry : strata.entrySet()) {
            var group = entry.getValue();
            Collections.shuffle(group, new Random(request.seed() ^ entry.getKey().hashCode()));
            int trainEnd = (int) Math.round(group.size() * request.trainRatio());
            int validationSize = (int) Math.round(group.size() * request.validationRatio());
            trainEnd = Math.min(group.size(), trainEnd);
            int validationEnd = Math.min(group.size(), trainEnd + validationSize);
            for (int i = 0; i < group.size(); i++) {
                group.get(i).setSplit(i < trainEnd ? DatasetSplit.TRAIN
                        : i < validationEnd ? DatasetSplit.VALIDATION : DatasetSplit.TEST);
            }
        }
        rebalanceExact(all, request);
        tickets.saveAll(all);
        return counts(datasetId, request.seed());
    }

    private void rebalanceExact(List<DatasetTicketEntity> all, SplitRequest request) {
        int total = all.size();
        var targets = new EnumMap<DatasetSplit, Integer>(DatasetSplit.class);
        targets.put(DatasetSplit.TRAIN, (int) Math.round(total * request.trainRatio()));
        targets.put(DatasetSplit.VALIDATION, (int) Math.round(total * request.validationRatio()));
        targets.put(DatasetSplit.TEST,
                total - targets.get(DatasetSplit.TRAIN) - targets.get(DatasetSplit.VALIDATION));

        var ratios = new EnumMap<DatasetSplit, Double>(DatasetSplit.class);
        ratios.put(DatasetSplit.TRAIN, request.trainRatio());
        ratios.put(DatasetSplit.VALIDATION, request.validationRatio());
        ratios.put(DatasetSplit.TEST, request.testRatio());

        while (true) {
            DatasetSplit from = Arrays.stream(new DatasetSplit[] {
                            DatasetSplit.TRAIN, DatasetSplit.VALIDATION, DatasetSplit.TEST})
                    .filter(split -> count(all, split) > targets.get(split)).findFirst().orElse(null);
            DatasetSplit to = Arrays.stream(new DatasetSplit[] {
                            DatasetSplit.TRAIN, DatasetSplit.VALIDATION, DatasetSplit.TEST})
                    .filter(split -> count(all, split) < targets.get(split)).findFirst().orElse(null);
            if (from == null || to == null) return;

            DatasetTicketEntity selected = all.stream()
                    .filter(ticket -> ticket.getSplit() == from)
                    .max(Comparator
                            .comparingDouble((DatasetTicketEntity ticket) ->
                                    movementScore(all, ticket, from, to, ratios))
                            .thenComparingLong(ticket ->
                                    deterministicTie(ticket.getExternalTicketId(), request.seed())))
                    .orElseThrow();
            selected.setSplit(to);
        }
    }

    private double movementScore(List<DatasetTicketEntity> all, DatasetTicketEntity candidate,
                                 DatasetSplit from, DatasetSplit to,
                                 Map<DatasetSplit, Double> targetRatios) {
        long stratumTotal = all.stream().filter(ticket -> sameStratum(ticket, candidate)).count();
        long inFrom = all.stream().filter(ticket -> sameStratum(ticket, candidate)
                && ticket.getSplit() == from).count();
        long inTo = all.stream().filter(ticket -> sameStratum(ticket, candidate)
                && ticket.getSplit() == to).count();
        double fromExcess = (double) inFrom / stratumTotal - targetRatios.get(from);
        double toDeficit = targetRatios.get(to) - (double) inTo / stratumTotal;
        return fromExcess + toDeficit;
    }

    private boolean sameStratum(DatasetTicketEntity left, DatasetTicketEntity right) {
        return Objects.equals(left.getActualCategoryId(), right.getActualCategoryId())
                && Objects.equals(left.getActualSeverityId(), right.getActualSeverityId());
    }

    private long deterministicTie(String externalId, long seed) {
        return new Random(seed ^ externalId.hashCode()).nextLong();
    }

    private long count(List<DatasetTicketEntity> all, DatasetSplit split) {
        return all.stream().filter(ticket -> ticket.getSplit() == split).count();
    }

    public SplitResponse counts(Long datasetId, long seed) {
        long train = tickets.countByDatasetIdAndSplit(datasetId, DatasetSplit.TRAIN);
        long validation = tickets.countByDatasetIdAndSplit(datasetId, DatasetSplit.VALIDATION);
        long test = tickets.countByDatasetIdAndSplit(datasetId, DatasetSplit.TEST);
        long unassigned = tickets.countByDatasetIdAndSplit(datasetId, DatasetSplit.UNASSIGNED);
        return new SplitResponse(datasetId, seed, train, validation, test, unassigned);
    }

    private void validateRatios(SplitRequest request) {
        if (request.trainRatio() < 0 || request.validationRatio() < 0 || request.testRatio() < 0) {
            throw new IllegalArgumentException("Las proporciones no pueden ser negativas");
        }
        double sum = request.trainRatio() + request.validationRatio() + request.testRatio();
        if (Math.abs(sum - 1.0) > 0.000001) {
            throw new IllegalArgumentException("Las proporciones deben sumar 1.0");
        }
    }

    public record SplitRequest(double trainRatio, double validationRatio, double testRatio, long seed) {
        public SplitRequest {
            if (trainRatio == 0 && validationRatio == 0 && testRatio == 0) {
                trainRatio = 0.6;
                validationRatio = 0.1;
                testRatio = 0.3;
            }
        }
    }

    public record SplitResponse(Long datasetId, long seed, long train, long validation,
                                long test, long unassigned) {}
}

package pe.losmatus.itsmevaluator.dataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import pe.losmatus.itsmevaluator.ai.PromptCatalog;
import pe.losmatus.itsmevaluator.domain.DatasetSplit;
import pe.losmatus.itsmevaluator.persistence.*;

@Service
public class FineTuningExportService {
    private final DatasetRepository datasets;
    private final DatasetTicketRepository tickets;
    private final PromptCatalog prompts;
    private final ObjectMapper objectMapper;

    public FineTuningExportService(DatasetRepository datasets, DatasetTicketRepository tickets,
                                   PromptCatalog prompts, ObjectMapper objectMapper) {
        this.datasets = datasets;
        this.tickets = tickets;
        this.prompts = prompts;
        this.objectMapper = objectMapper;
    }

    public String exportCanonical(Long datasetId, DatasetSplit split) {
        validate(datasetId, split);
        var lines = new StringBuilder();
        for (var ticket : tickets.findAllByDatasetIdAndSplitOrderById(datasetId, split)) {
            var row = new LinkedHashMap<String, Object>();
            row.put("ticketId", ticket.getExternalTicketId());
            row.put("input", ticket.getDescription());
            row.put("output", Map.of(
                    "categoriaId", ticket.getActualCategoryId(),
                    "severidadId", ticket.getActualSeverityId()));
            append(lines, row);
        }
        return lines.toString();
    }

    public String exportOpenAiMessages(Long datasetId, DatasetSplit split) {
        validate(datasetId, split);
        var lines = new StringBuilder();
        for (var ticket : tickets.findAllByDatasetIdAndSplitOrderById(datasetId, split)) {
            var messages = List.of(
                    Map.of("role", "system", "content", prompts.systemPrompt()),
                    Map.of("role", "user", "content", prompts.userPrompt(ticket.getDescription())),
                    Map.of("role", "assistant", "content",
                            "{\"categoriaId\":" + ticket.getActualCategoryId()
                                    + ",\"severidadId\":" + ticket.getActualSeverityId() + "}"));
            append(lines, Map.of("messages", messages));
        }
        return lines.toString();
    }

    private void validate(Long datasetId, DatasetSplit split) {
        datasets.findById(datasetId).orElseThrow(() -> new NoSuchElementException("Dataset no encontrado"));
        if (split != DatasetSplit.TRAIN && split != DatasetSplit.VALIDATION) {
            throw new IllegalArgumentException(
                    "Fine-tuning solo puede exportar TRAIN o VALIDATION; TEST debe permanecer intacto");
        }
    }

    private void append(StringBuilder lines, Object row) {
        try {
            lines.append(objectMapper.writeValueAsString(row)).append('\n');
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo generar JSONL", ex);
        }
    }
}

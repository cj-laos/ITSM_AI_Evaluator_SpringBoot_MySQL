package pe.losmatus.itsmevaluator.dataset;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.losmatus.itsmevaluator.domain.DatasetSplit;
import pe.losmatus.itsmevaluator.persistence.DatasetEntity;
import pe.losmatus.itsmevaluator.persistence.DatasetRepository;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {
    private final DatasetRepository datasets;
    private final DatasetImportService importer;
    private final DatasetSplitService splitter;
    private final FineTuningExportService fineTuningExporter;

    public DatasetController(DatasetRepository datasets, DatasetImportService importer,
                             DatasetSplitService splitter,
                             FineTuningExportService fineTuningExporter) {
        this.datasets = datasets;
        this.importer = importer;
        this.splitter = splitter;
        this.fineTuningExporter = fineTuningExporter;
    }

    @GetMapping
    public List<DatasetEntity> list() {
        return datasets.findAll();
    }

    @PostMapping(path = "/import", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetImportService.ImportResponse importDataset(
            @RequestPart("file") MultipartFile file,
            @RequestParam String name,
            @RequestParam String version,
            @RequestParam(required = false) String description) throws IOException {
        return importer.importFile(file, name, version, description);
    }

    @PostMapping("/{datasetId}/split")
    public DatasetSplitService.SplitResponse split(@PathVariable Long datasetId,
                                                   @RequestBody DatasetSplitService.SplitRequest request) {
        return splitter.split(datasetId, request);
    }

    @GetMapping("/{datasetId}/split")
    public DatasetSplitService.SplitResponse splitCounts(@PathVariable Long datasetId) {
        return splitter.counts(datasetId, 0);
    }

    @GetMapping(value = "/{datasetId}/fine-tuning/canonical.jsonl", produces = "application/x-ndjson")
    public ResponseEntity<String> exportCanonical(@PathVariable Long datasetId,
                                                  @RequestParam(defaultValue = "TRAIN") DatasetSplit split) {
        return download("dataset-" + datasetId + "-" + split.name().toLowerCase() + "-canonical.jsonl",
                fineTuningExporter.exportCanonical(datasetId, split));
    }

    @GetMapping(value = "/{datasetId}/fine-tuning/openai.jsonl", produces = "application/x-ndjson")
    public ResponseEntity<String> exportOpenAi(@PathVariable Long datasetId,
                                               @RequestParam(defaultValue = "TRAIN") DatasetSplit split) {
        return download("dataset-" + datasetId + "-" + split.name().toLowerCase() + "-openai.jsonl",
                fineTuningExporter.exportOpenAiMessages(datasetId, split));
    }

    private ResponseEntity<String> download(String filename, String content) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}

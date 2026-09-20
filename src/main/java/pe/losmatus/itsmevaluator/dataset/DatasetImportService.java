package pe.losmatus.itsmevaluator.dataset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.losmatus.itsmevaluator.persistence.*;

@Service
public class DatasetImportService {
    private final DatasetRepository datasets;
    private final DatasetTicketRepository tickets;

    public DatasetImportService(DatasetRepository datasets, DatasetTicketRepository tickets) {
        this.datasets = datasets;
        this.tickets = tickets;
    }

    @Transactional
    public ImportResponse importFile(MultipartFile file, String name, String version, String description)
            throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if (datasets.findByNameAndVersion(name, version).isPresent()) {
            throw new IllegalArgumentException("Ya existe un dataset con el mismo nombre y versión");
        }

        var rows = readRows(file);
        if (rows.isEmpty()) throw new IllegalArgumentException("No se encontraron registros");
        var dataset = datasets.save(new DatasetEntity(name, version, description));
        var entities = new ArrayList<DatasetTicketEntity>(rows.size());
        var externalIds = new HashSet<String>();
        int rowNumber = 1;
        for (var row : rows) {
            rowNumber++;
            String externalId = required(row, rowNumber, "ticket_id", "id_ticket", "id");
            String ticketDescription = required(row, rowNumber, "descripcion", "descripción");
            int category = requiredInt(row, rowNumber, "categoria_id", "category_id");
            int severity = requiredInt(row, rowNumber, "severidad_id", "prioridad", "priority_id");
            if (category < 1 || category > 5) {
                throw new IllegalArgumentException("Fila " + rowNumber + ": categoria_id debe estar entre 1 y 5");
            }
            if (severity < 1 || severity > 4) {
                throw new IllegalArgumentException("Fila " + rowNumber + ": severidad_id debe estar entre 1 y 4");
            }
            if (!externalIds.add(externalId)) {
                throw new IllegalArgumentException("Fila " + rowNumber + ": ticket_id duplicado: " + externalId);
            }
            entities.add(new DatasetTicketEntity(dataset.getId(), externalId, ticketDescription,
                    category, severity, sha256(normalize(ticketDescription)),
                    optional(row, "origen_descripcion", "source_group")));
        }
        tickets.saveAll(entities);
        return new ImportResponse(dataset.getId(), dataset.getName(), dataset.getVersion(), entities.size());
    }

    private List<Map<String, String>> readRows(MultipartFile file) throws IOException {
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (filename.endsWith(".xlsx")) return readXlsx(file.getInputStream());
        if (filename.endsWith(".csv")) return readCsv(file.getInputStream());
        throw new IllegalArgumentException("Formato no admitido. Utiliza .xlsx o .csv");
    }

    private List<Map<String, String>> readXlsx(InputStream input) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheet("Dataset_Modelo");
            if (sheet == null) sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.rowIterator();
            if (!iterator.hasNext()) return List.of();
            var formatter = new DataFormatter(Locale.ROOT);
            var headers = cells(iterator.next(), formatter).stream().map(this::normalizeHeader).toList();
            var result = new ArrayList<Map<String, String>>();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                var values = cells(row, formatter);
                var mapped = new LinkedHashMap<String, String>();
                boolean hasValue = false;
                for (int i = 0; i < headers.size(); i++) {
                    String value = i < values.size() ? values.get(i).trim() : "";
                    mapped.put(headers.get(i), value);
                    hasValue |= !value.isBlank();
                }
                if (hasValue) result.add(mapped);
            }
            return result;
        }
    }

    private List<String> cells(Row row, DataFormatter formatter) {
        int count = Math.max(0, row.getLastCellNum());
        var values = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(cell == null ? "" : formatter.formatCellValue(cell));
        }
        return values;
    }

    private List<Map<String, String>> readCsv(InputStream input) throws IOException {
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8);
             var parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .get()
                     .parse(reader)) {
            var result = new ArrayList<Map<String, String>>();
            for (var record : parser) {
                var row = new LinkedHashMap<String, String>();
                record.toMap().forEach((key, value) -> row.put(normalizeHeader(key), value));
                result.add(row);
            }
            return result;
        }
    }

    private String required(Map<String, String> row, int line, String... aliases) {
        String value = optional(row, aliases);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fila " + line + ": falta " + aliases[0]);
        }
        return value.trim();
    }

    private int requiredInt(Map<String, String> row, int line, String... aliases) {
        String value = required(row, line, aliases);
        try {
            return Integer.parseInt(value.replace(".0", "").trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Fila " + line + ": " + aliases[0] + " no es entero");
        }
    }

    private String optional(Map<String, String> row, String... aliases) {
        for (String alias : aliases) {
            String value = row.get(normalizeHeader(alias));
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace(' ', '_');
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular el hash", ex);
        }
    }

    public record ImportResponse(Long datasetId, String name, String version, int importedTickets) {}
}

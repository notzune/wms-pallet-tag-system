/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.1
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.rail.*;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.DbQueryRepository;
import com.tbg.wms.db.OracleDbQueryRepository;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates WMS-backed rail preview and output generation.
 */
public final class RailWorkflowService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int ITEM_SLOTS = 13;

    private final AppConfig config;
    private final RailArtifactService artifactService;

    public RailWorkflowService(AppConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.artifactService = new RailArtifactService();
    }

    private static String normalizeTrainId(String trainId) {
        if (trainId == null || trainId.trim().isEmpty()) {
            throw new IllegalArgumentException("Train ID is required.");
        }
        return trainId.trim().toUpperCase(Locale.ROOT);
    }

    private static Set<String> collectShortCodes(List<RailStopRecord> records) {
        Set<String> codes = new TreeSet<>();
        for (RailStopRecord record : records) {
            for (RailStopRecord.ItemQuantity item : record.getItems()) {
                if (item != null && item.isValid()) {
                    codes.add(item.getItemNumber());
                }
            }
        }
        return codes;
    }

    private static RailDiagnostics buildDiagnostics(List<RailStopRecord> records,
                                                    List<RailLabelPlanner.PlannedRailLabel> planned,
                                                    Set<String> shortCodes,
                                                    Map<String, RailFamilyFootprint> resolved,
                                                    Map<String, List<RailFootprintCandidate>> wmsCandidates,
                                                    CsvFootprintLoad csvLoad) {
        int rowsWithMissing = 0;
        int rowsWithOverflow = 0;
        for (RailLabelPlanner.PlannedRailLabel row : planned) {
            if (!row.getMissingFootprintItems().isEmpty()) {
                rowsWithMissing++;
            }
            if (!row.getOverflowItems().isEmpty()) {
                rowsWithOverflow++;
            }
        }

        Set<String> missing = new TreeSet<>(shortCodes);
        missing.removeAll(resolved.keySet());

        Set<String> ambiguous = new TreeSet<>();
        for (Map.Entry<String, List<RailFootprintCandidate>> entry : wmsCandidates.entrySet()) {
            if (entry.getValue().size() > 1) {
                Set<String> signatures = entry.getValue().stream()
                        .map(candidate -> candidate.getItemNumber() + "|" + candidate.getFamilyCode() + "|" + candidate.getCasesPerPallet())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (signatures.size() > 1) {
                    ambiguous.add(entry.getKey());
                }
            }
        }

        return new RailDiagnostics(
                records.size(),
                planned.size(),
                shortCodes,
                missing,
                ambiguous,
                csvLoad.conflictingShortCodes,
                rowsWithMissing,
                rowsWithOverflow
        );
    }

    private static Map<String, RailFamilyFootprint> resolveWmsFootprints(Map<String, List<RailFootprintCandidate>> candidatesByShortCode) {
        Map<String, RailFamilyFootprint> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, List<RailFootprintCandidate>> entry : candidatesByShortCode.entrySet()) {
            String shortCode = entry.getKey();
            List<RailFootprintCandidate> candidates = entry.getValue();
            if (candidates == null || candidates.isEmpty()) {
                continue;
            }
            RailFootprintCandidate first = candidates.get(0);
            boolean consistent = true;
            for (int i = 1; i < candidates.size(); i++) {
                RailFootprintCandidate current = candidates.get(i);
                if (!first.getFamilyCode().equals(current.getFamilyCode())
                        || first.getCasesPerPallet() != current.getCasesPerPallet()) {
                    consistent = false;
                    break;
                }
            }
            if (!consistent) {
                continue;
            }
            resolved.put(shortCode, new RailFamilyFootprint(shortCode, first.getFamilyCode(), first.getCasesPerPallet()));
        }
        return resolved;
    }

    private static CsvFootprintLoad loadCsvFootprints(Path csvPath) throws Exception {
        if (csvPath == null || !Files.exists(csvPath) || !Files.isReadable(csvPath)) {
            return CsvFootprintLoad.empty();
        }

        Map<String, RailFamilyFootprint> byShortCode = new LinkedHashMap<>();
        Set<String> conflictingShortCodes = new TreeSet<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return CsvFootprintLoad.empty();
            }
            List<String> headers = RailCsvSupport.parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerIndex.put(RailCsvSupport.normalizeHeader(headers.get(i)), i);
            }

            int shortCodeCol = findColumn(headerIndex, "SHORTCODE", "UPC", "ALTPRTNUM");
            int familyCol = findColumn(headerIndex, "ITEMFAMILY", "FAMILY", "FAMILYCODE");
            int casesCol = findColumn(headerIndex, "CASEPA", "FOOTPRINT", "CASESPERPALLET", "UNITSPERPALLET");
            if (shortCodeCol < 0 || familyCol < 0 || casesCol < 0) {
                return CsvFootprintLoad.empty();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = RailCsvSupport.parseCsvLine(line);
                String shortCode = at(values, shortCodeCol);
                String family = at(values, familyCol);
                int cases = parseInt(at(values, casesCol));
                RailFamilyFootprint candidate = new RailFamilyFootprint(shortCode, family, cases);
                if (!candidate.isValid()) {
                    continue;
                }
                RailFamilyFootprint existing = byShortCode.get(candidate.getItemNumber());
                if (existing != null && (!existing.getFamilyCode().equals(candidate.getFamilyCode())
                        || existing.getCasesPerPallet() != candidate.getCasesPerPallet())) {
                    conflictingShortCodes.add(candidate.getItemNumber());
                    continue;
                }
                byShortCode.put(candidate.getItemNumber(), candidate);
            }
        }

        return new CsvFootprintLoad(byShortCode, conflictingShortCodes);
    }

    private static int parseInt(String value) {
        if (value == null) {
            return 0;
        }
        String normalized = value.trim().replace(",", "");
        if (normalized.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String at(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index) == null ? "" : values.get(index).trim();
    }

    private static int findColumn(Map<String, Integer> indexByHeader, String... normalizedNames) {
        for (String name : normalizedNames) {
            Integer index = indexByHeader.get(name);
            if (index != null) {
                return index;
            }
        }
        return -1;
    }

    private static String nonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                parts.add(value);
            }
        }
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    public String defaultFootprintCsvPath() {
        Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
        return downloads.resolve("JC Labels v7 (Updated 1.5.21).csv").toString();
    }

    public PreparedRailJob prepareRailJob(String trainId, Path csvOverridePath, boolean useCsvOverride) throws Exception {
        String normalizedTrainId = normalizeTrainId(trainId);
        try (DbConnectionPool pool = new DbConnectionPool(config)) {
            DbQueryRepository repo = new OracleDbQueryRepository(pool.getDataSource());

            List<RailStopRecord> records = repo.findRailStopsByTrainId(normalizedTrainId);
            if (records.isEmpty()) {
                throw new IllegalArgumentException("No rail rows found for train: " + normalizedTrainId);
            }

            Set<String> shortCodes = collectShortCodes(records);
            Map<String, List<RailFootprintCandidate>> wmsCandidates = repo.findRailFootprintsByShortCode(new ArrayList<>(shortCodes));
            Map<String, RailFamilyFootprint> resolved = resolveWmsFootprints(wmsCandidates);

            CsvFootprintLoad csvLoad = CsvFootprintLoad.empty();
            if (useCsvOverride && csvOverridePath != null && Files.exists(csvOverridePath)) {
                csvLoad = loadCsvFootprints(csvOverridePath);
                resolved.putAll(csvLoad.byShortCode);
            }

            RailLabelPlanner planner = new RailLabelPlanner(ITEM_SLOTS);
            List<RailLabelPlanner.PlannedRailLabel> planned = planner.plan(records, resolved);
            RailDiagnostics diagnostics = buildDiagnostics(records, planned, shortCodes, resolved, wmsCandidates, csvLoad);
            return new PreparedRailJob(
                    normalizedTrainId,
                    records,
                    planned,
                    resolved,
                    diagnostics,
                    csvOverridePath,
                    useCsvOverride
            );
        }
    }

    public GenerationResult generateArtifacts(PreparedRailJob job, Path outputDir, Path templateDocx) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        Path targetDir = outputDir == null
                ? Path.of("out", "rail-gui-" + job.trainId + "-" + TS.format(LocalDateTime.now()))
                : outputDir;
        Files.createDirectories(targetDir);

        Path csvPath = targetDir.resolve("_TrainDetail.csv");
        new RailTrainDetailExporter().exportTrainDetailCsv(job.plannedRows, csvPath);

        Path summaryPath = targetDir.resolve("rail-helper-summary.txt");
        Files.writeString(summaryPath, buildSummaryText(job));

        RailArtifactService.WordArtifactResult wordArtifacts =
                artifactService.generateWordArtifacts(templateDocx, csvPath, targetDir);
        return new GenerationResult(targetDir, csvPath, summaryPath, wordArtifacts);
    }

    public String buildMergePreviewText(PreparedRailJob job, int maxRows) {
        StringBuilder sb = new StringBuilder();
        sb.append("Merge Row Preview\n");
        sb.append("=================\n");
        int shown = 0;
        for (RailLabelPlanner.PlannedRailLabel row : job.plannedRows) {
            if (shown >= maxRows) {
                break;
            }
            Map<String, String> fields = row.toMergeFields();
            sb.append("Row ").append(shown + 1).append(": ")
                    .append(fields.getOrDefault("TRAIN_NBR", ""))
                    .append(" / seq ").append(fields.getOrDefault("SEQ", ""))
                    .append(" / vehicle ").append(fields.getOrDefault("VEHICLE_ID", ""))
                    .append(" / load ").append(fields.getOrDefault("LOAD_NBR", ""))
                    .append('\n');
            for (int i = 1; i <= ITEM_SLOTS; i++) {
                String item = fields.getOrDefault("ITEM_NBR_" + i, "");
                String qty = fields.getOrDefault("TOTAL_CS_ITM_" + i, "");
                if (!item.isBlank()) {
                    sb.append("  [").append(i).append("] ")
                            .append(item)
                            .append(" -> ")
                            .append(qty)
                            .append('\n');
                }
            }
            sb.append("  Families: ")
                    .append(nonBlank(fields.getOrDefault("Item_1", ""), fields.getOrDefault("Item_2", ""), fields.getOrDefault("Item_3", "")))
                    .append('\n');
            if (!row.getMissingFootprintItems().isEmpty()) {
                sb.append("  Missing footprint: ").append(String.join(", ", row.getMissingFootprintItems())).append('\n');
            }
            if (!row.getOverflowItems().isEmpty()) {
                sb.append("  Overflow (>13): ")
                        .append(row.getOverflowItems().stream()
                                .map(RailStopRecord.ItemQuantity::getItemNumber)
                                .collect(Collectors.joining(", ")))
                        .append('\n');
            }
            sb.append('\n');
            shown++;
        }
        if (job.plannedRows.size() > shown) {
            sb.append("... ").append(job.plannedRows.size() - shown).append(" more rows not shown.\n");
        }
        return sb.toString();
    }

    public String buildDiagnosticsText(PreparedRailJob job) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rail Diagnostics\n");
        sb.append("================\n");
        sb.append("Train ID: ").append(job.trainId).append('\n');
        sb.append("Rows loaded from WMS: ").append(job.records.size()).append('\n');
        sb.append("Rows planned: ").append(job.plannedRows.size()).append('\n');
        sb.append("Distinct short codes: ").append(job.diagnostics.shortCodes.size()).append('\n');
        sb.append("Resolved footprints: ").append(job.resolvedFootprints.size()).append('\n');
        sb.append("Missing footprint short codes: ").append(job.diagnostics.missingFootprintShortCodes.size()).append('\n');
        sb.append("Rows with missing footprint: ").append(job.diagnostics.rowsWithMissingFootprint).append('\n');
        sb.append("Rows exceeding 13 item slots: ").append(job.diagnostics.rowsWithOverflow).append('\n');
        sb.append("Ambiguous WMS short-code mappings: ").append(job.diagnostics.ambiguousWmsShortCodes.size()).append('\n');
        sb.append("CSV duplicate conflicts: ").append(job.diagnostics.csvConflictingShortCodes.size()).append('\n');
        sb.append('\n');

        if (!job.diagnostics.ambiguousWmsShortCodes.isEmpty()) {
            sb.append("Ambiguous WMS mappings:\n");
            for (String code : job.diagnostics.ambiguousWmsShortCodes) {
                sb.append(" - ").append(code).append('\n');
            }
            sb.append('\n');
        }
        if (!job.diagnostics.csvConflictingShortCodes.isEmpty()) {
            sb.append("Conflicting CSV short codes:\n");
            for (String code : job.diagnostics.csvConflictingShortCodes) {
                sb.append(" - ").append(code).append('\n');
            }
            sb.append('\n');
        }
        if (!job.diagnostics.missingFootprintShortCodes.isEmpty()) {
            sb.append("Missing footprint short codes:\n");
            for (String code : job.diagnostics.missingFootprintShortCodes) {
                sb.append(" - ").append(code).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String buildSummaryText(PreparedRailJob job) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rail Helper Summary\n");
        sb.append("===================\n");
        sb.append("Train ID: ").append(job.trainId).append('\n');
        sb.append("Rows exported: ").append(job.plannedRows.size()).append('\n');
        sb.append("Distinct short codes: ").append(job.diagnostics.shortCodes.size()).append('\n');
        sb.append("Resolved footprints: ").append(job.resolvedFootprints.size()).append('\n');
        sb.append("Rows with missing footprint: ").append(job.diagnostics.rowsWithMissingFootprint).append('\n');
        sb.append("Rows exceeding item slot limit: ").append(job.diagnostics.rowsWithOverflow).append('\n');
        sb.append("Ambiguous WMS short-code mappings: ").append(job.diagnostics.ambiguousWmsShortCodes.size()).append('\n');
        sb.append("CSV duplicate conflicts: ").append(job.diagnostics.csvConflictingShortCodes.size()).append('\n');
        if (!job.diagnostics.missingFootprintShortCodes.isEmpty()) {
            sb.append("Missing short codes: ").append(String.join(", ", job.diagnostics.missingFootprintShortCodes)).append('\n');
        }
        sb.append('\n');
        sb.append("Template merge fields used:\n");
        sb.append("DATE, SEQ, TRAIN_NBR, VEHICLE_ID, DCS_WHSE, LOAD_NBR, ITEM_NBR_1..13, TOTAL_CS_ITM_1..13, Item_1..3\n");
        return sb.toString();
    }

    private static final class CsvFootprintLoad {
        private final Map<String, RailFamilyFootprint> byShortCode;
        private final Set<String> conflictingShortCodes;

        private CsvFootprintLoad(Map<String, RailFamilyFootprint> byShortCode, Set<String> conflictingShortCodes) {
            this.byShortCode = Collections.unmodifiableMap(new LinkedHashMap<>(byShortCode));
            this.conflictingShortCodes = Collections.unmodifiableSet(new TreeSet<>(conflictingShortCodes));
        }

        private static CsvFootprintLoad empty() {
            return new CsvFootprintLoad(Map.of(), Set.of());
        }
    }

    public static final class PreparedRailJob {
        private final String trainId;
        private final List<RailStopRecord> records;
        private final List<RailLabelPlanner.PlannedRailLabel> plannedRows;
        private final Map<String, RailFamilyFootprint> resolvedFootprints;
        private final RailDiagnostics diagnostics;
        private final Path csvOverridePath;
        private final boolean useCsvOverride;

        private PreparedRailJob(String trainId,
                                List<RailStopRecord> records,
                                List<RailLabelPlanner.PlannedRailLabel> plannedRows,
                                Map<String, RailFamilyFootprint> resolvedFootprints,
                                RailDiagnostics diagnostics,
                                Path csvOverridePath,
                                boolean useCsvOverride) {
            this.trainId = trainId;
            this.records = Collections.unmodifiableList(new ArrayList<>(records));
            this.plannedRows = Collections.unmodifiableList(new ArrayList<>(plannedRows));
            this.resolvedFootprints = Collections.unmodifiableMap(new LinkedHashMap<>(resolvedFootprints));
            this.diagnostics = diagnostics;
            this.csvOverridePath = csvOverridePath;
            this.useCsvOverride = useCsvOverride;
        }

        public String getTrainId() {
            return trainId;
        }

        public List<RailLabelPlanner.PlannedRailLabel> getPlannedRows() {
            return plannedRows;
        }

        public RailDiagnostics getDiagnostics() {
            return diagnostics;
        }

        public Path getCsvOverridePath() {
            return csvOverridePath;
        }

        public boolean isUseCsvOverride() {
            return useCsvOverride;
        }
    }

    public static final class RailDiagnostics {
        private final int wmsRows;
        private final int plannedRows;
        private final Set<String> shortCodes;
        private final Set<String> missingFootprintShortCodes;
        private final Set<String> ambiguousWmsShortCodes;
        private final Set<String> csvConflictingShortCodes;
        private final int rowsWithMissingFootprint;
        private final int rowsWithOverflow;

        private RailDiagnostics(int wmsRows,
                                int plannedRows,
                                Set<String> shortCodes,
                                Set<String> missingFootprintShortCodes,
                                Set<String> ambiguousWmsShortCodes,
                                Set<String> csvConflictingShortCodes,
                                int rowsWithMissingFootprint,
                                int rowsWithOverflow) {
            this.wmsRows = wmsRows;
            this.plannedRows = plannedRows;
            this.shortCodes = Collections.unmodifiableSet(new TreeSet<>(shortCodes));
            this.missingFootprintShortCodes = Collections.unmodifiableSet(new TreeSet<>(missingFootprintShortCodes));
            this.ambiguousWmsShortCodes = Collections.unmodifiableSet(new TreeSet<>(ambiguousWmsShortCodes));
            this.csvConflictingShortCodes = Collections.unmodifiableSet(new TreeSet<>(csvConflictingShortCodes));
            this.rowsWithMissingFootprint = rowsWithMissingFootprint;
            this.rowsWithOverflow = rowsWithOverflow;
        }
    }

    public static final class GenerationResult {
        private final Path outputDirectory;
        private final Path mergeCsvPath;
        private final Path summaryPath;
        private final RailArtifactService.WordArtifactResult wordArtifacts;

        private GenerationResult(Path outputDirectory,
                                 Path mergeCsvPath,
                                 Path summaryPath,
                                 RailArtifactService.WordArtifactResult wordArtifacts) {
            this.outputDirectory = outputDirectory;
            this.mergeCsvPath = mergeCsvPath;
            this.summaryPath = summaryPath;
            this.wordArtifacts = wordArtifacts;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public Path getMergeCsvPath() {
            return mergeCsvPath;
        }

        public Path getSummaryPath() {
            return summaryPath;
        }

        public RailArtifactService.WordArtifactResult getWordArtifacts() {
            return wordArtifacts;
        }
    }
}

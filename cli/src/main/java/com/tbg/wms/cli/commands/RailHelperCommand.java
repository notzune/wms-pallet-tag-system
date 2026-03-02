/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.cli.commands;

import com.tbg.wms.core.rail.RailFamilyFootprint;
import com.tbg.wms.core.rail.RailLabelPlanner;
import com.tbg.wms.core.rail.RailStopRecord;
import com.tbg.wms.core.rail.RailTrainDetailExporter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates merge-ready rail label data with deterministic family footprint math.
 */
@Command(
        name = "rail-helper",
        description = "Build rail office merge data from CSV input and footprint lookup"
)
public final class RailHelperCommand implements Callable<Integer> {

    private static final DateTimeFormatter DEFAULT_DATE = DateTimeFormatter.ofPattern("MM-dd-yy");
    private static final Pattern ITEM_HEADER_PATTERN = Pattern.compile("ITEMNBR(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern QTY_HEADER_PATTERN = Pattern.compile("TOTALCSITM(\\d+)", Pattern.CASE_INSENSITIVE);

    @Option(names = {"--input-csv"}, required = true, description = "Input rail data CSV path")
    private Path inputCsv;

    @Option(names = {"--item-footprint-csv"}, required = true, description = "Item footprint CSV path")
    private Path itemFootprintCsv;

    @Option(names = {"--output-dir"}, defaultValue = "out/rail-helper", description = "Output directory path")
    private Path outputDir;

    @Option(names = {"--template-docx"}, description = "Optional Word template to copy beside output CSV")
    private Path templateDocx;

    @Option(names = {"--train-id"}, description = "Optional train ID filter")
    private String trainIdFilter;

    /**
     * Builds merge-ready rail output from input and footprint CSVs.
     *
     * @return exit code (0 success)
     * @throws Exception when parsing/export fails
     */
    @Override
    public Integer call() throws Exception {
        ensureReadable(inputCsv, "input-csv");
        ensureReadable(itemFootprintCsv, "item-footprint-csv");
        if (templateDocx != null) {
            ensureReadable(templateDocx, "template-docx");
        }

        Map<String, RailFamilyFootprint> footprintByItem = loadFootprintMap(itemFootprintCsv);
        List<RailStopRecord> records = loadRailRecords(inputCsv);
        if (trainIdFilter != null && !trainIdFilter.isBlank()) {
            records = filterByTrainId(records, trainIdFilter.trim());
        }
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No rail records found after parsing/filter.");
        }

        RailLabelPlanner planner = new RailLabelPlanner();
        List<RailLabelPlanner.PlannedRailLabel> plannedRows = planner.plan(records, footprintByItem);

        Files.createDirectories(outputDir);
        Path trainDetailCsv = outputDir.resolve("_TrainDetail.csv");
        new RailTrainDetailExporter().exportTrainDetailCsv(plannedRows, trainDetailCsv);
        Path summary = outputDir.resolve("rail-helper-summary.txt");
        Files.writeString(summary, buildSummary(plannedRows, footprintByItem.size()));

        if (templateDocx != null) {
            Path copied = outputDir.resolve(templateDocx.getFileName());
            Files.copy(templateDocx, copied, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Rail helper output generated:");
        System.out.println(" - Merge CSV: " + trainDetailCsv.toAbsolutePath());
        System.out.println(" - Summary: " + summary.toAbsolutePath());
        if (templateDocx != null) {
            System.out.println(" - Copied template: " + outputDir.resolve(templateDocx.getFileName()).toAbsolutePath());
        }
        System.out.println("Word template merge fields expected: DATE, SEQ, TRAIN_NBR, VEHICLE_ID, DCS_WHSE, LOAD_NBR, ITEM_NBR_1..6, TOTAL_CS_ITM_1..6, Item_1..3");
        return 0;
    }

    private void ensureReadable(Path path, String optionName) {
        Objects.requireNonNull(path, optionName + " path cannot be null");
        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Invalid " + optionName + ": " + path);
        }
    }

    private List<RailStopRecord> filterByTrainId(List<RailStopRecord> records, String trainId) {
        List<RailStopRecord> filtered = new ArrayList<>();
        for (RailStopRecord record : records) {
            if (trainId.equals(record.getTrainNumber())) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private Map<String, RailFamilyFootprint> loadFootprintMap(Path csvPath) throws Exception {
        Map<String, RailFamilyFootprint> map = new HashMap<>();
        readCsv(csvPath, row -> {
            String item = normalize(raw(row, "ITEM_NBR", "ITEM", "SKU", "PRTNUM", "A"));
            String family = normalize(raw(row, "ITEM_FAMILY", "FAMILY", "FAMILY_CODE", "H"));
            int footprint = parseInt(raw(row, "FOOTPRINT", "CASES_PER_PALLET", "UNITS_PER_PALLET", "J"), 0);
            RailFamilyFootprint entry = new RailFamilyFootprint(item, family, footprint);
            if (entry.isValid()) {
                map.put(entry.getItemNumber(), entry);
            }
        });
        return map;
    }

    private List<RailStopRecord> loadRailRecords(Path csvPath) throws Exception {
        List<RailStopRecord> records = new ArrayList<>();
        readCsv(csvPath, row -> {
            Map<Integer, String> itemBySlot = new TreeMapNumericKeyMap().extract(row, ITEM_HEADER_PATTERN);
            Map<Integer, Integer> qtyBySlot = new TreeMapNumericKeyMap().extractInt(row, QTY_HEADER_PATTERN);
            List<RailStopRecord.ItemQuantity> items = new ArrayList<>();
            for (Map.Entry<Integer, String> entry : itemBySlot.entrySet()) {
                int slot = entry.getKey();
                String item = normalize(entry.getValue());
                int cases = qtyBySlot.getOrDefault(slot, 0);
                if (!item.isBlank() && cases > 0) {
                    items.add(new RailStopRecord.ItemQuantity(item, cases));
                }
            }
            if (items.isEmpty()) {
                return;
            }

            String date = normalize(raw(row, "DATE"));
            if (date.isBlank()) {
                date = LocalDate.now().format(DEFAULT_DATE);
            }

            String sequence = normalize(raw(row, "SEQ", "SEQUENCE", "STOP", "STOP_SEQ"));
            String train = normalize(raw(row, "TRAIN_NBR", "TRAIN", "TRAIN_ID"));
            String vehicle = normalize(raw(row, "VEHICLE_ID", "VEHICLE", "CAR_NBR", "CAR"));
            String warehouse = normalize(raw(row, "DCS_WHSE", "WHSE", "WH", "WAREHOUSE"));
            String load = normalize(raw(row, "LOAD_NBR", "LOAD", "LOAD_ID"));

            records.add(new RailStopRecord(date, sequence, train, vehicle, warehouse, load, items));
        });

        records.sort(Comparator.comparing(RailStopRecord::getTrainNumber)
                .thenComparing(RailStopRecord::getSequence)
                .thenComparing(RailStopRecord::getLoadNumber));
        return records;
    }

    private String buildSummary(List<RailLabelPlanner.PlannedRailLabel> plannedRows, int footprintCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rail Helper Summary\n");
        sb.append("===================\n");
        sb.append("Rows exported: ").append(plannedRows.size()).append('\n');
        sb.append("Footprint items loaded: ").append(footprintCount).append('\n');

        int totalMissingRows = 0;
        TreeSet<String> missingItems = new TreeSet<>();
        for (RailLabelPlanner.PlannedRailLabel row : plannedRows) {
            if (!row.getMissingFootprintItems().isEmpty()) {
                totalMissingRows++;
                missingItems.addAll(row.getMissingFootprintItems());
            }
        }

        sb.append("Rows with missing footprint: ").append(totalMissingRows).append('\n');
        if (!missingItems.isEmpty()) {
            sb.append("Missing items: ").append(String.join(", ", missingItems)).append('\n');
        }

        sb.append('\n');
        sb.append("Template merge fields used:\n");
        sb.append("DATE, SEQ, TRAIN_NBR, VEHICLE_ID, DCS_WHSE, LOAD_NBR, ITEM_NBR_1..6, TOTAL_CS_ITM_1..6, Item_1..3\n");
        return sb.toString();
    }

    private static String raw(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(normalizeHeader(key));
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.replace(",", "").trim();
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void readCsv(Path path, CsvRowConsumer rowConsumer) throws Exception {
        List<String> normalizedHeaders;
        try (java.io.BufferedReader reader = Files.newBufferedReader(path)) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return;
            }
            List<String> headers = parseCsvLine(firstLine);
            normalizedHeaders = new ArrayList<>(headers.size());
            for (String header : headers) {
                normalizedHeaders.add(normalizeHeader(header));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cells = parseCsvLine(line);
                Map<String, String> row = new HashMap<>();
                for (int col = 0; col < normalizedHeaders.size(); col++) {
                    String key = normalizedHeaders.get(col);
                    String value = col < cells.size() ? cells.get(col) : "";
                    row.put(key, value);
                }
                rowConsumer.accept(Collections.unmodifiableMap(row));
            }
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    /**
     * Extracts numbered column groups such as ITEM_NBR_1 ... ITEM_NBR_13.
     */
    private static final class TreeMapNumericKeyMap {
        Map<Integer, String> extract(Map<String, String> row, Pattern pattern) {
            Map<Integer, String> byIndex = new java.util.TreeMap<>();
            for (Map.Entry<String, String> entry : row.entrySet()) {
                Matcher matcher = pattern.matcher(entry.getKey());
                if (!matcher.find()) {
                    continue;
                }
                int index = parseInt(matcher.group(1), -1);
                if (index > 0) {
                    byIndex.put(index, entry.getValue());
                }
            }
            return byIndex;
        }

        Map<Integer, Integer> extractInt(Map<String, String> row, Pattern pattern) {
            Map<Integer, Integer> byIndex = new java.util.TreeMap<>();
            for (Map.Entry<String, String> entry : row.entrySet()) {
                Matcher matcher = pattern.matcher(entry.getKey());
                if (!matcher.find()) {
                    continue;
                }
                int index = parseInt(matcher.group(1), -1);
                if (index > 0) {
                    byIndex.put(index, parseInt(entry.getValue(), 0));
                }
            }
            return byIndex;
        }
    }

    @FunctionalInterface
    private interface CsvRowConsumer {
        void accept(Map<String, String> row) throws Exception;
    }
}

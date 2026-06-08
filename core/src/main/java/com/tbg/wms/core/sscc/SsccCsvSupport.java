package com.tbg.wms.core.sscc;

import com.tbg.wms.core.rail.RailCsvSupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CSV import helpers for SSCC label rows.
 */
public final class SsccCsvSupport {
    private static final List<String> REQUIRED_HEADERS = List.of(
            "Sales Order #",
            "Purchase Order #",
            "Shipment #",
            "Carrier Code",
            "Trailer ID",
            "Destination",
            "Destination Address",
            "Customer Name",
            "Facility",
            "Item #",
            "Level 2 Reference #",
            "Originally Shipped LPN",
            "Sum of Ship Cases",
            "New Received LPN"
    );

    private SsccCsvSupport() {
    }

    public static List<String> requiredHeaders() {
        return REQUIRED_HEADERS;
    }

    public static List<SsccLabelRow> parse(Path csvPath) throws IOException {
        Objects.requireNonNull(csvPath, "csvPath cannot be null");
        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        return parse(lines);
    }

    public static List<SsccLabelRow> parse(List<String> lines) {
        Objects.requireNonNull(lines, "lines cannot be null");
        if (lines.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> columns = headerColumns(lines.get(0));
        validateRequiredHeaders(columns);

        List<SsccLabelRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            List<String> values = RailCsvSupport.parseCsvLine(line);
            rows.add(parseRow(values, columns));
        }
        return List.copyOf(rows);
    }

    private static SsccLabelRow parseRow(List<String> values, Map<String, Integer> columns) {
        return new SsccLabelRow(
                value(values, columns, "Sales Order #"),
                value(values, columns, "Purchase Order #"),
                value(values, columns, "Shipment #"),
                value(values, columns, "Carrier Code"),
                value(values, columns, "Trailer ID"),
                value(values, columns, "Destination"),
                value(values, columns, "Destination Address"),
                value(values, columns, "Customer Name"),
                value(values, columns, "Facility"),
                value(values, columns, "Item #"),
                value(values, columns, "Level 2 Reference #"),
                value(values, columns, "Originally Shipped LPN"),
                parseDouble(value(values, columns, "Sum of Ship Cases")),
                value(values, columns, "New Received LPN")
        ).withNonNegativeCases();
    }

    private static Map<String, Integer> headerColumns(String headerLine) {
        List<String> headers = RailCsvSupport.parseCsvLine(headerLine);
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String key = RailCsvSupport.normalizeHeader(header);
            if (!key.isBlank()) {
                columns.put(key, i);
            }
        }
        return columns;
    }

    private static void validateRequiredHeaders(Map<String, Integer> columns) {
        List<String> missing = new ArrayList<>();
        for (String header : REQUIRED_HEADERS) {
            if (!columns.containsKey(RailCsvSupport.normalizeHeader(header))) {
                missing.add(header);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("CSV is missing required headers: " + String.join(", ", missing));
        }
    }

    private static String value(List<String> values, Map<String, Integer> columns, String header) {
        Integer index = columns.get(RailCsvSupport.normalizeHeader(header));
        if (index == null || index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index).trim();
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0d;
        }
        String normalized = value.trim();
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric value in 'Sum of Ship Cases': " + normalized, ex);
        }
    }
}

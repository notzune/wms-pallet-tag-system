/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.commands.rail;

import com.tbg.wms.core.rail.RailCsvSupport;
import com.tbg.wms.core.rail.RailFamilyFootprint;
import com.tbg.wms.core.rail.RailStopRecord;

import java.io.BufferedReader;
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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads rail-helper CSV inputs into normalized domain records.
 */
final class RailHelperDataSupport {

    private static final DateTimeFormatter DEFAULT_DATE = DateTimeFormatter.ofPattern("MM-dd-yy");
    private static final Pattern ITEM_HEADER_PATTERN = Pattern.compile("ITEMNBR(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern QTY_HEADER_PATTERN = Pattern.compile("TOTALCSITM(\\d+)", Pattern.CASE_INSENSITIVE);

    Map<String, RailFamilyFootprint> loadFootprintMap(Path csvPath) throws Exception {
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

    List<RailStopRecord> loadRailRecords(Path csvPath) throws Exception {
        List<RailStopRecord> records = new ArrayList<>();
        readCsv(csvPath, row -> {
            Map<Integer, String> itemBySlot = extract(row, ITEM_HEADER_PATTERN);
            Map<Integer, Integer> qtyBySlot = extractInt(row, QTY_HEADER_PATTERN);
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

    List<RailStopRecord> filterByTrainId(List<RailStopRecord> records, String trainId) {
        String normalizedFilter = normalize(trainId).toUpperCase(Locale.ROOT);
        List<RailStopRecord> filtered = new ArrayList<>();
        for (RailStopRecord record : records) {
            if (record.getTrainNumber().toUpperCase(Locale.ROOT).equals(normalizedFilter)) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private void readCsv(Path path, CsvRowConsumer rowConsumer) throws Exception {
        List<String> normalizedHeaders;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return;
            }
            List<String> headers = RailCsvSupport.parseCsvLine(firstLine);
            normalizedHeaders = new ArrayList<>(headers.size());
            for (String header : headers) {
                normalizedHeaders.add(RailCsvSupport.normalizeHeader(header));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cells = RailCsvSupport.parseCsvLine(line);
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

    private static String raw(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(RailCsvSupport.normalizeHeader(key));
            if (value != null) {
                return value;
            }
        }
        return "";
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

    private static Map<Integer, String> extract(Map<String, String> row, Pattern pattern) {
        Map<Integer, String> byIndex = new TreeMap<>();
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

    private static Map<Integer, Integer> extractInt(Map<String, String> row, Pattern pattern) {
        Map<Integer, Integer> byIndex = new TreeMap<>();
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

    @FunctionalInterface
    private interface CsvRowConsumer {
        void accept(Map<String, String> row) throws Exception;
    }
}

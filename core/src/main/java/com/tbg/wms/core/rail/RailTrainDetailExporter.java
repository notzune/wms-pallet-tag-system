package com.tbg.wms.core.rail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes rail merge output in `_TrainDetail.csv` layout consumed by the Word template.
 */
public final class RailTrainDetailExporter {

    private static final List<String> MERGE_HEADERS = List.of(
            "DATE",
            "Item_1",
            "Item_2",
            "Item_3",
            "SEQ",
            "TRAIN_NBR",
            "VEHICLE_ID",
            "DCS_WHSE",
            "LOAD_NBR",
            "ITEM_NBR_1",
            "TOTAL_CS_ITM_1",
            "ITEM_NBR_2",
            "TOTAL_CS_ITM_2",
            "ITEM_NBR_3",
            "TOTAL_CS_ITM_3",
            "ITEM_NBR_4",
            "TOTAL_CS_ITM_4",
            "ITEM_NBR_5",
            "TOTAL_CS_ITM_5",
            "ITEM_NBR_6",
            "TOTAL_CS_ITM_6"
    );

    /**
     * Exports merge rows with template-compatible column names.
     *
     * @param plannedRows planned rail rows
     * @param outputCsv output file path
     * @throws IOException if output cannot be written
     */
    public void exportTrainDetailCsv(List<RailLabelPlanner.PlannedRailLabel> plannedRows, Path outputCsv) throws IOException {
        Objects.requireNonNull(plannedRows, "plannedRows cannot be null");
        Objects.requireNonNull(outputCsv, "outputCsv cannot be null");
        Files.createDirectories(outputCsv.toAbsolutePath().getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(outputCsv, StandardCharsets.UTF_8)) {
            writer.write(String.join(",", MERGE_HEADERS));
            writer.newLine();
            for (RailLabelPlanner.PlannedRailLabel row : plannedRows) {
                if (row == null) {
                    continue;
                }
                Map<String, String> fields = row.toMergeFields();
                writer.write(toCsvLine(fields));
                writer.newLine();
            }
        }
    }

    private static String toCsvLine(Map<String, String> fields) {
        List<String> values = new ArrayList<>(MERGE_HEADERS.size());
        for (String header : MERGE_HEADERS) {
            values.add(escapeCsv(fields.getOrDefault(header, "")));
        }
        return String.join(",", values);
    }

    private static String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        boolean needsQuotes = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        if (!needsQuotes) {
            return safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}

/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.commands.rail;

import com.tbg.wms.core.rail.RailLabelPlanner;

import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;

/**
 * Builds rail-helper summary text and console output lines.
 *
 * <p>This keeps report wording and merge-field guidance separate from CSV loading/planning so
 * operator-facing rail-helper output can evolve without touching the data path.</p>
 */
final class RailHelperOutputSupport {

    String buildSummary(List<RailLabelPlanner.PlannedRailLabel> plannedRows, int footprintCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rail Helper Summary\n");
        sb.append("===================\n");
        sb.append("Rows exported: ").append(plannedRows.size()).append('\n');
        sb.append("Footprint items loaded: ").append(footprintCount).append('\n');

        int totalMissingRows = 0;
        int totalOverflowRows = 0;
        TreeSet<String> missingItems = new TreeSet<>();
        for (RailLabelPlanner.PlannedRailLabel row : plannedRows) {
            if (!row.getMissingFootprintItems().isEmpty()) {
                totalMissingRows++;
                missingItems.addAll(row.getMissingFootprintItems());
            }
            if (!row.getOverflowItems().isEmpty()) {
                totalOverflowRows++;
            }
        }

        sb.append("Rows with missing footprint: ").append(totalMissingRows).append('\n');
        sb.append("Rows exceeding item slot limit: ").append(totalOverflowRows).append('\n');
        if (!missingItems.isEmpty()) {
            sb.append("Missing items: ").append(String.join(", ", missingItems)).append('\n');
        }

        sb.append('\n');
        sb.append("Template merge fields used:\n");
        sb.append("DATE, SEQ, TRAIN_NBR, VEHICLE_ID, DCS_WHSE, LOAD_NBR, ITEM_NBR_1..13, TOTAL_CS_ITM_1..13, Item_1..3\n");
        return sb.toString();
    }

    List<String> buildSuccessLines(Path trainDetailCsv, Path summary, Path copiedTemplate) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("Rail helper output generated:");
        lines.add(" - Merge CSV: " + trainDetailCsv.toAbsolutePath());
        lines.add(" - Summary: " + summary.toAbsolutePath());
        if (copiedTemplate != null) {
            lines.add(" - Copied template: " + copiedTemplate.toAbsolutePath());
        }
        lines.add("Word template merge fields expected: DATE, SEQ, TRAIN_NBR, VEHICLE_ID, DCS_WHSE, LOAD_NBR, ITEM_NBR_1..13, TOTAL_CS_ITM_1..13, Item_1..3");
        return List.copyOf(lines);
    }
}

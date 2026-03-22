/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.cli.commands.rail;

import com.tbg.wms.core.rail.RailFamilyFootprint;
import com.tbg.wms.core.rail.RailLabelPlanner;
import com.tbg.wms.core.rail.RailStopRecord;
import com.tbg.wms.core.rail.RailTrainDetailExporter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Generates merge-ready rail label data with deterministic family footprint math.
 */
@Command(
        name = "rail-helper",
        description = "Build rail office merge data from CSV input and footprint lookup"
)
public final class RailHelperCommand implements Callable<Integer> {

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
    private final RailHelperDataSupport dataSupport = new RailHelperDataSupport();

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

        Map<String, RailFamilyFootprint> footprintByItem = dataSupport.loadFootprintMap(itemFootprintCsv);
        List<RailStopRecord> records = dataSupport.loadRailRecords(inputCsv);
        if (trainIdFilter != null && !trainIdFilter.isBlank()) {
            records = dataSupport.filterByTrainId(records, trainIdFilter.trim());
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
        System.out.println("Word template merge fields expected: DATE, SEQ, TRAIN_NBR, VEHICLE_ID, DCS_WHSE, LOAD_NBR, ITEM_NBR_1..13, TOTAL_CS_ITM_1..13, Item_1..3");
        return 0;
    }

    private void ensureReadable(Path path, String optionName) {
        Objects.requireNonNull(path, optionName + " path cannot be null");
        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Invalid " + optionName + ": " + path);
        }
    }

    private String buildSummary(List<RailLabelPlanner.PlannedRailLabel> plannedRows, int footprintCount) {
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

}

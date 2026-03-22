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
    private final RailHelperOutputSupport outputSupport = new RailHelperOutputSupport();

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
        Files.writeString(summary, outputSupport.buildSummary(plannedRows, footprintByItem.size()));

        Path copiedTemplate = null;
        if (templateDocx != null) {
            copiedTemplate = outputDir.resolve(templateDocx.getFileName());
            Files.copy(templateDocx, copiedTemplate, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        for (String line : outputSupport.buildSuccessLines(trainDetailCsv, summary, copiedTemplate)) {
            System.out.println(line);
        }
        return 0;
    }

    private void ensureReadable(Path path, String optionName) {
        Objects.requireNonNull(path, optionName + " path cannot be null");
        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Invalid " + optionName + ": " + path);
        }
    }
}

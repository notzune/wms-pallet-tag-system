/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.cli.commands.rail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RailHelperCommand}.
 */
final class RailHelperCommandTest {

    @TempDir
    Path tempDir;

    /**
     * Verifies filtering by train ID and expected output artifacts.
     */
    @Test
    void commandGeneratesTrainDetailCsvForFilteredTrain() throws Exception {
        Path inputCsv = tempDir.resolve("input.csv");
        Path footprintCsv = tempDir.resolve("footprint.csv");
        Path outputDir = tempDir.resolve("out");

        Files.write(inputCsv, List.of(
                "DATE,SEQ,TRAIN_NBR,VEHICLE_ID,DCS_WHSE,LOAD_NBR,ITEM_NBR_1,TOTAL_CS_ITM_1,ITEM_NBR_2,TOTAL_CS_ITM_2",
                "03-02-26,142,TRAIN-A,\"CAR,100\",D-2,LOAD-1,01832,2300,00818,1650",
                "03-02-26,143,TRAIN-B,CAR-200,D-2,LOAD-2,01832,500,,0"
        ));
        Files.write(footprintCsv, List.of(
                "ITEM_NBR,ITEM_FAMILY,FOOTPRINT",
                "01832,DOM,100",
                "00818,CAN,75"
        ));

        int exitCode = new CommandLine(new RailHelperCommand()).execute(
                "--input-csv", inputCsv.toString(),
                "--item-footprint-csv", footprintCsv.toString(),
                "--output-dir", outputDir.toString(),
                "--train-id", "TRAIN-A"
        );

        assertEquals(0, exitCode);
        Path trainDetail = outputDir.resolve("_TrainDetail.csv");
        Path summary = outputDir.resolve("rail-helper-summary.txt");

        assertTrue(Files.exists(trainDetail));
        assertTrue(Files.exists(summary));

        List<String> csvLines = Files.readAllLines(trainDetail);
        assertEquals(2, csvLines.size(), "header + 1 filtered row expected");

        String summaryText = Files.readString(summary);
        assertTrue(summaryText.contains("Rows exported: 1"));
        assertTrue(summaryText.contains("Rows with missing footprint: 0"));
    }

    /**
     * Verifies missing footprint items are reported in the summary.
     */
    @Test
    void commandReportsMissingFootprintItemsInSummary() throws Exception {
        Path inputCsv = tempDir.resolve("input.csv");
        Path footprintCsv = tempDir.resolve("footprint.csv");
        Path outputDir = tempDir.resolve("out");

        Files.write(inputCsv, List.of(
                "DATE,SEQ,TRAIN_NBR,VEHICLE_ID,DCS_WHSE,LOAD_NBR,ITEM_NBR_1,TOTAL_CS_ITM_1",
                "03-02-26,200,TRAIN-Z,CAR-1,D-9,LOAD-9,99999,10"
        ));
        Files.write(footprintCsv, List.of(
                "ITEM_NBR,ITEM_FAMILY,FOOTPRINT",
                "01832,DOM,100"
        ));

        int exitCode = new CommandLine(new RailHelperCommand()).execute(
                "--input-csv", inputCsv.toString(),
                "--item-footprint-csv", footprintCsv.toString(),
                "--output-dir", outputDir.toString()
        );

        assertEquals(0, exitCode);
        String summaryText = Files.readString(outputDir.resolve("rail-helper-summary.txt"));
        assertTrue(summaryText.contains("Rows exported: 1"));
        assertTrue(summaryText.contains("Rows with missing footprint: 1"));
        assertTrue(summaryText.contains("Missing items: 99999"));
    }
}

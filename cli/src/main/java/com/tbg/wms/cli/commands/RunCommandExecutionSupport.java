/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.label.LabelSelectionSupport;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.RuntimePathResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared execution helpers for the run command.
 *
 * <p>This helper isolates input validation, print-mode resolution, output-path preparation,
 * and routed printer lookup from the command class so CLI workflow changes stay easier to test.</p>
 */
final class RunCommandExecutionSupport {

    private static final int MAX_LABELS_PER_JOB = 10_000;

    String resolveInputId(String shipmentId, String carrierMoveId) {
        boolean hasShipment = shipmentId != null && !shipmentId.isBlank();
        boolean hasCarrier = carrierMoveId != null && !carrierMoveId.isBlank();
        if (hasShipment == hasCarrier) {
            throw new IllegalArgumentException("Specify exactly one of --shipment-id or --carrier-move-id.");
        }
        return hasCarrier ? carrierMoveId.trim() : shipmentId.trim();
    }

    boolean isCarrierMoveMode(String carrierMoveId) {
        return carrierMoveId != null && !carrierMoveId.isBlank();
    }

    boolean isPrintToFileMode(boolean dryRun, boolean printToFile) {
        return dryRun || printToFile;
    }

    String effectiveOutputDir(boolean printToFile, String outputDir) {
        return printToFile
                ? RuntimePathResolver.resolveJarSiblingDir(RunCommand.class, "out").toString()
                : outputDir;
    }

    Path prepareOutputDirectory(String outputDirectory) throws Exception {
        Path outputPath = Paths.get(outputDirectory);
        Files.createDirectories(outputPath);
        return outputPath;
    }

    List<Lpn> resolveSelectedShipmentLpns(
            LabelWorkflowService.PreparedJob prepared,
            String labelSelectionExpression
    ) {
        Objects.requireNonNull(prepared, "prepared cannot be null");
        if (labelSelectionExpression == null || labelSelectionExpression.isBlank()) {
            return prepared.getLpnsForLabels();
        }
        List<LabelSelectionRef> availableSelections = LabelSelectionSupport.buildShipmentSelections(
                prepared.getShipmentId(),
                prepared.getLpnsForLabels()
        );
        List<LabelSelectionRef> selectedRefs = LabelSelectionSupport.selectByExpression(
                availableSelections,
                labelSelectionExpression
        );
        return LabelSelectionSupport.selectLpnsByRefs(prepared.getLpnsForLabels(), selectedRefs);
    }

    String resolvePrinterId(
            boolean printToFileMode,
            String printerOverride,
            PrinterRoutingService routing,
            String stagingLocation
    ) {
        if (printToFileMode) {
            return null;
        }

        String override = printerOverride == null ? "" : printerOverride.trim();
        if (!override.isEmpty()) {
            return override;
        }

        PrinterConfig routed = routing.selectPrinter(Map.of(
                "stagingLocation",
                stagingLocation == null || stagingLocation.isBlank() ? "UNKNOWN" : stagingLocation
        ));
        if (!routed.isEnabled()) {
            throw new IllegalArgumentException("Could not resolve an enabled printer from routing.");
        }
        return routed.getId();
    }

    void enforceMaxLabels(int labels) {
        if (labels > MAX_LABELS_PER_JOB) {
            throw new IllegalArgumentException("Label count exceeds safe upper bound: " + MAX_LABELS_PER_JOB);
        }
    }
}

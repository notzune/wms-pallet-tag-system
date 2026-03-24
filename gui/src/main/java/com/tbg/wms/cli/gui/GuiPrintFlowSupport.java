/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Pure planning and messaging helpers for GUI print execution.
 */
final class GuiPrintFlowSupport {

    PrintPlan planPrint(
            boolean carrierMoveMode,
            LabelWorkflowService.PreparedJob preparedJob,
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
            LabelWorkflowService.PrinterOption selectedPrinter,
            boolean printToFile,
            PreviewSelectionSupport.SelectionSnapshot selection,
            boolean includeInfoTags,
            Path defaultOutputDir,
            String timestamp
    ) {
        Objects.requireNonNull(selection, "selection cannot be null");
        Objects.requireNonNull(defaultOutputDir, "defaultOutputDir cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");

        boolean previewMissing = carrierMoveMode ? preparedCarrierJob == null : preparedJob == null;
        if (previewMissing) {
            throw new IllegalArgumentException("Run Preview first.");
        }
        if (!printToFile && selectedPrinter == null) {
            throw new IllegalArgumentException("Select a printer.");
        }

        List<Lpn> selectedShipmentLpns = carrierMoveMode ? List.of() : selection.selectedShipmentLpns();
        List<LabelSelectionRef> selectedCarrierLabels = carrierMoveMode ? selection.selectedCarrierLabels() : List.of();
        if ((!carrierMoveMode && selectedShipmentLpns.isEmpty())
                || (carrierMoveMode && selectedCarrierLabels.isEmpty())) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }

        String printerId = printToFile ? null : selectedPrinter.getId();
        Path outputDir = carrierMoveMode
                ? defaultOutputDir.resolve("gui-cmid-" + preparedCarrierJob.getCarrierMoveId() + "-" + timestamp)
                : defaultOutputDir.resolve("gui-" + preparedJob.getShipmentId() + "-" + timestamp);
        int plannedLabels = carrierMoveMode ? selectedCarrierLabels.size() : selectedShipmentLpns.size();
        return new PrintPlan(
                carrierMoveMode,
                printToFile,
                printerId,
                outputDir,
                selectedShipmentLpns,
                selectedCarrierLabels,
                plannedLabels,
                selection.infoTagCount(),
                includeInfoTags
        );
    }

    String buildConfirmationMessage(PrintPlan plan, LabelWorkflowService.PrinterOption selectedPrinter) {
        Objects.requireNonNull(plan, "plan cannot be null");
        Objects.requireNonNull(selectedPrinter, "selectedPrinter cannot be null");
        return "Print " + plan.plannedLabels() + " labels + " + plan.plannedInfoTags()
                + " info tags to " + selectedPrinter + "?";
    }

    String buildCompletionStatus(AdvancedPrintWorkflowService.PrintResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        if (result.isPrintToFile()) {
            return "Saved " + result.getLabelsPrinted() + " labels and " + result.getInfoTagsPrinted()
                    + " info tags to " + result.getOutputDirectory();
        }
        return "Printed " + result.getLabelsPrinted() + " labels and " + result.getInfoTagsPrinted()
                + " info tags to " + result.getPrinterId() + " (" + result.getPrinterEndpoint() + ")";
    }

    String buildCompletionDialogMessage(AdvancedPrintWorkflowService.PrintResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        return (result.isPrintToFile() ? "Saved " : "Printed ")
                + result.getLabelsPrinted() + " labels and " + result.getInfoTagsPrinted()
                + " info tags.\nOutput: " + result.getOutputDirectory();
    }

    record PrintPlan(
            boolean carrierMoveMode,
            boolean printToFile,
            String printerId,
            Path outputDir,
            List<Lpn> selectedShipmentLpns,
            List<LabelSelectionRef> selectedCarrierLabels,
            int plannedLabels,
            int plannedInfoTags,
            boolean includeInfoTags
    ) {
        PrintPlan {
            selectedShipmentLpns = List.copyOf(selectedShipmentLpns);
            selectedCarrierLabels = List.copyOf(selectedCarrierLabels);
        }
    }
}

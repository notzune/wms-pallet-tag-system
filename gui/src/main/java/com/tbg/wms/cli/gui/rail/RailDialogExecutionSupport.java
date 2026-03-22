/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.GuiExceptionMessageSupport;
import com.tbg.wms.cli.gui.GuiPrinterTargetSupport;
import com.tbg.wms.cli.gui.LabelWorkflowService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Execution and messaging helpers for the rail dialog shell.
 */
final class RailDialogExecutionSupport {

    PreviewRequest preparePreviewRequest(String trainId) {
        String normalized = trainId == null ? "" : trainId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Train ID is required.");
        }
        return new PreviewRequest(normalized);
    }

    GenerationRequest prepareGenerationRequest(
            RailWorkflowService.PreparedRailJob preparedJob,
            String outputDirText,
            LabelWorkflowService.PrinterOption selectedPrinter,
            boolean forcePrint,
            boolean printNowSelected
    ) {
        Objects.requireNonNull(preparedJob, "preparedJob cannot be null");
        boolean printToFile = GuiPrinterTargetSupport.isPrintToFile(selectedPrinter);
        boolean shouldPrint = !printToFile && (forcePrint || printNowSelected);
        Path outputDir = outputDirText == null || outputDirText.trim().isEmpty()
                ? null
                : Paths.get(outputDirText.trim());
        String printerId = shouldPrint && selectedPrinter != null
                ? selectedPrinter.getId()
                : GuiPrinterTargetSupport.FILE_PRINTER_ID;
        return new GenerationRequest(outputDir, printerId, shouldPrint);
    }

    String previewReadyMessage() {
        return "Preview ready.";
    }

    String previewFailedMessage() {
        return "Preview failed.";
    }

    String generationBusyMessage(boolean shouldPrint) {
        return shouldPrint ? "Generating PDF and printing..." : "Generating PDF...";
    }

    String generationFailedMessage() {
        return "Generation failed.";
    }

    String rootMessage(Throwable throwable) {
        return GuiExceptionMessageSupport.rootMessage(throwable);
    }

    record PreviewRequest(String trainId) {
    }

    record GenerationRequest(
            Path outputDirectory,
            String printerId,
            boolean shouldPrint
    ) {
    }
}

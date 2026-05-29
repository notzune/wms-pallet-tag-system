/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.GuiExceptionMessageSupport;
import com.tbg.wms.cli.gui.GuiPrinterTargetSupport;
import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.core.rail.RailCarCard;
import com.tbg.wms.core.rail.RailTrainInputParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Execution and messaging helpers for the rail dialog shell.
 */
final class RailDialogExecutionSupport {
    private final RailTrainInputParser trainInputParser = new RailTrainInputParser();

    PreviewRequest preparePreviewRequest(String trainId) {
        return new PreviewRequest(trainInputParser.parse(trainId));
    }

    GenerationRequest prepareGenerationRequest(
            RailWorkflowService.PreparedRailJob preparedJob,
            List<RailCarCard> selectedCards,
            String outputDirText,
            LabelWorkflowService.PrinterOption selectedPrinter,
            boolean forcePrint,
            boolean printNowSelected
    ) {
        Objects.requireNonNull(preparedJob, "preparedJob cannot be null");
        if (selectedCards == null || selectedCards.isEmpty()) {
            throw new IllegalArgumentException("Select at least one rail row to generate.");
        }
        boolean printToFile = GuiPrinterTargetSupport.isPrintToFile(selectedPrinter);
        boolean shouldPrint = !printToFile && (forcePrint || printNowSelected);
        Path outputDir = outputDirText == null || outputDirText.trim().isEmpty()
                ? null
                : Paths.get(outputDirText.trim());
        String printerId = shouldPrint && selectedPrinter != null
                ? selectedPrinter.getId()
                : GuiPrinterTargetSupport.FILE_PRINTER_ID;
        return new GenerationRequest(List.copyOf(selectedCards), outputDir, printerId, shouldPrint);
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

    record PreviewRequest(List<String> trainIds) {
    }

    record GenerationRequest(
            List<RailCarCard> selectedCards,
            Path outputDirectory,
            String printerId,
            boolean shouldPrint
    ) {
    }
}

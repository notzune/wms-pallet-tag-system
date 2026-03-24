/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.GuiPrinterTargetSupport;
import com.tbg.wms.cli.gui.LabelWorkflowService;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Pure helper methods for rail dialog print-target policy and status text.
 */
final class RailDialogSupport {
    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrinterModel(
            List<LabelWorkflowService.PrinterOption> printers,
            Path defaultOutputDir
    ) {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
        model.addElement(GuiPrinterTargetSupport.buildSystemDefaultPrinterOption());
        for (LabelWorkflowService.PrinterOption option : printers) {
            model.addElement(option);
        }
        model.addElement(GuiPrinterTargetSupport.buildPrintToFileOption(defaultOutputDir));
        return model;
    }

    PrintTargetState syncPrintTargetState(
            LabelWorkflowService.PrinterOption selectedPrinter,
            boolean printNowSelected
    ) {
        if (GuiPrinterTargetSupport.isPrintToFile(selectedPrinter)) {
            return new PrintTargetState(false, false, "Save PDF");
        }
        return new PrintTargetState(printNowSelected, true, "Print");
    }

    Path resolveDefaultOutputDir(String outputDirText) {
        if (outputDirText == null || outputDirText.trim().isEmpty()) {
            return Paths.get("out", "rail-gui").toAbsolutePath();
        }
        return Paths.get(outputDirText.trim()).toAbsolutePath();
    }

    String buildGenerationMessage(RailWorkflowService.GenerationResult result) {
        return "PDF generated:\n" + result.getPdfPath()
                + "\n\nOutput directory:\n" + result.getOutputDirectory()
                + (result.isPrinted() ? "\n\nPrint command sent to " + result.getPrinterId() + "." : "");
    }

    String buildReadyMessage(RailWorkflowService.GenerationResult result) {
        return result.isPrinted() ? "PDF generated and print command sent." : "PDF generated.";
    }

    record PrintTargetState(boolean printNowSelected, boolean printNowEnabled, String printButtonText) {
    }
}

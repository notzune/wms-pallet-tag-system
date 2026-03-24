package com.tbg.wms.cli.gui;

import com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology;
import com.tbg.wms.core.print.PrinterConfig;

import javax.swing.*;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Executes barcode dialog actions so the factory can stay focused on UI construction.
 */
final class BarcodeDialogActionSupport {
    private final BarcodeDialogFactory.Dependencies dependencies;
    private final BarcodeDialogExecutionSupport executionSupport;
    private final BarcodeDialogFormSupport formSupport;
    private final GuiZplPreviewSupport zplPreviewSupport = new GuiZplPreviewSupport();

    BarcodeDialogActionSupport(
            BarcodeDialogFactory.Dependencies dependencies,
            BarcodeDialogExecutionSupport executionSupport,
            BarcodeDialogFormSupport formSupport
    ) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null");
        this.executionSupport = Objects.requireNonNull(executionSupport, "executionSupport cannot be null");
        this.formSupport = Objects.requireNonNull(formSupport, "formSupport cannot be null");
    }

    void generateBarcode(JDialog dialog,
                         JTextField dataField,
                         JComboBox<Symbology> typeCombo,
                         JComboBox<Orientation> orientationCombo,
                         JSpinner labelWidth,
                         JSpinner labelHeight,
                         JSpinner originX,
                         JSpinner originY,
                         JSpinner moduleWidth,
                         JSpinner moduleRatio,
                         JSpinner barcodeHeight,
                         JCheckBox humanReadable,
                         JSpinner copies,
                         JComboBox<LabelWorkflowService.PrinterOption> printerSelect,
                         JTextField outputDir) {
        String data;
        try {
            data = executionSupport.requireBarcodeData(dataField.getText());
        } catch (IllegalArgumentException ex) {
            dependencies.showError(ex.getMessage());
            return;
        }

        BarcodeRequest request = formSupport.buildRequest(
                data,
                typeCombo,
                orientationCombo,
                labelWidth,
                labelHeight,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                humanReadable,
                copies
        );

        String zpl = executionSupport.buildZpl(request);
        LabelWorkflowService.PrinterOption printer = (LabelWorkflowService.PrinterOption) printerSelect.getSelectedItem();
        boolean printToFile = dependencies.isPrintToFileSelected(printer);
        Path outputPath = executionSupport.resolveOutputPath(outputDir.getText(), data, printToFile);
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, zpl);
        } catch (Exception ex) {
            dependencies.showError(executionSupport.buildWriteFailureMessage(ex));
            return;
        }

        if (printToFile) {
            JOptionPane.showMessageDialog(
                    dialog,
                    executionSupport.buildGeneratedMessage(outputPath),
                    "Barcode Generated",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (printer == null) {
            dependencies.showError("Select a print target.");
            return;
        }

        try {
            PrinterConfig printerConfig = dependencies.resolvePrinter(printer.getId());
            if (printerConfig == null) {
                throw new IllegalArgumentException("Printer not found: " + printer.getId());
            }
            dependencies.printBarcode(printerConfig, zpl);
        } catch (Exception ex) {
            dependencies.showError(executionSupport.buildPrintFailureMessage(ex));
            return;
        }

        JOptionPane.showMessageDialog(
                dialog,
                executionSupport.buildPrintedMessage(outputPath),
                "Barcode Printed",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    List<GuiZplPreviewSupport.PreviewDocument> buildPreviewDocuments(
            JTextField dataField,
            JComboBox<Symbology> typeCombo,
            JComboBox<Orientation> orientationCombo,
            JSpinner labelWidth,
            JSpinner labelHeight,
            JSpinner originX,
            JSpinner originY,
            JSpinner moduleWidth,
            JSpinner moduleRatio,
            JSpinner barcodeHeight,
            JCheckBox humanReadable,
            JSpinner copies
    ) {
        String data = executionSupport.requireBarcodeData(dataField.getText());

        BarcodeRequest request = formSupport.buildRequest(
                data,
                typeCombo,
                orientationCombo,
                labelWidth,
                labelHeight,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                humanReadable,
                copies
        );
        return zplPreviewSupport.buildBarcodeDocuments(request);
    }

    void previewBarcode(
            JFrame owner,
            JTextField dataField,
            JComboBox<Symbology> typeCombo,
            JComboBox<Orientation> orientationCombo,
            JSpinner labelWidth,
            JSpinner labelHeight,
            JSpinner originX,
            JSpinner originY,
            JSpinner moduleWidth,
            JSpinner moduleRatio,
            JSpinner barcodeHeight,
            JCheckBox humanReadable,
            JSpinner copies
    ) {
        try {
            ZplPreviewToolDialog.openWithDocuments(
                    owner,
                    "Barcode Preview",
                    buildPreviewDocuments(
                            dataField,
                            typeCombo,
                            orientationCombo,
                            labelWidth,
                            labelHeight,
                            originX,
                            originY,
                            moduleWidth,
                            moduleRatio,
                            barcodeHeight,
                            humanReadable,
                            copies
                    )
            );
        } catch (IllegalArgumentException ex) {
            dependencies.showError(ex.getMessage());
        }
    }
}

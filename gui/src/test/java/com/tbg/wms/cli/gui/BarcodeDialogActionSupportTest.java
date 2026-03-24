package com.tbg.wms.cli.gui;

import com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology;
import com.tbg.wms.core.print.PrinterConfig;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BarcodeDialogActionSupportTest {

    @Test
    void generateBarcode_shouldRejectBlankDataBeforeAnyWrite() {
        TestDependencies dependencies = new TestDependencies();
        BarcodeDialogActionSupport support = new BarcodeDialogActionSupport(
                dependencies,
                new BarcodeDialogExecutionSupport(Path.of("out"), DateTimeFormatter.ofPattern("'TS'"), 40),
                new BarcodeDialogFormSupport()
        );

        support.generateBarcode(
                null,
                new JTextField(" "),
                new JComboBox<>(Symbology.values()),
                new JComboBox<>(Orientation.values()),
                spinner(812, 1, 10000),
                spinner(1218, 1, 20000),
                spinner(60, 0, 10000),
                spinner(60, 0, 10000),
                spinner(3, 1, 20),
                spinner(3, 1, 10),
                spinner(220, 1, 2000),
                new JCheckBox("Human readable", true),
                spinner(1, 1, 100),
                printerCombo(new LabelWorkflowService.PrinterOption("__FILE__", "Print to file", "file")),
                new JTextField("out")
        );

        assertEquals("Barcode data is required.", dependencies.lastError);
        assertNull(dependencies.lastPrintedZpl);
    }

    @Test
    void generateBarcode_shouldFailWhenPrinterCannotBeResolved() {
        TestDependencies dependencies = new TestDependencies();
        dependencies.printToFile = false;
        BarcodeDialogActionSupport support = new BarcodeDialogActionSupport(
                dependencies,
                new BarcodeDialogExecutionSupport(Path.of("out"), DateTimeFormatter.ofPattern("'TS'"), 40),
                new BarcodeDialogFormSupport()
        );

        support.generateBarcode(
                null,
                new JTextField("ABC123"),
                new JComboBox<>(Symbology.values()),
                new JComboBox<>(Orientation.values()),
                spinner(812, 1, 10000),
                spinner(1218, 1, 20000),
                spinner(60, 0, 10000),
                spinner(60, 0, 10000),
                spinner(3, 1, 20),
                spinner(3, 1, 10),
                spinner(220, 1, 2000),
                new JCheckBox("Human readable", true),
                spinner(1, 1, 100),
                printerCombo(new LabelWorkflowService.PrinterOption("OFFICE", "Office", "10.0.0.1:9100")),
                new JTextField("out")
        );

        assertEquals("Failed to print barcode: Printer not found: OFFICE", dependencies.lastError);
        assertNull(dependencies.lastPrintedZpl);
    }

    private static JSpinner spinner(int value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }

    private static JComboBox<LabelWorkflowService.PrinterOption> printerCombo(LabelWorkflowService.PrinterOption option) {
        JComboBox<LabelWorkflowService.PrinterOption> combo = new JComboBox<>();
        combo.addItem(option);
        combo.setSelectedItem(option);
        return combo;
    }

    private static final class TestDependencies implements BarcodeDialogFactory.Dependencies {
        private boolean printToFile = true;
        private String lastError;
        private String lastPrintedZpl;

        @Override
        public DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(boolean includeFileOption) {
            return new DefaultComboBoxModel<>();
        }

        @Override
        public boolean isPrintToFileSelected(LabelWorkflowService.PrinterOption selected) {
            return printToFile;
        }

        @Override
        public Path defaultPrintToFileOutputDir() {
            return Path.of("out");
        }

        @Override
        public void installClipboardBehavior(javax.swing.text.JTextComponent... fields) {
        }

        @Override
        public void showError(String message) {
            lastError = message;
        }

        @Override
        public String rootMessage(Throwable throwable) {
            return throwable == null ? "" : throwable.getMessage();
        }

        @Override
        public PrinterConfig resolvePrinter(String printerId) {
            return null;
        }

        @Override
        public void printBarcode(PrinterConfig printerConfig, String zpl) {
            lastPrintedZpl = zpl;
        }
    }
}

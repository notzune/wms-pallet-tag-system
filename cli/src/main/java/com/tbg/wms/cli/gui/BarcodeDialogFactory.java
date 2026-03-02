/*
 * Copyright (c) 2026 Tropicana Brands Group
 */

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.barcode.BarcodeZplBuilder;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Factory that creates and displays the barcode generator dialog.
 */
final class BarcodeDialogFactory {

    interface Dependencies {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(boolean includeFileOption);
        boolean isPrintToFileSelected(LabelWorkflowService.PrinterOption selected);
        Path defaultPrintToFileOutputDir();
        void installClipboardBehavior(JTextComponent... fields);
        void showError(String message);
        String rootMessage(Throwable throwable);
        PrinterConfig resolvePrinter(String printerId) throws Exception;
    }

    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final DateTimeFormatter OUTPUT_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final int BARCODE_DEFAULT_LABEL_WIDTH_DOTS = 812;
    private static final int BARCODE_DEFAULT_LABEL_HEIGHT_DOTS = 1218;
    private static final int BARCODE_DEFAULT_ORIGIN_X = 60;
    private static final int BARCODE_DEFAULT_ORIGIN_Y = 60;
    private static final int BARCODE_DEFAULT_MODULE_WIDTH = 3;
    private static final int BARCODE_DEFAULT_MODULE_RATIO = 3;
    private static final int BARCODE_DEFAULT_HEIGHT = 220;

    private final Dependencies dependencies;

    BarcodeDialogFactory(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null");
    }

    void open(JFrame owner) {
        Objects.requireNonNull(owner, "owner cannot be null");
        JDialog dialog = new JDialog(owner, "Barcode Generator", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField dataField = new JTextField(24);
        JComboBox<Symbology> typeCombo = new JComboBox<>(Symbology.values());
        JComboBox<Orientation> orientationCombo = new JComboBox<>(Orientation.values());
        JSpinner labelWidth = new JSpinner(new SpinnerNumberModel(BARCODE_DEFAULT_LABEL_WIDTH_DOTS, 1, 10000, 1));
        JSpinner labelHeight = new JSpinner(new SpinnerNumberModel(BARCODE_DEFAULT_LABEL_HEIGHT_DOTS, 1, 20000, 1));
        JSpinner originX = new JSpinner(new SpinnerNumberModel(BARCODE_DEFAULT_ORIGIN_X, 0, 10000, 1));
        JSpinner originY = new JSpinner(new SpinnerNumberModel(BARCODE_DEFAULT_ORIGIN_Y, 0, 10000, 1));
        JSpinner moduleWidth = new JSpinner(new SpinnerNumberModel(BARCODE_DEFAULT_MODULE_WIDTH, 1, 20, 1));
        JSpinner moduleRatio = new JSpinner(new SpinnerNumberModel(BARCODE_DEFAULT_MODULE_RATIO, 1, 10, 1));
        JSpinner barcodeHeight = new JSpinner(new SpinnerNumberModel(BARCODE_DEFAULT_HEIGHT, 1, 2000, 1));
        JCheckBox humanReadable = new JCheckBox("Human readable", true);
        JSpinner copies = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JTextField outputDir = new JTextField(dependencies.defaultPrintToFileOutputDir().toString());
        outputDir.setColumns(40);
        JTextField scannerProfile = new JTextField("Honeywell Granit 1980i / THOR VM1A optimized preset");
        scannerProfile.setEditable(false);
        scannerProfile.setFocusable(false);
        dependencies.installClipboardBehavior(dataField, outputDir);

        JComboBox<LabelWorkflowService.PrinterOption> printerSelect = new JComboBox<>();
        printerSelect.setModel(dependencies.buildPrintTargetModel(true));

        int row = 0;
        addFormRow(form, gbc, row++, "Data", dataField);
        addFormRow(form, gbc, row++, "Type", typeCombo);
        addFormRow(form, gbc, row++, "Copies", copies);
        addFormRow(form, gbc, row++, "Scanner Profile", scannerProfile);
        addFormRow(form, gbc, row++, "Printer", printerSelect);

        Runnable syncOutputState = () -> {
            LabelWorkflowService.PrinterOption selected =
                    (LabelWorkflowService.PrinterOption) printerSelect.getSelectedItem();
            boolean printToFile = dependencies.isPrintToFileSelected(selected);
            outputDir.setEnabled(printToFile);
            outputDir.setEditable(printToFile);
        };
        printerSelect.addActionListener(e -> syncOutputState.run());
        syncOutputState.run();

        JButton advancedButton = new JButton("Advanced Settings...");
        JButton generateButton = new JButton("Generate");
        JButton closeButton = new JButton("Close");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(advancedButton);
        buttons.add(generateButton);
        buttons.add(closeButton);

        advancedButton.addActionListener(e -> openAdvancedSettingsDialog(
                dialog,
                printerSelect,
                orientationCombo,
                labelWidth,
                labelHeight,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                humanReadable,
                outputDir
        ));

        generateButton.addActionListener(e -> generateBarcode(
                dialog,
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
                copies,
                printerSelect,
                outputDir
        ));

        closeButton.addActionListener(e -> dialog.dispose());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void openAdvancedSettingsDialog(JDialog parent,
                                            JComboBox<LabelWorkflowService.PrinterOption> printerSelect,
                                            JComboBox<Orientation> orientationCombo,
                                            JSpinner labelWidth,
                                            JSpinner labelHeight,
                                            JSpinner originX,
                                            JSpinner originY,
                                            JSpinner moduleWidth,
                                            JSpinner moduleRatio,
                                            JSpinner barcodeHeight,
                                            JCheckBox humanReadable,
                                            JTextField outputDir) {
        JDialog advancedDialog = new JDialog(parent, "Advanced Barcode Settings", Dialog.ModalityType.APPLICATION_MODAL);
        advancedDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        advancedDialog.setLayout(new BorderLayout(8, 8));

        JPanel advancedForm = new JPanel(new GridBagLayout());
        GridBagConstraints advancedGbc = new GridBagConstraints();
        advancedGbc.insets = new Insets(6, 6, 6, 6);
        advancedGbc.anchor = GridBagConstraints.WEST;
        advancedGbc.fill = GridBagConstraints.HORIZONTAL;

        int advancedRow = 0;
        addFormRow(advancedForm, advancedGbc, advancedRow++, "Orientation", orientationCombo);
        addFormRow(advancedForm, advancedGbc, advancedRow++, "Label Width (dots)", labelWidth);
        addFormRow(advancedForm, advancedGbc, advancedRow++, "Label Height (dots)", labelHeight);
        addFormRow(advancedForm, advancedGbc, advancedRow++, "Origin X", originX);
        addFormRow(advancedForm, advancedGbc, advancedRow++, "Origin Y", originY);
        addFormRow(advancedForm, advancedGbc, advancedRow++, "Module Width", moduleWidth);
        addFormRow(advancedForm, advancedGbc, advancedRow++, "Module Ratio", moduleRatio);
        addFormRow(advancedForm, advancedGbc, advancedRow++, "Barcode Height", barcodeHeight);
        JLabel outputDirLabel = addFormRow(advancedForm, advancedGbc, advancedRow++, "Output Dir", outputDir);

        advancedGbc.gridx = 1;
        advancedGbc.gridy = advancedRow;
        advancedGbc.gridwidth = 2;
        advancedForm.add(humanReadable, advancedGbc);

        Runnable syncAdvancedOutputState = () -> {
            LabelWorkflowService.PrinterOption selected = (LabelWorkflowService.PrinterOption) printerSelect.getSelectedItem();
            boolean printToFile = dependencies.isPrintToFileSelected(selected);
            outputDir.setEnabled(printToFile);
            outputDir.setEditable(printToFile);
            outputDirLabel.setEnabled(printToFile);
        };
        syncAdvancedOutputState.run();

        JPanel advancedButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeAdvanced = new JButton("Done");
        closeAdvanced.addActionListener(x -> advancedDialog.dispose());
        advancedButtons.add(closeAdvanced);

        advancedDialog.add(advancedForm, BorderLayout.CENTER);
        advancedDialog.add(advancedButtons, BorderLayout.SOUTH);
        advancedDialog.pack();
        advancedDialog.setLocationRelativeTo(parent);
        advancedDialog.setVisible(true);
    }

    private void generateBarcode(JDialog dialog,
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
        String data = dataField.getText().trim();
        if (data.isEmpty()) {
            dependencies.showError("Barcode data is required.");
            return;
        }

        BarcodeRequest request = new BarcodeRequest(
                data,
                (Symbology) typeCombo.getSelectedItem(),
                (Orientation) orientationCombo.getSelectedItem(),
                (int) labelWidth.getValue(),
                (int) labelHeight.getValue(),
                (int) originX.getValue(),
                (int) originY.getValue(),
                (int) moduleWidth.getValue(),
                (int) moduleRatio.getValue(),
                (int) barcodeHeight.getValue(),
                humanReadable.isSelected(),
                (int) copies.getValue()
        );

        String zpl = BarcodeZplBuilder.build(request);
        LabelWorkflowService.PrinterOption printer = (LabelWorkflowService.PrinterOption) printerSelect.getSelectedItem();
        boolean printToFile = dependencies.isPrintToFileSelected(printer);
        Path outputPath = resolveOutputPath(outputDir.getText(), data, printToFile);
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, zpl);
        } catch (Exception ex) {
            dependencies.showError("Failed to write ZPL file: " + ex.getMessage());
            return;
        }

        if (printToFile) {
            JOptionPane.showMessageDialog(
                    dialog,
                    "ZPL saved to " + outputPath,
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
            new NetworkPrintService().print(printerConfig, zpl, "barcode");
        } catch (Exception ex) {
            dependencies.showError("Failed to print barcode: " + dependencies.rootMessage(ex));
            return;
        }

        JOptionPane.showMessageDialog(
                dialog,
                "Printed barcode label.\nZPL saved to " + outputPath,
                "Barcode Printed",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private Path resolveOutputPath(String outputDir, String data, boolean printToFileSelected) {
        String dir;
        if (printToFileSelected) {
            dir = (outputDir == null || outputDir.isBlank())
                    ? dependencies.defaultPrintToFileOutputDir().toString()
                    : outputDir.trim();
        } else {
            dir = dependencies.defaultPrintToFileOutputDir().toString();
        }
        Path outputPath = Paths.get(dir);
        String fileName = String.format("barcode-%s-%s.zpl",
                OUTPUT_TS.format(LocalDateTime.now()),
                safeSlug(data));
        return outputPath.resolve(fileName);
    }

    private static JLabel addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        JLabel rowLabel = new JLabel(label + ":");
        form.add(rowLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(field, gbc);
        return rowLabel;
    }

    private static String safeSlug(String value) {
        if (value == null) {
            return "data";
        }
        String slug = NON_ALNUM_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        if (slug.isEmpty()) {
            return "data";
        }
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }
}

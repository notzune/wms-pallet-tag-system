/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.barcode.BarcodeZplBuilder;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Swing-based GUI for shipment preview and label printing.
 */
public final class LabelGuiFrame extends JFrame {

    private final JTextField shipmentField = new JTextField(16);
    private final JComboBox<LabelWorkflowService.PrinterOption> printerCombo = new JComboBox<>();
    private final JButton previewButton = new JButton("Preview");
    private final JButton clearButton = new JButton("Clear");
    private final JButton printButton = new JButton("Confirm Print");
    private final JTextArea shipmentArea = new JTextArea();
    private final JTextArea mathArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready.");

    private static final String FILE_PRINTER_ID = "FILE";

    private final AppConfig config = new AppConfig();
    private final LabelWorkflowService service = new LabelWorkflowService(config);
    private LabelWorkflowService.PreparedJob preparedJob;

    public LabelGuiFrame() {
        super(buildWindowTitle());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1080, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(buildToolBar(), BorderLayout.NORTH);
        topContainer.add(buildTopPanel(), BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        wireActions();
        wireShortcuts();
        loadPrintersAsync();
        printButton.setEnabled(false);
    }

    private JComponent buildToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton toolsButton = new JButton("Tools");
        toolsButton.setFocusable(false);
        toolBar.add(toolsButton);

        JPopupMenu toolsMenu = new JPopupMenu();
        JMenuItem barcodeItem = new JMenuItem("Barcode Generator...");
        barcodeItem.addActionListener(e -> openBarcodeDialog());
        toolsMenu.add(barcodeItem);

        toolsButton.addActionListener(e ->
                toolsMenu.show(toolsButton, 0, toolsButton.getHeight()));

        return toolBar;
    }

    private JComponent buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Shipment ID:"), gbc);

        gbc.gridx = 1;
        panel.add(shipmentField, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Printer:"), gbc);

        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(printerCombo, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(previewButton, gbc);

        gbc.gridx = 5;
        panel.add(clearButton, gbc);

        gbc.gridx = 6;
        panel.add(printButton, gbc);

        return panel;
    }

    private JComponent buildCenterPanel() {
        shipmentArea.setEditable(false);
        shipmentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        mathArea.setEditable(false);
        mathArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(shipmentArea),
                new JScrollPane(mathArea));
        split.setDividerLocation(340);
        return split;
    }

    private JComponent buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void wireActions() {
        previewButton.addActionListener(e -> previewJob());
        clearButton.addActionListener(e -> clearForm());
        printButton.addActionListener(e -> confirmAndPrint());
    }

    private void wireShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "preview");
        actionMap.put("preview", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                previewJob();
            }
        });
    }

    private void loadPrintersAsync() {
        setBusy("Loading printers...");
        SwingWorker<List<LabelWorkflowService.PrinterOption>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LabelWorkflowService.PrinterOption> doInBackground() throws Exception {
                return service.loadPrinters();
            }

            @Override
            protected void done() {
                try {
                    List<LabelWorkflowService.PrinterOption> printers = get();
                    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
                    for (LabelWorkflowService.PrinterOption printer : printers) {
                        model.addElement(printer);
                    }
                    model.addElement(new LabelWorkflowService.PrinterOption(
                            FILE_PRINTER_ID,
                            "Print to file",
                            resolveJarOutputDir().toString()
                    ));
                    printerCombo.setModel(model);
                    if (model.getSize() > 0) {
                        printerCombo.setSelectedIndex(0);
                        setReady("Printers loaded.");
                    } else {
                        setReady("No enabled printers found in routing config.");
                    }
                } catch (Exception ex) {
                    setReady("Failed to load printers.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void previewJob() {
        String shipmentId = shipmentField.getText().trim();
        if (shipmentId.isEmpty()) {
            showError("Enter a shipment ID.");
            return;
        }

        setBusy("Preparing preview...");
        printButton.setEnabled(false);
        preparedJob = null;

        SwingWorker<LabelWorkflowService.PreparedJob, Void> worker = new SwingWorker<>() {
            @Override
            protected LabelWorkflowService.PreparedJob doInBackground() throws Exception {
                return service.prepareJob(shipmentId);
            }

            @Override
            protected void done() {
                try {
                    preparedJob = get();
                    renderPreview(preparedJob);
                    printButton.setEnabled(true);
                    setReady("Preview ready. Verify details, then click Confirm Print.");
                } catch (Exception ex) {
                    setReady("Preview failed.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void renderPreview(LabelWorkflowService.PreparedJob job) {
        StringBuilder summary = new StringBuilder();
        summary.append("Shipment: ").append(job.getShipment().getShipmentId()).append('\n');
        summary.append("Order: ").append(value(job.getShipment().getOrderId())).append('\n');
        summary.append("Ship To: ").append(value(job.getShipment().getShipToName())).append('\n');
        summary.append("Address: ").append(value(job.getShipment().getShipToAddress1())).append(", ")
                .append(value(job.getShipment().getShipToCity())).append(", ")
                .append(value(job.getShipment().getShipToState())).append(" ")
                .append(value(job.getShipment().getShipToZip())).append('\n');
        summary.append("PO: ").append(value(job.getShipment().getCustomerPo())).append('\n');
        summary.append("Location No: ").append(value(job.getShipment().getLocationNumber())).append('\n');
        summary.append("Carrier Move: ").append(value(job.getShipment().getCarrierCode())).append(" ")
                .append(value(job.getShipment().getCarrierMoveId())).append('\n');
        summary.append("Staging Location: ").append(value(job.getStagingLocation())).append('\n');
        summary.append('\n');
        summary.append("Label Plan:\n");
        summary.append(" - Actual LPNs: ").append(job.getShipment().getLpnCount()).append('\n');
        summary.append(" - Labels To Generate: ").append(job.getLpnsForLabels().size()).append('\n');
        summary.append(" - Virtual Labels Used: ").append(job.isUsingVirtualLabels() ? "YES" : "NO").append('\n');
        summary.append(" - Total Units: ").append(job.getPlanResult().getTotalUnits()).append('\n');
        summary.append(" - Estimated Pallets (Footprint): ").append(job.getPlanResult().getEstimatedPallets()).append('\n');
        summary.append(" - Full Pallets (Footprint): ").append(job.getPlanResult().getFullPallets()).append('\n');
        summary.append(" - Partial Pallets (Footprint): ").append(job.getPlanResult().getPartialPallets()).append('\n');
        summary.append(" - Missing Footprint SKUs: ")
                .append(job.getPlanResult().getSkusMissingFootprint().isEmpty()
                        ? "None"
                        : String.join(", ", job.getPlanResult().getSkusMissingFootprint()))
                .append('\n');
        shipmentArea.setText(summary.toString());

        StringBuilder math = new StringBuilder();
        math.append("Pallet Math (Full vs Partial)\n");
        math.append(String.format("%-20s %-10s %-14s %-8s %-10s %-10s %s%n",
                "SKU", "Units", "Units/Pallet", "Full", "Partial", "TotalPal", "Description"));
        math.append("----------------------------------------------------------------------------------------------------\n");
        for (LabelWorkflowService.SkuMathRow row : job.getSkuMathRows()) {
            math.append(String.format("%-20s %-10d %-14s %-8d %-10d %-10d %s%n",
                    value(row.getSku()),
                    row.getUnits(),
                    row.getUnitsPerPallet() == null ? "-" : row.getUnitsPerPallet().toString(),
                    row.getFullPallets(),
                    row.getPartialUnits(),
                    row.getEstimatedPallets(),
                    value(row.getDescription())));
        }
        mathArea.setText(math.toString());
    }

    private void confirmAndPrint() {
        LabelWorkflowService.PreparedJob job = preparedJob;
        if (job == null) {
            showError("Run Preview first.");
            return;
        }

        LabelWorkflowService.PrinterOption selected = (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem();
        boolean printToFile = isPrintToFileSelected(selected);
        if (!printToFile && selected == null) {
            showError("Select a printer.");
            return;
        }

        if (!printToFile) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Print " + job.getLpnsForLabels().size() + " labels to " + selected + "?",
                    "Confirm Print",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        setBusy("Printing...");
        printButton.setEnabled(false);
        previewButton.setEnabled(false);
        clearButton.setEnabled(false);

        SwingWorker<LabelWorkflowService.PrintResult, Void> worker = new SwingWorker<>() {
            @Override
            protected LabelWorkflowService.PrintResult doInBackground() throws Exception {
                Path outDir = Paths.get("out", "gui-" + job.getShipmentId() + "-" +
                        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()));
                String printerId = printToFile ? null : (selected == null ? null : selected.getId());
                return service.print(job, printerId, outDir, printToFile);
            }

            @Override
            protected void done() {
                previewButton.setEnabled(true);
                printButton.setEnabled(true);
                clearButton.setEnabled(true);
                try {
                    LabelWorkflowService.PrintResult result = get();
                    if (result.isPrintToFile()) {
                        setReady("Saved " + result.getLabelsPrinted() + " labels to " + result.getOutputDirectory());
                        JOptionPane.showMessageDialog(
                                LabelGuiFrame.this,
                                "Saved " + result.getLabelsPrinted() + " labels.\nOutput: " + result.getOutputDirectory(),
                                "Print Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        setReady("Printed " + result.getLabelsPrinted() + " labels to " + result.getPrinterId() +
                                " (" + result.getPrinterEndpoint() + ")");
                        JOptionPane.showMessageDialog(
                                LabelGuiFrame.this,
                                "Printed " + result.getLabelsPrinted() + " labels.\nOutput: " + result.getOutputDirectory(),
                                "Print Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } catch (Exception ex) {
                    setReady("Print failed.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void setBusy(String message) {
        statusLabel.setText(message);
        previewButton.setEnabled(false);
        clearButton.setEnabled(false);
        printButton.setEnabled(false);
    }

    private static String buildWindowTitle() {
        String version = com.tbg.wms.cli.commands.RootCommand.class.getAnnotation(
                picocli.CommandLine.Command.class
        ).version()[0];
        return "WMS Pallet Tag System - " + version;
    }

    private void setReady(String message) {
        statusLabel.setText(message);
        previewButton.setEnabled(true);
        clearButton.setEnabled(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void clearForm() {
        shipmentField.setText("");
        shipmentArea.setText("");
        mathArea.setText("");
        preparedJob = null;
        printButton.setEnabled(false);
        shipmentField.requestFocusInWindow();
        setReady("Cleared. Enter the next shipment ID.");
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = Objects.requireNonNullElse(throwable, new RuntimeException("Unknown error"));
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return (message == null || message.isBlank()) ? cursor.getClass().getSimpleName() : message;
    }

    private String value(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    private void openBarcodeDialog() {
        JDialog dialog = new JDialog(this, "Barcode Generator", Dialog.ModalityType.APPLICATION_MODAL);
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
        JSpinner labelWidth = new JSpinner(new SpinnerNumberModel(812, 1, 10000, 1));
        JSpinner labelHeight = new JSpinner(new SpinnerNumberModel(1218, 1, 20000, 1));
        JSpinner originX = new JSpinner(new SpinnerNumberModel(40, 0, 10000, 1));
        JSpinner originY = new JSpinner(new SpinnerNumberModel(40, 0, 10000, 1));
        JSpinner moduleWidth = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        JSpinner moduleRatio = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        JSpinner barcodeHeight = new JSpinner(new SpinnerNumberModel(120, 1, 2000, 1));
        JCheckBox humanReadable = new JCheckBox("Human readable", true);
        JSpinner copies = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JCheckBox printToFileCheck = new JCheckBox("Print to file", true);
        JTextField outputDir = new JTextField(defaultBarcodeOutputDir());
        JComboBox<LabelWorkflowService.PrinterOption> printerSelect = new JComboBox<>();
        printerSelect.setModel(buildPrinterModel(false));

        int row = 0;
        addFormRow(form, gbc, row++, "Data", dataField);
        addFormRow(form, gbc, row++, "Type", typeCombo);
        addFormRow(form, gbc, row++, "Orientation", orientationCombo);
        addFormRow(form, gbc, row++, "Label Width (dots)", labelWidth);
        addFormRow(form, gbc, row++, "Label Height (dots)", labelHeight);
        addFormRow(form, gbc, row++, "Origin X", originX);
        addFormRow(form, gbc, row++, "Origin Y", originY);
        addFormRow(form, gbc, row++, "Module Width", moduleWidth);
        addFormRow(form, gbc, row++, "Module Ratio", moduleRatio);
        addFormRow(form, gbc, row++, "Barcode Height", barcodeHeight);
        addFormRow(form, gbc, row++, "Copies", copies);
        addFormRow(form, gbc, row++, "Output Dir", outputDir);
        addFormRow(form, gbc, row++, "Printer", printerSelect);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        form.add(humanReadable, gbc);
        row++;
        gbc.gridy = row;
        form.add(printToFileCheck, gbc);

        JButton generateButton = new JButton("Generate");
        JButton closeButton = new JButton("Close");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(generateButton);
        buttons.add(closeButton);

        generateButton.addActionListener(e -> {
            String data = dataField.getText().trim();
            if (data.isEmpty()) {
                showError("Barcode data is required.");
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
            Path outputPath = resolveOutputPath(outputDir.getText(), data);
            try {
                Files.createDirectories(outputPath.getParent());
                Files.writeString(outputPath, zpl);
            } catch (Exception ex) {
                showError("Failed to write ZPL file: " + ex.getMessage());
                return;
            }

            if (printToFileCheck.isSelected()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "ZPL saved to " + outputPath,
                        "Barcode Generated",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            LabelWorkflowService.PrinterOption printer = (LabelWorkflowService.PrinterOption) printerSelect.getSelectedItem();
            if (printer == null) {
                showError("Select a printer or enable Print to file.");
                return;
            }

            try {
                PrinterConfig printerConfig = service.resolvePrinter(printer.getId());
                if (printerConfig == null) {
                    throw new IllegalArgumentException("Printer not found: " + printer.getId());
                }
                new NetworkPrintService().print(printerConfig, zpl, "barcode");
            } catch (Exception ex) {
                showError("Failed to print barcode: " + rootMessage(ex));
                return;
            }

            JOptionPane.showMessageDialog(
                    dialog,
                    "Printed barcode label.\nZPL saved to " + outputPath,
                    "Barcode Printed",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrinterModel(boolean includeFileOption) {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
        ComboBoxModel<LabelWorkflowService.PrinterOption> baseModel = printerCombo.getModel();
        for (int i = 0; i < baseModel.getSize(); i++) {
            LabelWorkflowService.PrinterOption option = baseModel.getElementAt(i);
            if (!includeFileOption && FILE_PRINTER_ID.equals(option.getId())) {
                continue;
            }
            model.addElement(option);
        }
        return model;
    }

    private static void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        form.add(new JLabel(label + ":"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(field, gbc);
    }

    private static Path resolveOutputPath(String outputDir, String data) {
        String dir = (outputDir == null || outputDir.isBlank()) ? defaultBarcodeOutputDir() : outputDir.trim();
        Path outputPath = Paths.get(dir);
        String fileName = String.format("barcode-%s-%s.zpl",
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()),
                safeSlug(data));
        return outputPath.resolve(fileName);
    }

    private static String defaultBarcodeOutputDir() {
        Path baseDir = resolveJarOutputDir();
        return baseDir.resolve("barcodes").toString();
    }

    private static Path resolveJarOutputDir() {
        try {
            Path codeSource = Paths.get(Objects.requireNonNull(LabelGuiFrame.class
                    .getProtectionDomain()
                    .getCodeSource())
                    .getLocation()
                    .toURI());
            Path baseDir = Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
            return baseDir.resolve("out");
        } catch (Exception e) {
            return Paths.get("out");
        }
    }

    private static boolean isPrintToFileSelected(LabelWorkflowService.PrinterOption selected) {
        return selected != null && FILE_PRINTER_ID.equals(selected.getId());
    }

    private static String safeSlug(String value) {
        if (value == null) {
            return "data";
        }
        String slug = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        if (slug.isEmpty()) {
            return "data";
        }
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }
}

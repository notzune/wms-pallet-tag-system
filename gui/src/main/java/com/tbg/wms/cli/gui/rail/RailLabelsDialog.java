/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.GuiExceptionMessageSupport;
import com.tbg.wms.cli.gui.GuiPrinterTargetSupport;
import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.cli.gui.TextFieldClipboardController;
import com.tbg.wms.cli.gui.WorkflowShortcutBinder;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.rail.RailCarCard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Rail labels screen with train load, railcar table preview, card preview, and print action.
 */
public final class RailLabelsDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;

    private final JTextField trainIdField = new JTextField(16);
    private final JTextField outputDirField = new JTextField(48);
    private final JComboBox<LabelWorkflowService.PrinterOption> printerCombo = new JComboBox<>();
    private final JCheckBox printNowCheck = new JCheckBox("Print after PDF generation", false);
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JButton loadButton = new JButton("Load Preview");
    private final JButton generatePdfButton = new JButton("Generate PDF");
    private final JButton printButton = new JButton("Print");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"SEQ", "VEHICLE", "CAN", "DOM", "KEV", "LOAD_NBR"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable previewTable = new JTable(tableModel);
    private final JTextArea cardPreviewArea = new JTextArea();
    private final JTextArea diagnosticsArea = new JTextArea();

    private final transient RailWorkflowService service;
    private final transient TextFieldClipboardController clipboardController = new TextFieldClipboardController();
    private transient RailWorkflowService.PreparedRailJob preparedJob;

    public RailLabelsDialog(JFrame owner, AppConfig config) {
        super(owner, "Rail Labels Workflow", true);
        this.service = new RailWorkflowService(Objects.requireNonNull(config, "config cannot be null"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1220, 820);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        outputDirField.setText(Paths.get("out", "rail-gui").toAbsolutePath().toString());
        cardPreviewArea.setEditable(false);
        diagnosticsArea.setEditable(false);
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        cardPreviewArea.setFont(mono);
        diagnosticsArea.setFont(mono);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        clipboardController.install(trainIdField, outputDirField);
        wireActions();
        WorkflowShortcutBinder.bindPreviewShortcut(getRootPane(), loadButton, "loadRailPreview");
        setButtonsEnabled(false);
        loadPrintersAsync();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Train ID:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.2;
        panel.add(trainIdField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(loadButton, gbc);
        gbc.gridx = 3;
        panel.add(generatePdfButton, gbc);
        gbc.gridx = 4;
        panel.add(printButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Printer:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(printerCombo, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(printNowCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Output Directory:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(outputDirField, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseOutputDirectory());
        panel.add(browseButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(new JLabel("Print to file keeps the generated PDF in the output directory."), gbc);

        return panel;
    }

    private JComponent buildCenterPanel() {
        previewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        previewTable.getSelectionModel().addListSelectionListener(e -> updateCardPreviewFromSelection());
        JScrollPane tableScroll = new JScrollPane(previewTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Railcar Preview Table"));

        JScrollPane cardScroll = new JScrollPane(cardPreviewArea);
        cardScroll.setBorder(BorderFactory.createTitledBorder("Railcar Card Preview"));

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, cardScroll);
        horizontal.setDividerLocation(560);

        JScrollPane diagnosticsScroll = new JScrollPane(diagnosticsArea);
        diagnosticsScroll.setBorder(BorderFactory.createTitledBorder("Diagnostics"));
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontal, diagnosticsScroll);
        vertical.setDividerLocation(520);
        return vertical;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(statusLabel, BorderLayout.WEST);
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.add(closeButton);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private void wireActions() {
        loadButton.addActionListener(e -> loadPreview());
        generatePdfButton.addActionListener(e -> generate(false));
        printButton.addActionListener(e -> generate(true));
        printerCombo.addActionListener(e -> syncPrintTargetUi());
    }

    private void browseOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!outputDirField.getText().isBlank()) {
            chooser.setSelectedFile(Paths.get(outputDirField.getText()).toFile());
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadPreview() {
        String trainId = trainIdField.getText().trim();
        if (trainId.isEmpty()) {
            showError("Train ID is required.");
            return;
        }

        setBusy("Loading WMS rail rows...");
        clearPreview();
        setButtonsEnabled(false);

        SwingWorker<RailWorkflowService.PreparedRailJob, Void> worker = new SwingWorker<>() {
            @Override
            protected RailWorkflowService.PreparedRailJob doInBackground() throws Exception {
                return service.prepareRailJob(trainId);
            }

            @Override
            protected void done() {
                try {
                    preparedJob = get();
                    renderTable(preparedJob.getCards());
                    diagnosticsArea.setText(service.buildDiagnosticsText(preparedJob));
                    if (!preparedJob.getCards().isEmpty()) {
                        previewTable.setRowSelectionInterval(0, 0);
                    }
                    setButtonsEnabled(true);
                    setReady("Preview ready.");
                } catch (Exception ex) {
                    setReady("Preview failed.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void generate(boolean forcePrint) {
        if (preparedJob == null) {
            showError("Load Preview first.");
            return;
        }

        LabelWorkflowService.PrinterOption selectedPrinter = (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem();
        boolean printToFile = GuiPrinterTargetSupport.isPrintToFile(selectedPrinter);
        boolean shouldPrint = !printToFile && (forcePrint || printNowCheck.isSelected());
        setBusy(shouldPrint ? "Generating PDF and printing..." : "Generating PDF...");
        loadButton.setEnabled(false);
        generatePdfButton.setEnabled(false);
        printButton.setEnabled(false);

        SwingWorker<RailWorkflowService.GenerationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected RailWorkflowService.GenerationResult doInBackground() throws Exception {
                Path outputDir = outputDirField.getText().trim().isEmpty() ? null : Paths.get(outputDirField.getText().trim());
                String printerId = shouldPrint && selectedPrinter != null ? selectedPrinter.getId() : GuiPrinterTargetSupport.FILE_PRINTER_ID;
                return service.generatePdf(preparedJob, outputDir, printerId);
            }

            @Override
            protected void done() {
                loadButton.setEnabled(true);
                setButtonsEnabled(preparedJob != null);
                try {
                    RailWorkflowService.GenerationResult result = get();
                    String message = "PDF generated:\n" + result.getPdfPath() +
                            "\n\nOutput directory:\n" + result.getOutputDirectory() +
                            (result.isPrinted() ? "\n\nPrint command sent to " + result.getPrinterId() + "." : "");
                    diagnosticsArea.append("\n\nGeneration Result\n-----------------\n" + message + '\n');
                    setReady(result.isPrinted() ? "PDF generated and print command sent." : "PDF generated.");
                } catch (Exception ex) {
                    setReady("Generation failed.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void renderTable(List<RailCarCard> cards) {
        tableModel.setRowCount(0);
        for (RailCarCard card : cards) {
            tableModel.addRow(new Object[]{
                    card.getSequence(),
                    card.getVehicleId(),
                    card.getCanPallets(),
                    card.getDomPallets(),
                    card.getKevPallets(),
                    card.getLoadNumbers()
            });
        }
    }

    private void updateCardPreviewFromSelection() {
        if (preparedJob == null) {
            cardPreviewArea.setText("");
            return;
        }
        int index = previewTable.getSelectedRow();
        if (index < 0 || index >= preparedJob.getCards().size()) {
            cardPreviewArea.setText("");
            return;
        }
        cardPreviewArea.setText(service.buildCardPreviewText(preparedJob.getCards().get(index)));
        cardPreviewArea.setCaretPosition(0);
    }

    private void clearPreview() {
        preparedJob = null;
        tableModel.setRowCount(0);
        cardPreviewArea.setText("");
        diagnosticsArea.setText("");
    }

    private void loadPrintersAsync() {
        statusLabel.setText("Loading rail printers...");
        printerCombo.setEnabled(false);
        SwingWorker<List<LabelWorkflowService.PrinterOption>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LabelWorkflowService.PrinterOption> doInBackground() throws Exception {
                return service.loadRailPrinters();
            }

            @Override
            protected void done() {
                try {
                    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
                    model.addElement(GuiPrinterTargetSupport.buildSystemDefaultPrinterOption());
                    for (LabelWorkflowService.PrinterOption option : get()) {
                        model.addElement(option);
                    }
                    model.addElement(GuiPrinterTargetSupport.buildPrintToFileOption(defaultOutputDir()));
                    printerCombo.setModel(model);
                    if (model.getSize() > 0) {
                        printerCombo.setSelectedIndex(0);
                    }
                    printerCombo.setEnabled(true);
                    syncPrintTargetUi();
                    setReady("Ready.");
                } catch (Exception ex) {
                    setReady("Failed to load rail printers.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void syncPrintTargetUi() {
        LabelWorkflowService.PrinterOption selectedPrinter = (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem();
        boolean printToFile = GuiPrinterTargetSupport.isPrintToFile(selectedPrinter);
        if (printToFile) {
            printNowCheck.setSelected(false);
        }
        printNowCheck.setEnabled(!printToFile);
        printButton.setText(printToFile ? "Save PDF" : "Print");
    }

    private Path defaultOutputDir() {
        return outputDirField.getText().trim().isEmpty()
                ? Paths.get("out", "rail-gui").toAbsolutePath()
                : Paths.get(outputDirField.getText().trim()).toAbsolutePath();
    }

    private void setButtonsEnabled(boolean enabled) {
        generatePdfButton.setEnabled(enabled);
        printButton.setEnabled(enabled);
    }

    private void setBusy(String message) {
        statusLabel.setText(message);
        loadButton.setEnabled(false);
    }

    private void setReady(String message) {
        statusLabel.setText(message);
        loadButton.setEnabled(true);
    }

    private String rootMessage(Throwable throwable) {
        return GuiExceptionMessageSupport.rootMessage(throwable);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Rail Labels", JOptionPane.ERROR_MESSAGE);
    }
}

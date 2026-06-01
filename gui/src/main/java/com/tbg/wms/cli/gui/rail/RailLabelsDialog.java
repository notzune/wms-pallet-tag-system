/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.GuiExceptionMessageSupport;
import com.tbg.wms.cli.gui.GuiHelpSupport;
import com.tbg.wms.cli.gui.GuiHelpTopics;
import com.tbg.wms.cli.gui.GuiPrinterTargetSupport;
import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.cli.gui.TextFieldClipboardController;
import com.tbg.wms.cli.gui.WorkflowShortcutBinder;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.rail.RailCarCard;

import javax.swing.*;
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
    private final JTextField labelDateField = new JTextField(8);
    private final JTextField outputDirField = new JTextField(48);
    private final JComboBox<LabelWorkflowService.PrinterOption> printerCombo = new JComboBox<>();
    private final JCheckBox printNowCheck = new JCheckBox("Print after PDF generation", false);
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JButton loadButton = new JButton("Load Preview");
    private final JButton generatePdfButton = new JButton("Generate PDF");
    private final JButton printButton = new JButton("Print");
    private final JButton selectAllButton = new JButton("Select All");
    private final JButton clearAllButton = new JButton("Clear All");
    private final JButton invertSelectionButton = new JButton("Invert");
    private final JButton calendarButton = new JButton("Calendar...");
    private final JButton todayButton = new JButton("Today");

    private final RailPrintableCardTableModel tableModel = new RailPrintableCardTableModel();
    private final JTable previewTable = new JTable(tableModel);
    private final JTextArea cardPreviewArea = new JTextArea();
    private final JTextArea diagnosticsArea = new JTextArea();

    private final transient RailWorkflowService service;
    private final transient TextFieldClipboardController clipboardController = new TextFieldClipboardController();
    private final transient RailLabelDateSupport dateSupport = new RailLabelDateSupport();
    private final transient RailDialogSupport dialogSupport = new RailDialogSupport();
    private final transient RailDialogExecutionSupport executionSupport = new RailDialogExecutionSupport();
    private final transient RailDialogActionSupport actionSupport =
            new RailDialogActionSupport(dialogSupport, executionSupport);
    private final transient RailDialogUiStateSupport uiStateSupport = new RailDialogUiStateSupport();
    private transient RailWorkflowService.PreparedRailJob preparedJob;

    public RailLabelsDialog(JFrame owner, AppConfig config) {
        super(owner, "Rail Labels Workflow", true);
        this.service = new RailWorkflowService(Objects.requireNonNull(config, "config cannot be null"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1220, 820);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        outputDirField.setText(Paths.get("out", "rail-gui").toAbsolutePath().toString());
        labelDateField.setText(dateSupport.todayText());
        cardPreviewArea.setEditable(false);
        diagnosticsArea.setEditable(false);
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        cardPreviewArea.setFont(mono);
        diagnosticsArea.setFont(mono);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        clipboardController.install(trainIdField, labelDateField, outputDirField);
        wireActions();
        bindTableSelectionShortcuts();
        WorkflowShortcutBinder.bindPreviewShortcut(getRootPane(), loadButton, "loadRailPreview");
        applyUiState(uiStateSupport.initial());
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

        gbc.gridx = 5;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        panel.add(GuiHelpSupport.createHelpButton(this, "Rail Labels", GuiHelpTopics.railLabels()), gbc);
        gbc.anchor = GridBagConstraints.WEST;

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
        panel.add(new JLabel("Label Date:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(labelDateField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(calendarButton, gbc);

        gbc.gridx = 3;
        panel.add(todayButton, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("MM-DD-YY"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
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
        gbc.gridy = 4;
        panel.add(new JLabel("Print to file keeps the generated PDF in the output directory."), gbc);

        return panel;
    }

    private JComponent buildCenterPanel() {
        previewTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        previewTable.getSelectionModel().addListSelectionListener(e -> updateCardPreviewFromSelection());
        JScrollPane tableScroll = new JScrollPane(previewTable);
        JPanel tablePanel = new JPanel(new BorderLayout(4, 4));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Railcar Preview Table"));
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        JPanel selectionControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        selectionControls.add(selectAllButton);
        selectionControls.add(clearAllButton);
        selectionControls.add(invertSelectionButton);
        tablePanel.add(selectionControls, BorderLayout.SOUTH);

        JScrollPane cardScroll = new JScrollPane(cardPreviewArea);
        cardScroll.setBorder(BorderFactory.createTitledBorder("Railcar Card Preview"));

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, cardScroll);
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
        selectAllButton.addActionListener(e -> tableModel.setAllPrintable());
        clearAllButton.addActionListener(e -> tableModel.clearAllPrintable());
        invertSelectionButton.addActionListener(e -> tableModel.invertPrintable());
        todayButton.addActionListener(e -> labelDateField.setText(dateSupport.todayText()));
        calendarButton.addActionListener(e -> showCalendarPopup());
    }

    private void bindTableSelectionShortcuts() {
        previewTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "togglePrintableRows");
        previewTable.getActionMap().put("togglePrintableRows", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int[] selectedRows = previewTable.getSelectedRows();
                int[] modelRows = new int[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    modelRows[i] = previewTable.convertRowIndexToModel(selectedRows[i]);
                }
                tableModel.togglePrintableRows(modelRows);
            }
        });
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

    private void showCalendarPopup() {
        JPopupMenu popup = new JPopupMenu();
        SpinnerDateModel model = new SpinnerDateModel(dateSupport.toDate(labelDateField.getText()), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "MM-dd-yy"));
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            labelDateField.setText(dateSupport.formatDate(model.getDate()));
            popup.setVisible(false);
        });
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(spinner, BorderLayout.CENTER);
        panel.add(applyButton, BorderLayout.EAST);
        popup.add(panel);
        popup.show(calendarButton, 0, calendarButton.getHeight());
    }

    private void loadPreview() {
        RailDialogExecutionSupport.PreviewRequest request;
        try {
            request = executionSupport.preparePreviewRequest(trainIdField.getText(), labelDateField.getText());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        applyUiState(uiStateSupport.previewLoading());
        clearPreview();

        SwingWorker<RailWorkflowService.PreparedRailJob, Void> worker = new SwingWorker<>() {
            @Override
            protected RailWorkflowService.PreparedRailJob doInBackground() throws Exception {
                return service.prepareRailJob(request.trainIds(), request.labelDate());
            }

            @Override
            protected void done() {
                try {
                    preparedJob = get();
                    RailDialogActionSupport.PreviewOutcome outcome =
                            actionSupport.buildPreviewOutcome(preparedJob, service.buildDiagnosticsText(preparedJob));
                    renderTable(outcome.cards());
                    diagnosticsArea.setText(outcome.diagnosticsText());
                    if (outcome.shouldSelectFirstRow()) {
                        previewTable.setRowSelectionInterval(0, 0);
                    }
                    applyUiState(uiStateSupport.previewReady(outcome.readyMessage()));
                } catch (Exception ex) {
                    applyUiState(uiStateSupport.previewFailed(executionSupport.previewFailedMessage()));
                    showError(executionSupport.rootMessage(ex));
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

        RailDialogExecutionSupport.GenerationRequest request;
        try {
            request = executionSupport.prepareGenerationRequest(
                    preparedJob,
                    tableModel.selectedCards(),
                    outputDirField.getText(),
                    (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem(),
                    forcePrint,
                    printNowCheck.isSelected()
            );
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        applyUiState(uiStateSupport.generationBusy(executionSupport.generationBusyMessage(request.shouldPrint())));

        SwingWorker<RailWorkflowService.GenerationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected RailWorkflowService.GenerationResult doInBackground() throws Exception {
                return service.generatePdf(preparedJob, request.selectedCards(), request.outputDirectory(), request.printerId());
            }

            @Override
            protected void done() {
                try {
                    RailWorkflowService.GenerationResult result = get();
                    RailDialogActionSupport.GenerationOutcome outcome = actionSupport.buildGenerationOutcome(result);
                    diagnosticsArea.append(outcome.diagnosticsAppend());
                    applyUiState(uiStateSupport.generationComplete(outcome.readyMessage(), preparedJob != null));
                } catch (Exception ex) {
                    applyUiState(uiStateSupport.generationComplete(executionSupport.generationFailedMessage(), preparedJob != null));
                    showError(executionSupport.rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void renderTable(List<RailCarCard> cards) {
        tableModel.setCards(cards);
    }

    private void updateCardPreviewFromSelection() {
        if (preparedJob == null) {
            cardPreviewArea.setText("");
            return;
        }
        int index = previewTable.getSelectedRow();
        if (index < 0) {
            cardPreviewArea.setText("");
            return;
        }
        int modelIndex = previewTable.convertRowIndexToModel(index);
        if (modelIndex < 0 || modelIndex >= tableModel.getRowCount()) {
            cardPreviewArea.setText("");
            return;
        }
        cardPreviewArea.setText(service.buildCardPreviewText(tableModel.cardAt(modelIndex)));
        cardPreviewArea.setCaretPosition(0);
    }

    private void clearPreview() {
        preparedJob = null;
        tableModel.setCards(List.of());
        cardPreviewArea.setText("");
        diagnosticsArea.setText("");
    }

    private void loadPrintersAsync() {
        applyUiState(uiStateSupport.loadingPrinters());
        SwingWorker<List<LabelWorkflowService.PrinterOption>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LabelWorkflowService.PrinterOption> doInBackground() throws Exception {
                return service.loadRailPrinters();
            }

            @Override
            protected void done() {
                try {
                    RailDialogActionSupport.PrinterLoadOutcome outcome =
                            actionSupport.buildPrinterLoadOutcome(get(), defaultOutputDir());
                    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = outcome.model();
                    printerCombo.setModel(model);
                    if (outcome.shouldSelectFirst()) {
                        printerCombo.setSelectedIndex(0);
                    }
                    syncPrintTargetUi();
                    applyUiState(uiStateSupport.printersReady(outcome.readyMessage()));
                } catch (Exception ex) {
                    applyUiState(uiStateSupport.printersReady("Failed to load rail printers."));
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void syncPrintTargetUi() {
        LabelWorkflowService.PrinterOption selectedPrinter = (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem();
        RailDialogSupport.PrintTargetState state =
                dialogSupport.syncPrintTargetState(selectedPrinter, printNowCheck.isSelected());
        printNowCheck.setSelected(state.printNowSelected());
        printNowCheck.setEnabled(state.printNowEnabled());
        printButton.setText(state.printButtonText());
    }

    private Path defaultOutputDir() {
        return dialogSupport.resolveDefaultOutputDir(outputDirField.getText());
    }

    private void applyUiState(RailDialogUiStateSupport.UiState state) {
        statusLabel.setText(state.statusMessage());
        loadButton.setEnabled(state.loadEnabled());
        generatePdfButton.setEnabled(state.generateEnabled());
        printButton.setEnabled(state.printEnabled());
        printerCombo.setEnabled(state.printerEnabled());
    }

    private String rootMessage(Throwable throwable) {
        return GuiExceptionMessageSupport.rootMessage(throwable);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Rail Labels", JOptionPane.ERROR_MESSAGE);
    }
}

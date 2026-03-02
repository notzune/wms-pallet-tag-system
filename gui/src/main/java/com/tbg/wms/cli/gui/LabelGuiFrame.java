/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.gui;

import com.tbg.wms.cli.gui.rail.RailLabelsDialog;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.print.PrinterConfig;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Swing desktop GUI for shipment/carrier preview and print workflows.
 *
 * <p>This frame coordinates high-level user interactions and delegates focused behaviors
 * (for example barcode-dialog rendering and text-field clipboard policy) to dedicated helpers
 * to keep responsibilities separated.</p>
 */
public final class LabelGuiFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    // Synthetic printer option used to enable print-to-file from the dropdown.
    private static final String FILE_PRINTER_ID = "FILE";
    private static final String PREF_PRINT_TO_FILE_DIR = "printToFile.defaultOutputDir";
    private static final int SHIPMENT_MIN_CHARS = 11;
    private static final int COMBO_WIDTH_REDUCTION_PX = 12;
    private static final DateTimeFormatter OUTPUT_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_QUEUE_ITEMS = 500;
    private static final int MAX_PREVIEW_STOPS = 250;
    private static final int MAX_PREVIEW_SHIPMENTS_PER_STOP = 250;
    private static final int MAX_PREVIEW_SKU_ROWS_PER_SHIPMENT = 1000;
    private final JLabel inputLabel = new JLabel("Carrier Move ID:");
    private final JTextField shipmentField = new JTextField(24);
    private final JRadioButton carrierMoveModeButton = new JRadioButton("Carrier Move ID", true);
    private final JRadioButton shipmentModeButton = new JRadioButton("Shipment ID");
    private final JComboBox<LabelWorkflowService.PrinterOption> printerCombo = new JComboBox<>();
    private final JButton previewButton = new JButton("Preview");
    private final JButton clearButton = new JButton("Clear");
    private final JButton printButton = new JButton("Confirm Print");
    private final JTextArea shipmentArea = new JTextArea();
    private final JPanel shipmentPreviewPanel = new JPanel();
    private final JTextArea mathArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready.");
    private final transient Preferences preferences = Preferences.userNodeForPackage(LabelGuiFrame.class);
    private final transient TextFieldClipboardController clipboardController = new TextFieldClipboardController();

    private final transient AppConfig config = new AppConfig();
    private final transient LabelWorkflowService service = new LabelWorkflowService(config);
    private final transient AdvancedPrintWorkflowService advancedService = new AdvancedPrintWorkflowService(config);
    private final transient LabelPreviewFormatter previewFormatter = new LabelPreviewFormatter();
    private final transient BarcodeDialogFactory barcodeDialogFactory = new BarcodeDialogFactory(new BarcodeDependencies());
    private transient List<LabelWorkflowService.PrinterOption> loadedPrinters = List.of();
    private transient LabelWorkflowService.PreparedJob preparedJob;
    private transient AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob;

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
        installTerminalLikeMouseClipboardBehavior(shipmentField);
        updateInputModeUi();
        autoResumeIfFound();
        applyTopRowSizing();
        loadPrintersAsync();
        printButton.setEnabled(false);
    }

    private static String buildWindowTitle() {
        Package pkg = LabelGuiFrame.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        if (version == null || version.isBlank()) {
            version = System.getProperty("wms.tags.version", "");
        }
        return version == null || version.isBlank()
                ? "WMS Pallet Tag System"
                : "WMS Pallet Tag System - " + version;
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
        JMenuItem queueItem = new JMenuItem("Queue Print...");
        queueItem.addActionListener(e -> openQueueDialog());
        toolsMenu.add(queueItem);
        JMenuItem railLabelsItem = new JMenuItem("Rail Labels...");
        railLabelsItem.addActionListener(e -> openRailLabelsDialog());
        toolsMenu.add(railLabelsItem);
        JMenuItem resumeItem = new JMenuItem("Resume Incomplete Job...");
        resumeItem.addActionListener(e -> openResumeDialog());
        toolsMenu.add(resumeItem);
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(e -> openSettingsDialog());
        toolsMenu.addSeparator();
        toolsMenu.add(settingsItem);

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
        panel.add(inputLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.20;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(shipmentField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        ButtonGroup inputModeGroup = new ButtonGroup();
        inputModeGroup.add(carrierMoveModeButton);
        inputModeGroup.add(shipmentModeButton);
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        modePanel.add(carrierMoveModeButton);
        modePanel.add(shipmentModeButton);
        panel.add(modePanel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Printer:"), gbc);

        gbc.gridx = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.80;
        panel.add(printerCombo, gbc);

        gbc.gridx = 5;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(previewButton, gbc);

        gbc.gridx = 6;
        panel.add(clearButton, gbc);

        gbc.gridx = 7;
        panel.add(printButton, gbc);

        return panel;
    }

    private JComponent buildCenterPanel() {
        shipmentArea.setEditable(false);
        shipmentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        shipmentPreviewPanel.setLayout(new BoxLayout(shipmentPreviewPanel, BoxLayout.Y_AXIS));
        shipmentPreviewPanel.add(shipmentArea);
        mathArea.setEditable(false);
        mathArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(shipmentPreviewPanel),
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
        carrierMoveModeButton.addActionListener(e -> updateInputModeUi());
        shipmentModeButton.addActionListener(e -> updateInputModeUi());
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
                    loadedPrinters = List.copyOf(printers);
                    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = buildPrintTargetModel(true);
                    int printerCount = printers.size();
                    printerCombo.setModel(model);
                    applyTopRowSizing();
                    if (model.getSize() > 0) {
                        printerCombo.setSelectedIndex(0);
                        if (printerCount == 0) {
                            setReady("No enabled printers found. Print to file available.");
                        } else {
                            setReady("Printers loaded.");
                        }
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
        String inputId = shipmentField.getText().trim();
        if (inputId.isEmpty()) {
            showError(isCarrierMoveMode() ? "Enter a Carrier Move ID." : "Enter a Shipment ID.");
            return;
        }

        setBusy("Preparing preview...");
        printButton.setEnabled(false);
        preparedJob = null;
        preparedCarrierJob = null;

        SwingWorker<Object, Void> worker = new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                if (isCarrierMoveMode()) {
                    return advancedService.prepareCarrierMoveJob(inputId);
                }
                return service.prepareJob(inputId);
            }

            @Override
            protected void done() {
                try {
                    Object prepared = get();
                    if (prepared instanceof AdvancedPrintWorkflowService.PreparedCarrierMoveJob) {
                        preparedCarrierJob = (AdvancedPrintWorkflowService.PreparedCarrierMoveJob) prepared;
                        renderCarrierMovePreview(preparedCarrierJob);
                    } else {
                        preparedJob = (LabelWorkflowService.PreparedJob) prepared;
                        renderPreview(preparedJob);
                    }
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
        shipmentPreviewPanel.removeAll();
        shipmentArea.setText(previewFormatter.buildShipmentSummaryText(job, 1));
        shipmentPreviewPanel.add(shipmentArea);
        mathArea.setText(previewFormatter.buildShipmentMathText(job, MAX_PREVIEW_SKU_ROWS_PER_SHIPMENT));
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
    }

    private void renderCarrierMovePreview(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        shipmentPreviewPanel.removeAll();
        StringBuilder summary = new StringBuilder(previewFormatter.buildCarrierMoveSummary(job));
        shipmentArea.setText(summary.toString());
        shipmentPreviewPanel.add(shipmentArea);

        int shownStops = addStopPreviewSections(job);
        if (job.getStopGroups().size() > shownStops) {
            summary.append("Preview Notice: Showing first ").append(MAX_PREVIEW_STOPS)
                    .append(" stops of ").append(job.getStopGroups().size()).append(".\n");
            shipmentArea.setText(summary.toString());
        }

        mathArea.setText(previewFormatter.buildCarrierMoveMathText(
                job,
                MAX_PREVIEW_STOPS,
                MAX_PREVIEW_SHIPMENTS_PER_STOP));
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
    }

    private JComponent buildStopPreviewSection(AdvancedPrintWorkflowService.PreparedStopGroup stop) {
        Objects.requireNonNull(stop, "stop cannot be null");
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));

        String label = previewFormatter.stopPreviewLabel(stop);
        JToggleButton toggle = new JToggleButton(label + "  [expanded]", true);
        toggle.setFocusPainted(false);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        container.add(toggle, BorderLayout.NORTH);

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        details.setRows(Math.max(16, stop.getShipmentJobs().size() * 16));
        details.setText(previewFormatter.buildStopDetailsText(
                stop,
                MAX_PREVIEW_SHIPMENTS_PER_STOP,
                MAX_PREVIEW_SKU_ROWS_PER_SHIPMENT));

        JScrollPane detailsScroll = new JScrollPane(details);
        detailsScroll.setBorder(BorderFactory.createEmptyBorder());
        container.add(detailsScroll, BorderLayout.CENTER);

        toggle.addActionListener(e -> {
            boolean expanded = toggle.isSelected();
            toggle.setText(label + (expanded ? "  [expanded]" : "  [collapsed]"));
            detailsScroll.setVisible(expanded);
            container.revalidate();
            container.repaint();
        });
        return container;
    }

    private int addStopPreviewSections(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        int shown = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            if (shown >= MAX_PREVIEW_STOPS) {
                break;
            }
            shipmentPreviewPanel.add(buildStopPreviewSection(stop));
            shown++;
        }
        return shown;
    }

    private void confirmAndPrint() {
        boolean carrierMoveMode = isCarrierMoveMode();
        boolean previewMissing = carrierMoveMode ? preparedCarrierJob == null : preparedJob == null;
        if (previewMissing) {
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
            int plannedLabels = carrierMoveMode
                    ? previewFormatter.countCarrierMoveLabels(preparedCarrierJob)
                    : preparedJob.getLpnsForLabels().size();
            int plannedInfoTags = carrierMoveMode
                    ? preparedCarrierJob.getTotalStops() + 1
                    : 1;
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    carrierMoveMode
                            ? "Print " + plannedLabels + " labels + " + plannedInfoTags + " info tags to " + selected + "?"
                            : "Print " + plannedLabels + " labels to " + selected + "?",
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

        SwingWorker<AdvancedPrintWorkflowService.PrintResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AdvancedPrintWorkflowService.PrintResult doInBackground() throws Exception {
                String printerId = printToFile ? null : (selected == null ? null : selected.getId());
                if (carrierMoveMode) {
                    Path outDir = defaultPrintToFileOutputDir().resolve("gui-cmid-" + preparedCarrierJob.getCarrierMoveId() + "-" +
                            OUTPUT_TS.format(LocalDateTime.now()));
                    return advancedService.printCarrierMoveJob(preparedCarrierJob, printerId, outDir, printToFile);
                }
                Path outDir = defaultPrintToFileOutputDir().resolve("gui-" + preparedJob.getShipmentId() + "-" +
                        OUTPUT_TS.format(LocalDateTime.now()));
                return advancedService.printShipmentJob(preparedJob, printerId, outDir, printToFile);
            }

            @Override
            protected void done() {
                previewButton.setEnabled(true);
                printButton.setEnabled(true);
                clearButton.setEnabled(true);
                try {
                    AdvancedPrintWorkflowService.PrintResult result = get();
                    if (result.isPrintToFile()) {
                        setReady("Saved " + result.getLabelsPrinted() + " labels and " + result.getInfoTagsPrinted() +
                                " info tags to " + result.getOutputDirectory());
                        JOptionPane.showMessageDialog(
                                LabelGuiFrame.this,
                                "Saved " + result.getLabelsPrinted() + " labels and " + result.getInfoTagsPrinted() +
                                        " info tags.\nOutput: " + result.getOutputDirectory(),
                                "Print Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        setReady("Printed " + result.getLabelsPrinted() + " labels and " + result.getInfoTagsPrinted() +
                                " info tags to " + result.getPrinterId() +
                                " (" + result.getPrinterEndpoint() + ")");
                        JOptionPane.showMessageDialog(
                                LabelGuiFrame.this,
                                "Printed " + result.getLabelsPrinted() + " labels and " + result.getInfoTagsPrinted() +
                                        " info tags.\nOutput: " + result.getOutputDirectory(),
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
        shipmentPreviewPanel.removeAll();
        shipmentPreviewPanel.add(shipmentArea);
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
        mathArea.setText("");
        preparedJob = null;
        preparedCarrierJob = null;
        printButton.setEnabled(false);
        shipmentField.requestFocusInWindow();
        setReady("Cleared. Enter the next " + (isCarrierMoveMode() ? "Carrier Move ID." : "Shipment ID."));
    }

    private void updateInputModeUi() {
        inputLabel.setText(isCarrierMoveMode() ? "Carrier Move ID:" : "Shipment ID:");
        preparedJob = null;
        preparedCarrierJob = null;
        shipmentArea.setText("");
        shipmentPreviewPanel.removeAll();
        shipmentPreviewPanel.add(shipmentArea);
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
        mathArea.setText("");
        printButton.setEnabled(false);
    }

    private boolean isCarrierMoveMode() {
        return carrierMoveModeButton.isSelected();
    }

    private void openQueueDialog() {
        JDialog dialog = new JDialog(this, "Queue Print", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(8, 8));

        JTextArea inputArea = new JTextArea(12, 72);
        inputArea.setLineWrap(false);
        installTerminalLikeMouseClipboardBehavior(inputArea);

        JComboBox<String> defaultType = new JComboBox<>(new String[]{"Carrier Move ID", "Shipment ID"});
        JLabel hint = new JLabel("Use prefixes for mixed queue: C:<cmid> or S:<shipment>. One item per line.");

        JTextArea previewArea = new JTextArea(14, 72);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel top = new JPanel(new BorderLayout(4, 4));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Default Type:"));
        controls.add(defaultType);
        top.add(controls, BorderLayout.NORTH);
        top.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        top.add(hint, BorderLayout.SOUTH);

        JButton previewBtn = new JButton("Preview Queue");
        JButton printBtn = new JButton("Print Queue");
        JButton closeBtn = new JButton("Close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(previewBtn);
        buttons.add(printBtn);
        buttons.add(closeBtn);

        final AdvancedPrintWorkflowService.PreparedQueueJob[] prepared = new AdvancedPrintWorkflowService.PreparedQueueJob[1];

        previewBtn.addActionListener(e -> {
            try {
                List<AdvancedPrintWorkflowService.QueueRequestItem> requests =
                        QueueInputParser.parse(
                                inputArea.getText(),
                                defaultType.getSelectedIndex() == 0
                                        ? AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE
                                        : AdvancedPrintWorkflowService.QueueItemType.SHIPMENT,
                                MAX_QUEUE_ITEMS);
                prepared[0] = advancedService.prepareQueue(requests);
                previewArea.setText(previewFormatter.buildQueuePreview(prepared[0]));
            } catch (Exception ex) {
                showError(rootMessage(ex));
            }
        });

        printBtn.addActionListener(e -> {
            if (prepared[0] == null) {
                showError("Preview queue first.");
                return;
            }
            LabelWorkflowService.PrinterOption selected = (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem();
            boolean printToFile = isPrintToFileSelected(selected);
            String printerId = printToFile ? null : (selected == null ? null : selected.getId());
            try {
                AdvancedPrintWorkflowService.QueuePrintResult result = advancedService.printQueue(prepared[0], printerId, printToFile);
                JOptionPane.showMessageDialog(dialog,
                        "Queue complete.\nLabels: " + result.getTotalLabelsPrinted() +
                                "\nInfo Tags: " + result.getTotalInfoTagsPrinted(),
                        "Queue Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                showError("Queue print failed: " + rootMessage(ex));
            }
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(new JScrollPane(previewArea), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openResumeDialog() {
        try {
            List<AdvancedPrintWorkflowService.ResumeCandidate> candidates = advancedService.listIncompleteJobs();
            if (candidates.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No incomplete jobs found.", "Resume Job", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] options = new String[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                AdvancedPrintWorkflowService.ResumeCandidate c = candidates.get(i);
                options[i] = c.mode() + " " + c.sourceId() + " | progress " + c.nextTaskIndex() + "/" + c.totalTasks();
            }
            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Select a job to resume (safe mode reprints last successful label/tag):",
                    "Resume Incomplete Job",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (selected == null) {
                return;
            }
            int index = -1;
            for (int i = 0; i < options.length; i++) {
                if (Objects.equals(options[i], selected)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                return;
            }
            AdvancedPrintWorkflowService.PrintResult result = advancedService.resumeJob(candidates.get(index).checkpointId());
            JOptionPane.showMessageDialog(this,
                    "Resume complete.\nLabels: " + result.getLabelsPrinted() +
                            "\nInfo Tags: " + result.getInfoTagsPrinted() +
                            "\nOutput: " + result.getOutputDirectory(),
                    "Resume Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Resume failed: " + rootMessage(ex));
        }
    }

    private void openRailLabelsDialog() {
        RailLabelsDialog dialog = new RailLabelsDialog(this, config);
        dialog.setVisible(true);
    }

    private void autoResumeIfFound() {
        SwingUtilities.invokeLater(() -> {
            try {
                List<AdvancedPrintWorkflowService.ResumeCandidate> candidates = advancedService.listIncompleteJobs();
                if (candidates.isEmpty()) {
                    return;
                }
                AdvancedPrintWorkflowService.ResumeCandidate latest = candidates.get(0);
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "Found incomplete job (" + latest.mode() + " " + latest.sourceId() + ", " +
                                latest.nextTaskIndex() + "/" + latest.totalTasks() + ").\nResume now?",
                        "Incomplete Job Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    AdvancedPrintWorkflowService.PrintResult result = advancedService.resumeJob(latest.checkpointId());
                    setReady("Resumed job. Printed " + result.getLabelsPrinted() + " labels and " +
                            result.getInfoTagsPrinted() + " info tags.");
                }
            } catch (Exception ignored) {
                // Startup should continue even if resume scan fails.
            }
        });
    }

    private void applyTopRowSizing() {
        FontMetrics metrics = shipmentField.getFontMetrics(shipmentField.getFont());
        int shipmentMinWidth = (metrics.charWidth('0') * SHIPMENT_MIN_CHARS) + 20;

        Dimension shipmentPreferred = shipmentField.getPreferredSize();
        shipmentField.setPreferredSize(new Dimension(
                Math.max(shipmentPreferred.width, shipmentMinWidth),
                shipmentPreferred.height));
        shipmentField.setMinimumSize(new Dimension(shipmentMinWidth, shipmentPreferred.height));

        Dimension comboPreferred = printerCombo.getPreferredSize();
        int comboWidth = Math.max(280, comboPreferred.width - COMBO_WIDTH_REDUCTION_PX);
        int comboMinWidth = Math.max(220, comboWidth - 80);
        printerCombo.setPreferredSize(new Dimension(comboWidth, comboPreferred.height));
        printerCombo.setMinimumSize(new Dimension(comboMinWidth, comboPreferred.height));
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = Objects.requireNonNullElse(throwable, new RuntimeException("Unknown error"));
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return (message == null || message.isBlank()) ? cursor.getClass().getSimpleName() : message;
    }

    private void openBarcodeDialog() {
        barcodeDialogFactory.open(this);
    }

    private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(boolean includeFileOption) {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
        for (LabelWorkflowService.PrinterOption option : loadedPrinters) {
            model.addElement(option);
        }
        if (includeFileOption) {
            model.addElement(new LabelWorkflowService.PrinterOption(
                    FILE_PRINTER_ID,
                    "Print to file",
                    defaultPrintToFileOutputDir().toString()
            ));
        }
        return model;
    }

    private Path defaultPrintToFileOutputDir() {
        String configured = preferences.get(PREF_PRINT_TO_FILE_DIR, "");
        if (configured != null && !configured.isBlank()) {
            try {
                return Paths.get(configured.trim());
            } catch (InvalidPathException ignored) {
                // Fallback to runtime-derived default if persisted value is invalid.
            }
        }
        return resolveJarOutputDir();
    }

    private void openSettingsDialog() {
        JDialog dialog = new JDialog(this, "Settings", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField outputDirField = new JTextField(defaultPrintToFileOutputDir().toString(), 40);
        installTerminalLikeMouseClipboardBehavior(outputDirField);
        JButton browseButton = new JButton("Browse...");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        content.add(new JLabel("Default print-to-file output dir:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        content.add(outputDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        content.add(browseButton, gbc);

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.add(saveButton);
        buttonRow.add(cancelButton);

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(outputDirField.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select default print-to-file output directory");
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                outputDirField.setText(chooser.getSelectedFile().toPath().toString());
            }
        });

        saveButton.addActionListener(e -> {
            String raw = outputDirField.getText();
            if (raw == null || raw.isBlank()) {
                showError("Default output directory is required.");
                return;
            }

            Path configuredPath;
            try {
                configuredPath = Paths.get(raw.trim()).toAbsolutePath().normalize();
            } catch (InvalidPathException ex) {
                showError("Invalid output directory path.");
                return;
            }

            preferences.put(PREF_PRINT_TO_FILE_DIR, configuredPath.toString());
            LabelWorkflowService.PrinterOption previousSelection =
                    (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem();
            printerCombo.setModel(buildPrintTargetModel(true));
            applyTopRowSizing();
            restoreSelection(previousSelection);
            setReady("Settings saved.");
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(buttonRow, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void restoreSelection(LabelWorkflowService.PrinterOption previousSelection) {
        if (previousSelection == null) {
            if (printerCombo.getItemCount() > 0) {
                printerCombo.setSelectedIndex(0);
            }
            return;
        }
        for (int i = 0; i < printerCombo.getItemCount(); i++) {
            LabelWorkflowService.PrinterOption candidate = printerCombo.getItemAt(i);
            if (Objects.equals(candidate.getId(), previousSelection.getId())) {
                printerCombo.setSelectedIndex(i);
                return;
            }
        }
        if (printerCombo.getItemCount() > 0) {
            printerCombo.setSelectedIndex(0);
        }
    }

    private void installTerminalLikeMouseClipboardBehavior(JTextComponent... fields) {
        clipboardController.install(fields);
    }

    private final class BarcodeDependencies implements BarcodeDialogFactory.Dependencies {
        @Override
        public DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(boolean includeFileOption) {
            return LabelGuiFrame.this.buildPrintTargetModel(includeFileOption);
        }

        @Override
        public boolean isPrintToFileSelected(LabelWorkflowService.PrinterOption selected) {
            return LabelGuiFrame.isPrintToFileSelected(selected);
        }

        @Override
        public Path defaultPrintToFileOutputDir() {
            return LabelGuiFrame.this.defaultPrintToFileOutputDir();
        }

        @Override
        public void installClipboardBehavior(JTextComponent... fields) {
            LabelGuiFrame.this.installTerminalLikeMouseClipboardBehavior(fields);
        }

        @Override
        public void showError(String message) {
            LabelGuiFrame.this.showError(message);
        }

        @Override
        public String rootMessage(Throwable throwable) {
            return LabelGuiFrame.this.rootMessage(throwable);
        }

        @Override
        public PrinterConfig resolvePrinter(String printerId) throws Exception {
            return service.resolvePrinter(printerId);
        }
    }
}

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
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.prefs.Preferences;

/**
 * Swing-based GUI for shipment preview and label printing.
 */
public final class LabelGuiFrame extends JFrame {

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

    // Synthetic printer option used to enable print-to-file from the dropdown.
    private static final String FILE_PRINTER_ID = "FILE";
    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final String PREF_PRINT_TO_FILE_DIR = "printToFile.defaultOutputDir";
    private static final int SHIPMENT_MIN_CHARS = 11;
    private static final int COMBO_WIDTH_REDUCTION_PX = 12;
    private static final DateTimeFormatter OUTPUT_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final Preferences preferences = Preferences.userNodeForPackage(LabelGuiFrame.class);

    private final AppConfig config = new AppConfig();
    private final LabelWorkflowService service = new LabelWorkflowService(config);
    private final AdvancedPrintWorkflowService advancedService = new AdvancedPrintWorkflowService(config);
    private List<LabelWorkflowService.PrinterOption> loadedPrinters = List.of();
    private LabelWorkflowService.PreparedJob preparedJob;
    private AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob;

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
        summary.append(" - Info Tags To Generate: ").append(1).append('\n');
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
        shipmentPreviewPanel.add(shipmentArea);

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
                    row.getPartialPallets(),
                    row.getEstimatedPallets(),
                    value(row.getDescription())));
        }
        int totalFull = sumFullPallets(job.getSkuMathRows());
        int totalPartial = sumPartialPallets(job.getSkuMathRows());
        int totalLabels = sumEstimatedPallets(job.getSkuMathRows());
        math.append("----------------------------------------------------------------------------------------------------\n");
        math.append(String.format("Totals -> Full: %d | Partial: %d | Labels Needed: %d%n", totalFull, totalPartial, totalLabels));
        mathArea.setText(math.toString());
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
    }

    private void renderCarrierMovePreview(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        shipmentPreviewPanel.removeAll();
        StringBuilder summary = new StringBuilder();
        summary.append("Carrier Move ID: ").append(job.getCarrierMoveId()).append('\n');
        summary.append("Total Stops: ").append(job.getTotalStops()).append('\n');
        summary.append("Estimated Labels: ").append(countCarrierMoveLabels(job)).append('\n');
        summary.append("Estimated Info Tags: ").append(job.getTotalStops() + 1).append('\n');
        shipmentArea.setText(summary.toString());
        shipmentPreviewPanel.add(shipmentArea);

        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            shipmentPreviewPanel.add(buildStopPreviewSection(stop));
        }

        StringBuilder math = new StringBuilder();
        math.append("Carrier Move Pallet Math (Full vs Partial)\n");
        math.append(String.format("%-8s %-16s %-10s %-10s %-10s %-10s %-10s %s%n",
                "Stop", "Shipment", "Units", "Full", "Partial", "TotalPal", "LPNLbls", "Ship To"));
        math.append("----------------------------------------------------------------------------------------------------------------------\n");
        int totalFull = 0;
        int totalPartial = 0;
        int totalLabels = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                int full = sumFullPallets(shipmentJob.getSkuMathRows());
                int partial = sumPartialPallets(shipmentJob.getSkuMathRows());
                int estimated = sumEstimatedPallets(shipmentJob.getSkuMathRows());
                int lpnLabels = shipmentJob.getLpnsForLabels().size();
                int units = sumUnits(shipmentJob.getSkuMathRows());
                totalFull += full;
                totalPartial += partial;
                totalLabels += lpnLabels;
                math.append(String.format("%-8d %-16s %-10d %-10d %-10d %-10d %-10d %s%n",
                        stop.getStopPosition(),
                        value(shipmentJob.getShipmentId()),
                        units,
                        full,
                        partial,
                        estimated,
                        lpnLabels,
                        value(shipmentJob.getShipment().getShipToName())));
            }
        }
        math.append("----------------------------------------------------------------------------------------------------------------------\n");
        math.append(String.format("Totals -> Full: %d | Partial: %d | Labels Needed: %d%n", totalFull, totalPartial, totalLabels));
        mathArea.setText(math.toString());
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
    }

    private JComponent buildStopPreviewSection(AdvancedPrintWorkflowService.PreparedStopGroup stop) {
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));

        String label = "Stop " + stop.getStopPosition()
                + (stop.getStopSequence() == null ? "" : " (Seq " + stop.getStopSequence() + ")");
        JToggleButton toggle = new JToggleButton(label + "  [expanded]", true);
        toggle.setFocusPainted(false);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        container.add(toggle, BorderLayout.NORTH);

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        details.setRows(Math.max(4, stop.getShipmentJobs().size() * 2));
        StringBuilder section = new StringBuilder();
        int stopFull = 0;
        int stopPartial = 0;
        int stopLabels = 0;
        for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
            int full = sumFullPallets(shipmentJob.getSkuMathRows());
            int partial = sumPartialPallets(shipmentJob.getSkuMathRows());
            int labels = shipmentJob.getLpnsForLabels().size();
            stopFull += full;
            stopPartial += partial;
            stopLabels += labels;
            section.append("Shipment: ").append(value(shipmentJob.getShipmentId()))
                    .append(" | Labels: ").append(labels)
                    .append(" | Full: ").append(full)
                    .append(" | Partial: ").append(partial)
                    .append('\n')
                    .append("  Ship To: ").append(value(shipmentJob.getShipment().getShipToName()))
                    .append('\n');
        }
        section.append("Stop Totals -> Full: ").append(stopFull)
                .append(" | Partial: ").append(stopPartial)
                .append(" | Labels Needed: ").append(stopLabels)
                .append('\n');
        details.setText(section.toString());

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

    private void confirmAndPrint() {
        if (isCarrierMoveMode() && preparedCarrierJob == null) {
            showError("Run Preview first.");
            return;
        }
        if (!isCarrierMoveMode() && preparedJob == null) {
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
            int plannedLabels = isCarrierMoveMode()
                    ? countCarrierMoveLabels(preparedCarrierJob)
                    : preparedJob.getLpnsForLabels().size();
            int plannedInfoTags = isCarrierMoveMode()
                    ? preparedCarrierJob.getTotalStops() + 1
                    : 1;
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    isCarrierMoveMode()
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
                if (isCarrierMoveMode()) {
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

    private int countCarrierMoveLabels(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        int total = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                total += shipmentJob.getLpnsForLabels().size();
            }
        }
        return total;
    }

    private int sumUnits(List<LabelWorkflowService.SkuMathRow> rows) {
        int total = 0;
        for (LabelWorkflowService.SkuMathRow row : rows) {
            total += row.getUnits();
        }
        return total;
    }

    private int sumFullPallets(List<LabelWorkflowService.SkuMathRow> rows) {
        int total = 0;
        for (LabelWorkflowService.SkuMathRow row : rows) {
            total += row.getFullPallets();
        }
        return total;
    }

    private int sumPartialPallets(List<LabelWorkflowService.SkuMathRow> rows) {
        int total = 0;
        for (LabelWorkflowService.SkuMathRow row : rows) {
            total += row.getPartialPallets();
        }
        return total;
    }

    private int sumEstimatedPallets(List<LabelWorkflowService.SkuMathRow> rows) {
        int total = 0;
        for (LabelWorkflowService.SkuMathRow row : rows) {
            total += row.getEstimatedPallets();
        }
        return total;
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
                        parseQueueInput(inputArea.getText(), defaultType.getSelectedIndex() == 0
                                ? AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE
                                : AdvancedPrintWorkflowService.QueueItemType.SHIPMENT);
                prepared[0] = advancedService.prepareQueue(requests);
                StringBuilder preview = new StringBuilder();
                int totalLabels = 0;
                int totalInfoTags = 0;
                preview.append("Queue Items: ").append(prepared[0].getItems().size()).append('\n');
                for (AdvancedPrintWorkflowService.PreparedQueueItem item : prepared[0].getItems()) {
                    if (item.getType() == AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE) {
                        int labels = countCarrierMoveLabels(item.getCarrierMoveJob());
                        int info = item.getCarrierMoveJob().getTotalStops() + 1;
                        totalLabels += labels;
                        totalInfoTags += info;
                        preview.append(" - C:").append(item.getSourceId())
                                .append(" | stops=").append(item.getCarrierMoveJob().getTotalStops())
                                .append(" | labels=").append(labels)
                                .append(" | infoTags=").append(info)
                                .append('\n');
                    } else {
                        int labels = item.getShipmentJob().getLpnsForLabels().size();
                        totalLabels += labels;
                        totalInfoTags += 1;
                        preview.append(" - S:").append(item.getSourceId())
                                .append(" | labels=").append(labels)
                                .append(" | infoTags=1")
                                .append('\n');
                    }
                }
                preview.append('\n')
                        .append("Total labels: ").append(totalLabels).append('\n')
                        .append("Total info tags: ").append(totalInfoTags).append('\n');
                previewArea.setText(preview.toString());
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

    private List<AdvancedPrintWorkflowService.QueueRequestItem> parseQueueInput(String text,
                                                                                 AdvancedPrintWorkflowService.QueueItemType defaultType) {
        List<AdvancedPrintWorkflowService.QueueRequestItem> requests = new ArrayList<>();
        String[] lines = (text == null ? "" : text).split("\\r?\\n");
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            AdvancedPrintWorkflowService.QueueItemType type = defaultType;
            String id = line;
            if (line.length() > 2 && line.charAt(1) == ':') {
                char prefix = Character.toUpperCase(line.charAt(0));
                if (prefix == 'C') {
                    type = AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE;
                    id = line.substring(2).trim();
                } else if (prefix == 'S') {
                    type = AdvancedPrintWorkflowService.QueueItemType.SHIPMENT;
                    id = line.substring(2).trim();
                }
            }
            if (!id.isBlank()) {
                requests.add(new AdvancedPrintWorkflowService.QueueRequestItem(type, id));
            }
        }
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Queue input is empty.");
        }
        return requests;
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
        JTextField outputDir = new JTextField(defaultPrintToFileOutputDir().toString());
        outputDir.setColumns(40);
        installTerminalLikeMouseClipboardBehavior(dataField, outputDir);
        JComboBox<LabelWorkflowService.PrinterOption> printerSelect = new JComboBox<>();
        printerSelect.setModel(buildPrintTargetModel(true));

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
        JLabel outputDirLabel = addFormRow(form, gbc, row++, "Output Dir", outputDir);
        addFormRow(form, gbc, row++, "Printer", printerSelect);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        form.add(humanReadable, gbc);

        Runnable syncOutputState = () -> {
            LabelWorkflowService.PrinterOption selected = (LabelWorkflowService.PrinterOption) printerSelect.getSelectedItem();
            boolean printToFile = isPrintToFileSelected(selected);
            outputDir.setEnabled(printToFile);
            outputDir.setEditable(printToFile);
            outputDirLabel.setEnabled(printToFile);
        };
        printerSelect.addActionListener(e -> syncOutputState.run());
        syncOutputState.run();

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
            LabelWorkflowService.PrinterOption printer = (LabelWorkflowService.PrinterOption) printerSelect.getSelectedItem();
            boolean printToFile = isPrintToFileSelected(printer);
            Path outputPath = resolveOutputPath(outputDir.getText(), data, printToFile);
            try {
                Files.createDirectories(outputPath.getParent());
                Files.writeString(outputPath, zpl);
            } catch (Exception ex) {
                showError("Failed to write ZPL file: " + ex.getMessage());
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
                showError("Select a print target.");
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

    private Path resolveOutputPath(String outputDir, String data, boolean printToFileSelected) {
        String dir;
        if (printToFileSelected) {
            dir = (outputDir == null || outputDir.isBlank())
                    ? defaultPrintToFileOutputDir().toString()
                    : outputDir.trim();
        } else {
            dir = defaultPrintToFileOutputDir().toString();
        }
        Path outputPath = Paths.get(dir);
        String fileName = String.format("barcode-%s-%s.zpl",
                OUTPUT_TS.format(LocalDateTime.now()),
                safeSlug(data));
        return outputPath.resolve(fileName);
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
        String slug = NON_ALNUM_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        if (slug.isEmpty()) {
            return "data";
        }
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }

    private void installTerminalLikeMouseClipboardBehavior(JTextComponent... fields) {
        for (JTextComponent field : fields) {
            if (field == null) {
                continue;
            }
            field.setComponentPopupMenu(null);
            field.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleRightClickClipboardAction(e, field);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleRightClickClipboardAction(e, field);
                }
            });
        }
    }

    private void handleRightClickClipboardAction(MouseEvent event, JTextComponent field) {
        if (!(event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event))) {
            return;
        }

        String selection = field.getSelectedText();
        if (selection != null && !selection.isEmpty()) {
            field.copy();
            event.consume();
            return;
        }

        int position = field.viewToModel2D(event.getPoint());
        if (position >= 0) {
            field.setCaretPosition(position);
        }
        field.paste();
        event.consume();
    }
}

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
import com.tbg.wms.core.OutDirectoryRetentionService;
import com.tbg.wms.core.RuntimePathResolver;
import com.tbg.wms.core.RuntimeSettings;
import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.update.ReleaseAssetSupport;
import com.tbg.wms.core.update.ReleaseCheckService;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
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
    private static final String PREF_PRINT_TO_FILE_DIR = "printToFile.defaultOutputDir";
    private static final int SHIPMENT_MIN_CHARS = 11;
    private static final int COMBO_WIDTH_REDUCTION_PX = 12;
    private static final DateTimeFormatter OUTPUT_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_QUEUE_ITEMS = 500;
    private static final int MAX_PREVIEW_STOPS = 250;
    private static final int MAX_PREVIEW_SHIPMENTS_PER_STOP = 250;
    private static final int MAX_PREVIEW_SKU_ROWS_PER_SHIPMENT = 1000;
    private static final Icon UPDATE_AVAILABLE_ICON = new AlertBadgeIcon(16);
    private static final String[] VERSION_RESOURCE_PATHS = {
            "/META-INF/maven/com.tbg.wms/gui/pom.properties",
            "/META-INF/maven/com.tbg.wms/cli/pom.properties",
            "/META-INF/maven/com.tbg.wms/wms-pallet-tag-system/pom.properties"
    };
    private final JLabel inputLabel = new JLabel("Carrier Move ID:");
    private final JTextField shipmentField = new JTextField(24);
    private final JRadioButton carrierMoveModeButton = new JRadioButton("Carrier Move ID", true);
    private final JRadioButton shipmentModeButton = new JRadioButton("Shipment ID");
    private final JComboBox<LabelWorkflowService.PrinterOption> printerCombo = new JComboBox<>();
    private final JButton previewButton = new JButton("Preview");
    private final JButton clearButton = new JButton("Clear");
    private final JButton printButton = new JButton("Confirm Print");
    private final JButton toolsButton = new JButton("Tools");
    private final JButton labelSelectionToggleButton = new JButton("Deselect All");
    private final JToggleButton labelSelectionCollapseButton = new JToggleButton("Label Selection [collapsed]", false);
    private final JCheckBox includeInfoTagsCheckBox = new JCheckBox("Include info tags", true);
    private final JTextArea shipmentArea = new JTextArea();
    private final JPanel shipmentPreviewPanel = new JPanel();
    private final JPanel labelSelectionPanel = new JPanel();
    private final JPanel labelSelectionContentPanel = new JPanel(new BorderLayout(0, 6));
    private final JTextArea mathArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JLabel labelSelectionStatusLabel = new JLabel(" ");
    private final JLabel versionLabel = new JLabel();
    private final transient Preferences preferences = Preferences.userNodeForPackage(LabelGuiFrame.class);
    private final transient TextFieldClipboardController clipboardController = new TextFieldClipboardController();
    private final transient RuntimeSettings runtimeSettings = new RuntimeSettings();

    private final transient AppConfig config = new AppConfig();
    private final transient LabelWorkflowService service = new LabelWorkflowService(config);
    private final transient AdvancedPrintWorkflowService advancedService = new AdvancedPrintWorkflowService(config);
    private final transient LabelPreviewFormatter previewFormatter = new LabelPreviewFormatter();
    private final transient BarcodeDialogFactory barcodeDialogFactory = new BarcodeDialogFactory(new BarcodeDependencies());
    private final transient ReleaseCheckService releaseCheckService = new ReleaseCheckService();
    private final transient InstallMaintenanceService installMaintenanceService = new InstallMaintenanceService();
    private final transient GuidedUpdateService guidedUpdateService = new GuidedUpdateService();
    private transient List<LabelWorkflowService.PrinterOption> loadedPrinters = List.of();
    private transient List<JCheckBox> previewLabelCheckboxes = List.of();
    private transient List<PreviewLabelOption> previewLabelOptions = List.of();
    private transient LabelWorkflowService.PreparedJob preparedJob;
    private transient AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob;
    private transient ReleaseCheckService.ReleaseInfo latestReleaseInfo;
    private transient boolean updateCheckInProgress;

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
        versionLabel.setText("Version " + resolveVersionTag());
        updateInputModeUi();
        autoResumeIfFound();
        applyTopRowSizing();
        loadPrintersAsync();
        new OutDirectoryRetentionService().pruneDefaultOutDirectory(LabelGuiFrame.class);
        checkForUpdatesAsync(false);
        printButton.setEnabled(false);
    }

    private static String buildWindowTitle() {
        String version = resolveVersionTag();
        return version.isBlank()
                ? "WMS Pallet Tag System"
                : "WMS Pallet Tag System - " + version;
    }

    private static String resolveVersionTag() {
        Package pkg = LabelGuiFrame.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        if (version == null || version.isBlank()) {
            version = System.getProperty("wms.tags.version", "");
        }
        if (version == null || version.isBlank()) {
            version = resolveVersionFromPomProperties();
        }
        return version.trim();
    }

    private static String resolveVersionFromPomProperties() {
        for (String path : VERSION_RESOURCE_PATHS) {
            try (InputStream in = LabelGuiFrame.class.getResourceAsStream(path)) {
                if (in == null) {
                    continue;
                }
                Properties properties = new Properties();
                properties.load(in);
                String version = properties.getProperty("version", "").trim();
                if (!version.isEmpty()) {
                    return version;
                }
            } catch (Exception ignored) {
                // Fall through to next candidate resource.
            }
        }
        return "";
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

    private static boolean isPrintToFileSelected(LabelWorkflowService.PrinterOption selected) {
        return GuiPrinterTargetSupport.isPrintToFile(selected);
    }

    private JComponent buildToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolsButton.setFocusable(false);
        toolsButton.setHorizontalTextPosition(SwingConstants.LEFT);
        toolBar.add(toolsButton);

        JPopupMenu toolsMenu = new JPopupMenu();
        JMenuItem railLabelsItem = new JMenuItem("Rail Labels...");
        railLabelsItem.addActionListener(e -> openRailLabelsDialog());
        toolsMenu.add(railLabelsItem);
        JMenuItem queueItem = new JMenuItem("Queue Print...");
        queueItem.addActionListener(e -> openQueueDialog());
        toolsMenu.add(queueItem);
        JMenuItem barcodeItem = new JMenuItem("Barcode Generator...");
        barcodeItem.addActionListener(e -> openBarcodeDialog());
        toolsMenu.add(barcodeItem);
        toolsMenu.addSeparator();
        JMenuItem resumeItem = new JMenuItem("Resume Incomplete Job...");
        resumeItem.addActionListener(e -> openResumeDialog());
        toolsMenu.add(resumeItem);
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(e -> openSettingsDialog());
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
        versionLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 12));
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(versionLabel, BorderLayout.EAST);
        return panel;
    }

    private void wireActions() {
        previewButton.addActionListener(e -> previewJob());
        clearButton.addActionListener(e -> clearForm());
        printButton.addActionListener(e -> confirmAndPrint());
        labelSelectionToggleButton.addActionListener(e -> togglePreviewLabelSelection());
        labelSelectionCollapseButton.addActionListener(e -> updateLabelSelectionCollapseUi());
        includeInfoTagsCheckBox.addActionListener(e -> updatePreviewSelectionUi());
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
                    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = buildMainPrintTargetModel(true);
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
        resetPreviewLabelSelection();
        shipmentPreviewPanel.removeAll();
        shipmentPreviewPanel.add(shipmentArea);
        shipmentPreviewPanel.add(buildLabelSelectionPanel(buildShipmentLabelOptions(job)));
        updatePreviewSelectionUi();
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
    }

    private void renderCarrierMovePreview(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        resetPreviewLabelSelection();
        shipmentPreviewPanel.removeAll();
        shipmentPreviewPanel.add(shipmentArea);
        shipmentPreviewPanel.add(buildLabelSelectionPanel(buildCarrierMoveLabelOptions(job)));

        addStopPreviewSections(job);
        updatePreviewSelectionUi();
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

    private JComponent buildLabelSelectionPanel(List<PreviewLabelOption> options) {
        Objects.requireNonNull(options, "options cannot be null");
        previewLabelOptions = List.copyOf(options);
        previewLabelCheckboxes = new ArrayList<>(options.size());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(labelSelectionToggleButton);
        controls.add(includeInfoTagsCheckBox);
        controls.add(labelSelectionStatusLabel);

        JPanel checkboxGrid = new JPanel(new GridLayout(0, 3, 8, 4));
        for (PreviewLabelOption option : options) {
            JCheckBox checkbox = new JCheckBox(option.labelText(), true);
            checkbox.addActionListener(e -> updatePreviewSelectionUi());
            previewLabelCheckboxes.add(checkbox);
            checkboxGrid.add(checkbox);
        }

        labelSelectionPanel.removeAll();
        labelSelectionPanel.setLayout(new BorderLayout(0, 6));
        labelSelectionPanel.setBorder(BorderFactory.createEtchedBorder());
        labelSelectionCollapseButton.setFocusPainted(false);
        labelSelectionCollapseButton.setHorizontalAlignment(SwingConstants.LEFT);
        labelSelectionPanel.add(labelSelectionCollapseButton, BorderLayout.NORTH);
        labelSelectionContentPanel.removeAll();
        labelSelectionContentPanel.add(controls, BorderLayout.NORTH);
        labelSelectionContentPanel.add(checkboxGrid, BorderLayout.CENTER);
        labelSelectionPanel.add(labelSelectionContentPanel, BorderLayout.CENTER);
        labelSelectionCollapseButton.setSelected(false);
        updateLabelSelectionCollapseUi();
        return labelSelectionPanel;
    }

    private List<PreviewLabelOption> buildShipmentLabelOptions(LabelWorkflowService.PreparedJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        List<PreviewLabelOption> options = new ArrayList<>(job.getLpnsForLabels().size());
        int index = 1;
        for (Lpn lpn : job.getLpnsForLabels()) {
            options.add(new PreviewLabelOption(buildShipmentLabelOptionText(index, lpn), lpn, null));
            index++;
        }
        return options;
    }

    private List<PreviewLabelOption> buildCarrierMoveLabelOptions(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        List<PreviewLabelOption> options = new ArrayList<>();
        int index = 1;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                for (Lpn lpn : shipmentJob.getLpnsForLabels()) {
                    String labelText = buildCarrierMoveLabelOptionText(index, stop, shipmentJob, lpn);
                    LabelSelectionRef selection = LabelSelectionRef.forCarrierMove(
                            index,
                                    shipmentJob.getShipmentId(),
                                    resolveLpnId(lpn),
                                    stop.getStopPosition()
                    );
                    options.add(new PreviewLabelOption(labelText, lpn, selection));
                    index++;
                }
            }
        }
        return options;
    }

    private String buildShipmentLabelOptionText(int index, Lpn lpn) {
        return String.format("%02d. %s", index, resolveLpnId(lpn));
    }

    private String buildCarrierMoveLabelOptionText(
            int index,
            AdvancedPrintWorkflowService.PreparedStopGroup stop,
            LabelWorkflowService.PreparedJob shipmentJob,
            Lpn lpn
    ) {
        return String.format(
                "%02d. Stop %02d | Shipment %s | %s",
                index,
                stop.getStopPosition(),
                shipmentJob.getShipmentId(),
                resolveLpnId(lpn)
        );
    }

    private String resolveLpnId(Lpn lpn) {
        String lpnId = lpn == null || lpn.getLpnId() == null || lpn.getLpnId().isBlank() ? "UNKNOWN" : lpn.getLpnId();
        return lpnId;
    }

    private void togglePreviewLabelSelection() {
        boolean selectAll = previewLabelCheckboxes.stream().anyMatch(box -> !box.isSelected());
        for (JCheckBox checkbox : previewLabelCheckboxes) {
            checkbox.setSelected(selectAll);
        }
        updatePreviewSelectionUi();
    }

    private void updatePreviewSelectionUi() {
        int total = previewLabelCheckboxes.size();
        int selected = getSelectedPreviewOptions().size();
        int infoTags = currentInfoTagCount();
        int totalDocuments = selected + infoTags;
        labelSelectionStatusLabel.setText("Selected " + selected + " of " + total + " labels | Info Tags " + infoTags
                + " | Total Documents " + totalDocuments);
        labelSelectionToggleButton.setText(selected == total ? "Deselect All" : "Select All");
        refreshPreviewText();
        updatePrintButtonEnabled();
    }

    private void updateLabelSelectionCollapseUi() {
        boolean expanded = labelSelectionCollapseButton.isSelected();
        labelSelectionCollapseButton.setText(expanded
                ? "Label Selection [expanded]"
                : "Label Selection [collapsed]");
        labelSelectionContentPanel.setVisible(expanded);
        labelSelectionPanel.revalidate();
        labelSelectionPanel.repaint();
    }

    private void refreshPreviewText() {
        if (preparedCarrierJob != null && isCarrierMoveMode()) {
            List<LabelSelectionRef> selectedLabels = getSelectedCarrierMoveLabels();
            shipmentArea.setText(buildCarrierMoveSummaryText(preparedCarrierJob, selectedLabels));
            mathArea.setText(previewFormatter.buildCarrierMoveMathText(
                    preparedCarrierJob,
                    MAX_PREVIEW_STOPS,
                    MAX_PREVIEW_SHIPMENTS_PER_STOP,
                    selectedLabels.size(),
                    currentInfoTagCount()
            ));
            return;
        }
        if (preparedJob != null && !isCarrierMoveMode()) {
            List<Lpn> selectedLpns = getSelectedShipmentLpns();
            shipmentArea.setText(previewFormatter.buildShipmentSummaryText(
                    preparedJob,
                    selectedLpns,
                    currentInfoTagCount()
            ));
            mathArea.setText(previewFormatter.buildShipmentMathText(
                    preparedJob,
                    MAX_PREVIEW_SKU_ROWS_PER_SHIPMENT,
                    selectedLpns.size(),
                    currentInfoTagCount()
            ));
        }
    }

    private String buildCarrierMoveSummaryText(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            List<LabelSelectionRef> selectedLabels
    ) {
        StringBuilder summary = new StringBuilder(previewFormatter.buildCarrierMoveSummary(
                job,
                selectedLabels.size(),
                currentInfoTagCount()
        ));
        int shownStops = Math.min(job.getStopGroups().size(), MAX_PREVIEW_STOPS);
        if (job.getStopGroups().size() > shownStops) {
            summary.append("Preview Notice: Showing first ").append(MAX_PREVIEW_STOPS)
                    .append(" stops of ").append(job.getStopGroups().size()).append(".\n");
        }
        return summary.toString();
    }

    private List<PreviewLabelOption> getSelectedPreviewOptions() {
        if (previewLabelCheckboxes.isEmpty() || previewLabelOptions.isEmpty()) {
            return List.of();
        }
        List<PreviewLabelOption> selected = new ArrayList<>();
        for (int i = 0; i < previewLabelCheckboxes.size() && i < previewLabelOptions.size(); i++) {
            if (previewLabelCheckboxes.get(i).isSelected()) {
                selected.add(previewLabelOptions.get(i));
            }
        }
        return selected;
    }

    private List<Lpn> getSelectedShipmentLpns() {
        if (preparedJob == null) {
            return List.of();
        }
        List<Lpn> selected = new ArrayList<>();
        for (PreviewLabelOption option : getSelectedPreviewOptions()) {
            if (option.lpn() != null) {
                selected.add(option.lpn());
            }
        }
        return selected;
    }

    private List<LabelSelectionRef> getSelectedCarrierMoveLabels() {
        if (preparedCarrierJob == null) {
            return List.of();
        }
        List<LabelSelectionRef> selected = new ArrayList<>();
        for (PreviewLabelOption option : getSelectedPreviewOptions()) {
            if (option.carrierMoveSelection() != null) {
                selected.add(option.carrierMoveSelection());
            }
        }
        return selected;
    }

    private int countSelectedCarrierMoveStops(List<LabelSelectionRef> selectedLabels) {
        return (int) selectedLabels.stream()
                .map(LabelSelectionRef::getStopPosition)
                .distinct()
                .count();
    }

    private int currentInfoTagCount() {
        if (!includeInfoTagsCheckBox.isSelected()) {
            return 0;
        }
        if (preparedCarrierJob != null && isCarrierMoveMode()) {
            return AdvancedPrintWorkflowService.countCarrierMoveInfoTags(getSelectedCarrierMoveLabels(), true);
        }
        if (preparedJob != null && !isCarrierMoveMode()) {
            return AdvancedPrintWorkflowService.countShipmentInfoTags(getSelectedShipmentLpns().size(), true);
        }
        return 0;
    }

    private void resetPreviewLabelSelection() {
        previewLabelCheckboxes = List.of();
        previewLabelOptions = List.of();
        labelSelectionPanel.removeAll();
        labelSelectionContentPanel.removeAll();
        labelSelectionStatusLabel.setText(" ");
        labelSelectionToggleButton.setText("Deselect All");
        labelSelectionCollapseButton.setSelected(false);
        labelSelectionCollapseButton.setText("Label Selection [collapsed]");
        includeInfoTagsCheckBox.setSelected(true);
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

        List<Lpn> selectedShipmentLpns = carrierMoveMode ? List.of() : getSelectedShipmentLpns();
        List<LabelSelectionRef> selectedCarrierLabels =
                carrierMoveMode ? getSelectedCarrierMoveLabels() : List.of();
        if ((!carrierMoveMode && selectedShipmentLpns.isEmpty())
                || (carrierMoveMode && selectedCarrierLabels.isEmpty())) {
            showError("Select at least one label to print.");
            return;
        }

        if (!printToFile) {
            int plannedLabels = carrierMoveMode
                    ? selectedCarrierLabels.size()
                    : selectedShipmentLpns.size();
            int plannedInfoTags = currentInfoTagCount();
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    carrierMoveMode
                            ? "Print " + plannedLabels + " labels + " + plannedInfoTags + " info tags to " + selected + "?"
                            : "Print " + plannedLabels + " labels + " + plannedInfoTags + " info tags to " + selected + "?",
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
                String printerId = printToFile ? null : selected.getId();
                if (carrierMoveMode) {
                    Path outDir = defaultPrintToFileOutputDir().resolve("gui-cmid-" + preparedCarrierJob.getCarrierMoveId() + "-" +
                            OUTPUT_TS.format(LocalDateTime.now()));
                    return advancedService.printCarrierMoveJob(
                            preparedCarrierJob,
                            selectedCarrierLabels,
                            printerId,
                            outDir,
                            printToFile,
                            includeInfoTagsCheckBox.isSelected()
                    );
                }
                Path outDir = defaultPrintToFileOutputDir().resolve("gui-" + preparedJob.getShipmentId() + "-" +
                        OUTPUT_TS.format(LocalDateTime.now()));
                return advancedService.printShipmentJob(
                        preparedJob,
                        selectedShipmentLpns,
                        printerId,
                        outDir,
                        printToFile,
                        includeInfoTagsCheckBox.isSelected()
                );
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
        updatePrintButtonEnabled();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void checkForUpdatesAsync(boolean userInitiated) {
        checkForUpdatesAsync(userInitiated, null);
    }

    private void checkForUpdatesAsync(boolean userInitiated, JLabel statusOutput) {
        if (updateCheckInProgress) {
            if (userInitiated && statusOutput != null) {
                statusOutput.setText("Update check already in progress...");
            }
            return;
        }
        updateCheckInProgress = true;
        if (statusOutput != null) {
            statusOutput.setText("Checking for updates...");
        }

        SwingWorker<ReleaseCheckService.ReleaseInfo, Void> worker = new SwingWorker<>() {
            @Override
            protected ReleaseCheckService.ReleaseInfo doInBackground() throws Exception {
                return releaseCheckService.checkLatestRelease(resolveVersionTag());
            }

            @Override
            protected void done() {
                updateCheckInProgress = false;
                try {
                    latestReleaseInfo = get();
                    refreshUpdateAvailabilityUi();
                    if (statusOutput != null) {
                        statusOutput.setText(formatUpdateStatus());
                    }
                    if (userInitiated) {
                        showUpdatePrompt(latestReleaseInfo);
                    }
                } catch (Exception ex) {
                    if (statusOutput != null) {
                        statusOutput.setText("Update check failed.");
                    }
                    if (userInitiated) {
                        showError("Update check failed: " + rootMessage(ex));
                    }
                }
            }
        };
        worker.execute();
    }

    private String formatUpdateStatus() {
        if (latestReleaseInfo == null) {
            return "No update check has completed yet.";
        }
        if (latestReleaseInfo.updateAvailable()) {
            boolean guidedReady = latestReleaseInfo.preferredInstallerAsset() != null
                    && ReleaseAssetSupport.findChecksumAsset(
                    latestReleaseInfo.assets(),
                    latestReleaseInfo.preferredInstallerAsset()) != null;
            return !guidedReady
                    ? "Update available: " + latestReleaseInfo.latestVersion()
                    : "Update available: " + latestReleaseInfo.latestVersion() + " (guided install ready)";
        }
        return "Up to date on " + latestReleaseInfo.currentVersion() + ".";
    }

    private void refreshUpdateAvailabilityUi() {
        boolean updateAvailable = latestReleaseInfo != null && latestReleaseInfo.updateAvailable();
        toolsButton.setIcon(updateAvailable ? UPDATE_AVAILABLE_ICON : null);
        toolsButton.setToolTipText(updateAvailable
                ? "Update available: " + latestReleaseInfo.latestVersion()
                : null);
    }

    private void showUpdatePrompt(ReleaseCheckService.ReleaseInfo releaseInfo) {
        if (!releaseInfo.updateAvailable()) {
            JOptionPane.showMessageDialog(
                    this,
                    "You are up to date on version " + releaseInfo.currentVersion() + ".",
                    "Updates",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        ReleaseCheckService.ReleaseAsset installerAsset = releaseInfo.preferredInstallerAsset();
        boolean guidedReady = installerAsset != null
                && ReleaseAssetSupport.findChecksumAsset(releaseInfo.assets(), installerAsset) != null;
        Object[] options = !guidedReady
                ? new Object[]{"Open Download Page", "Close"}
                : new Object[]{"Download and Install", "Open Download Page", "Close"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Current version: " + releaseInfo.currentVersion()
                        + "\nLatest version: " + releaseInfo.latestVersion()
                        + (!guidedReady
                        ? "\n\nA verified guided upgrade is unavailable because the published release does not include both the installer and its checksum. Open the latest release page now?"
                        : "\n\nA verified packaged installer is available. Download it and start the upgrade now?"),
                "Update Available",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (guidedReady && choice == 0) {
            performGuidedUpgrade(releaseInfo);
            return;
        }
        if ((!guidedReady && choice == 0) || (guidedReady && choice == 1)) {
            openReleaseUrl(releaseInfo.releaseUrl());
        }
    }

    private void performGuidedUpgrade(ReleaseCheckService.ReleaseInfo releaseInfo) {
        Path installScript = installMaintenanceService.findInstallScript(LabelGuiFrame.class).orElse(null);
        if (installScript == null) {
            openReleaseUrl(releaseInfo.releaseUrl());
            return;
        }

        setBusy("Downloading update " + releaseInfo.latestVersion() + "...");
        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return guidedUpdateService.downloadInstaller(LabelGuiFrame.class, releaseInfo);
            }

            @Override
            protected void done() {
                try {
                    Path installerPath = get();
                    installMaintenanceService.launchInstaller(installScript, installerPath);
                    dispose();
                    System.exit(0);
                } catch (Exception ex) {
                    setReady("Update download failed.");
                    showError("Could not start guided update: " + rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void openReleaseUrl(String releaseUrl) {
        if (releaseUrl == null || releaseUrl.isBlank()) {
            showError("Release download URL is unavailable.");
            return;
        }
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                showError("Desktop browser launch is not supported on this machine.");
                return;
            }
            Desktop.getDesktop().browse(java.net.URI.create(releaseUrl));
        } catch (Exception ex) {
            showError("Could not open release page: " + rootMessage(ex));
        }
    }

    private void openUninstallDialog() {
        Path uninstallScript = installMaintenanceService.findUninstallScript(LabelGuiFrame.class).orElse(null);
        if (uninstallScript == null) {
            showError("Uninstall script not found in the packaged app or repo scripts directory.");
            return;
        }

        Object[] options = {"Cancel", "Uninstall Only", "Clean Wipe"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose uninstall mode:\n"
                        + "- Uninstall Only removes the installed product.\n"
                        + "- Clean Wipe also removes the install directory and runtime settings to prepare for a clean reinstall.\n\n"
                        + "The app will close after launch so the uninstall can complete.",
                "Uninstall / Clean Install Prep",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice != 1 && choice != 2) {
            return;
        }

        try {
            installMaintenanceService.launchUninstall(uninstallScript, choice == 2);
            dispose();
            System.exit(0);
        } catch (Exception ex) {
            showError("Failed to launch uninstall: " + rootMessage(ex));
        }
    }

    private void clearForm() {
        shipmentField.setText("");
        shipmentArea.setText("");
        resetPreviewLabelSelection();
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
        resetPreviewLabelSelection();
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

    private void updatePrintButtonEnabled() {
        if (preparedCarrierJob != null && isCarrierMoveMode()) {
            printButton.setEnabled(!getSelectedCarrierMoveLabels().isEmpty());
            return;
        }
        printButton.setEnabled(preparedJob != null && !getSelectedShipmentLpns().isEmpty() && !isCarrierMoveMode());
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

    private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildMainPrintTargetModel(boolean includeFileOption) {
        return buildPrintTargetModel(GuiPrinterTargetSupport.filterLabelScreenPrinters(loadedPrinters), includeFileOption);
    }

    private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(boolean includeFileOption) {
        return buildPrintTargetModel(loadedPrinters, includeFileOption);
    }

    private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(
            List<LabelWorkflowService.PrinterOption> printerOptions,
            boolean includeFileOption
    ) {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
        for (LabelWorkflowService.PrinterOption option : printerOptions) {
            model.addElement(option);
        }
        if (includeFileOption) {
            model.addElement(GuiPrinterTargetSupport.buildPrintToFileOption(defaultPrintToFileOutputDir()));
        }
        return model;
    }

    private Path defaultPrintToFileOutputDir() {
        String configured = preferences.get(PREF_PRINT_TO_FILE_DIR, "");
        if (configured != null && !configured.isBlank()) {
            try {
                return Paths.get(configured.trim());
            } catch (java.nio.file.InvalidPathException ignored) {
                // Fallback to runtime-derived default if persisted value is invalid.
            }
        }
        return RuntimePathResolver.resolveJarSiblingDir(LabelGuiFrame.class, "out");
    }

    private void openSettingsDialog() {
        MainSettingsDialog dialog = new MainSettingsDialog(
                this,
                defaultPrintToFileOutputDir(),
                runtimeSettings.outRetentionDays(OutDirectoryRetentionService.DEFAULT_RETENTION_DAYS),
                formatUpdateStatus(),
                this::showError,
                this::installTerminalLikeMouseClipboardBehavior,
                this::saveMainSettings,
                retentionDays -> runOutDirectoryCleanup(this, retentionDays),
                statusLabel -> checkForUpdatesAsync(true, statusLabel),
                this::openUninstallDialog,
                () -> openAdvancedSettingsDialog(this)
        );
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

    private void saveMainSettings(Path configuredPath, int retentionDays) {
        preferences.put(PREF_PRINT_TO_FILE_DIR, configuredPath.toString());
        runtimeSettings.setOutRetentionDays(retentionDays);
        LabelWorkflowService.PrinterOption previousSelection =
                (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem();
        printerCombo.setModel(buildMainPrintTargetModel(true));
        applyTopRowSizing();
        restoreSelection(previousSelection);
        setReady("Settings saved.");
    }

    private void runOutDirectoryCleanup(Component owner, int retentionDays) {
        OutDirectoryRetentionService.CleanupResult result =
                new OutDirectoryRetentionService().pruneDefaultOutDirectory(LabelGuiFrame.class);
        JOptionPane.showMessageDialog(
                owner,
                "Cleanup complete.\nDeleted files: " + result.getDeletedFiles()
                        + "\nDeleted folders: " + result.getDeletedDirectories()
                        + "\nRetention days: " + retentionDays
                        + "\nRoot: " + result.getRootDir(),
                "Out Cleanup",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void openAdvancedSettingsDialog(Component owner) {
        AdvancedSettingsDialog dialog = new AdvancedSettingsDialog(
                this,
                config,
                this::reloadRuntimeConfigArtifacts,
                this::showError,
                this::installTerminalLikeMouseClipboardBehavior
        );
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void reloadRuntimeConfigArtifacts() {
        service.clearCaches();
        advancedService.clearCaches();
        loadedPrinters = List.of();
        loadPrintersAsync();
        setReady("Runtime config reloaded.");
    }

    private static final class PreviewLabelOption {
        private final String labelText;
        private final Lpn lpn;
        private final LabelSelectionRef carrierMoveSelection;

        private PreviewLabelOption(
                String labelText,
                Lpn lpn,
                LabelSelectionRef carrierMoveSelection
        ) {
            this.labelText = Objects.requireNonNull(labelText, "labelText");
            this.lpn = lpn;
            this.carrierMoveSelection = carrierMoveSelection;
        }

        private String labelText() {
            return labelText;
        }

        private Lpn lpn() {
            return lpn;
        }

        private LabelSelectionRef carrierMoveSelection() {
            return carrierMoveSelection;
        }
    }
}

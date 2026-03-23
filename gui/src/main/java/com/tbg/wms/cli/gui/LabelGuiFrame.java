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
import com.tbg.wms.core.RuntimeSettings;
import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.update.ReleaseCheckService;
import com.tbg.wms.core.update.VersionSupport;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.Serial;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    @Serial
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
    private final JButton showLabelsButton = new JButton("Show Labels");
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
    private final JLabel dbStatusLedLabel = new JLabel();
    private final JLabel dbStatusLabel = new JLabel();
    private final JLabel versionLabel = new JLabel();
    private final transient Preferences preferences = Preferences.userNodeForPackage(LabelGuiFrame.class);
    private final transient TextFieldClipboardController clipboardController = new TextFieldClipboardController();
    private final transient RuntimeSettings runtimeSettings = new RuntimeSettings();

    private final transient AppConfig config = new AppConfig();
    private final transient LabelWorkflowService service = new LabelWorkflowService(config);
    private final transient AdvancedPrintWorkflowService advancedService = new AdvancedPrintWorkflowService(config);
    private final transient LabelPreviewFormatter previewFormatter = new LabelPreviewFormatter();
    private final transient PreviewSelectionSupport previewSelectionSupport = new PreviewSelectionSupport();
    private final transient PreviewSelectionPanelSupport previewSelectionPanelSupport = new PreviewSelectionPanelSupport();
    private final transient PreviewRenderSupport previewRenderSupport =
            new PreviewRenderSupport(previewFormatter, MAX_PREVIEW_STOPS, MAX_PREVIEW_SHIPMENTS_PER_STOP, MAX_PREVIEW_SKU_ROWS_PER_SHIPMENT);
    private final transient GuiPreviewDisplaySupport previewDisplaySupport =
            new GuiPreviewDisplaySupport(previewFormatter, MAX_PREVIEW_STOPS, MAX_PREVIEW_SHIPMENTS_PER_STOP, MAX_PREVIEW_SKU_ROWS_PER_SHIPMENT);
    private final transient GuiPreviewExecutionSupport previewExecutionSupport = new GuiPreviewExecutionSupport();
    private final transient GuiPrintFlowSupport printFlowSupport = new GuiPrintFlowSupport();
    private final transient GuiPrintExecutionSupport printExecutionSupport = new GuiPrintExecutionSupport(printFlowSupport);
    private final transient GuiPrinterSelectionSupport printerSelectionSupport = new GuiPrinterSelectionSupport();
    private final transient FramePrinterSelectionSupport framePrinterSelectionSupport = new FramePrinterSelectionSupport();
    private final transient GuiPreviewSelectionUiSupport previewSelectionUiSupport = new GuiPreviewSelectionUiSupport();
    private final transient LabelGuiFramePreviewShellSupport previewShellSupport = new LabelGuiFramePreviewShellSupport();
    private final transient LabelGuiFrameToolMenuSupport toolMenuSupport = new LabelGuiFrameToolMenuSupport();
    private final transient QueueResumeDialogSupport queueResumeDialogSupport =
            new QueueResumeDialogSupport(buildQueueResumeDependencies(), MAX_QUEUE_ITEMS);
    private final transient GuiSettingsDialogSupport settingsDialogSupport =
            new GuiSettingsDialogSupport(buildSettingsDependencies(), PREF_PRINT_TO_FILE_DIR, LabelGuiFrame.class);
    private transient BarcodeDialogFactory barcodeDialogFactory;
    private final transient GuiUpdateFlowSupport updateFlowSupport = new GuiUpdateFlowSupport();
    private final transient GuiUpdateExecutionSupport updateExecutionSupport = new GuiUpdateExecutionSupport(updateFlowSupport);
    private final transient GuiDbStatusSupport dbStatusSupport = new GuiDbStatusSupport();
    private final transient GuiZplPreviewSupport zplPreviewSupport = new GuiZplPreviewSupport();
    private final transient ReleaseCheckService releaseCheckService = new ReleaseCheckService();
    private final transient InstallMaintenanceService installMaintenanceService = new InstallMaintenanceService();
    private final transient GuidedUpdateService guidedUpdateService = new GuidedUpdateService();
    private transient List<LabelWorkflowService.PrinterOption> loadedPrinters = List.of();
    private transient List<JCheckBox> previewLabelCheckboxes = List.of();
    private transient List<PreviewSelectionSupport.LabelOption> previewLabelOptions = List.of();
    private transient LabelWorkflowService.PreparedJob preparedJob;
    private transient AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob;
    private transient ReleaseCheckService.ReleaseInfo latestReleaseInfo;
    private transient boolean updateCheckInProgress;
    private transient ZplPreviewToolDialog generatedLabelsPreviewDialog;

    public LabelGuiFrame() {
        super(buildWindowTitle());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImages(AppIconSupport.loadWindowIcons());
        setSize(1080, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(buildToolBar(), BorderLayout.NORTH);
        topContainer.add(buildTopPanel(), BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        this.barcodeDialogFactory = new BarcodeDialogFactory(buildBarcodeDependencies());

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
        refreshDbStatusAsync();
        printButton.setEnabled(false);
        showLabelsButton.setEnabled(false);
    }

    private static String buildWindowTitle() {
        String version = resolveVersionTag();
        return version.isBlank()
                ? "WMS Pallet Tag System"
                : "WMS Pallet Tag System - " + version;
    }

    private static String resolveVersionTag() {
        String version = VersionSupport.resolvePackageVersion(LabelGuiFrame.class);
        if (version.isBlank()) {
            version = System.getProperty("wms.tags.version", "").trim();
        }
        if (version.isBlank()) {
            version = resolveVersionFromPomProperties();
        }
        return version.trim();
    }

    private static String resolveVersionFromPomProperties() {
        return VersionSupport.readFirstNonBlankProperty(LabelGuiFrame.class, "version", VERSION_RESOURCE_PATHS);
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
        return toolMenuSupport.buildToolBar(toolsButton, buildToolMenuActions());
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
        panel.add(showLabelsButton, gbc);

        gbc.gridx = 8;
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
        dbStatusLabel.setFont(dbStatusLabel.getFont().deriveFont(dbStatusLabel.getFont().getSize2D() - 1f));
        versionLabel.setFont(versionLabel.getFont().deriveFont(versionLabel.getFont().getSize2D() - 1f));
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        dbPanel.setOpaque(false);
        dbStatusLedLabel.setVerticalAlignment(SwingConstants.CENTER);
        dbPanel.add(dbStatusLedLabel);
        dbPanel.add(dbStatusLabel);
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(dbPanel);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 12));
        rightPanel.add(versionLabel);
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);
        applyDbStatus(dbStatusSupport.checking(config.oracleService()));
        return panel;
    }

    private void wireActions() {
        previewButton.addActionListener(e -> previewJob());
        clearButton.addActionListener(e -> clearForm());
        showLabelsButton.addActionListener(e -> openGeneratedLabelPreview());
        printButton.addActionListener(e -> confirmAndPrint());
        labelSelectionToggleButton.addActionListener(e -> togglePreviewLabelSelection());
        labelSelectionCollapseButton.addActionListener(e -> updateLabelSelectionCollapseUi());
        includeInfoTagsCheckBox.addActionListener(e -> updatePreviewSelectionUi());
        carrierMoveModeButton.addActionListener(e -> updateInputModeUi());
        shipmentModeButton.addActionListener(e -> updateInputModeUi());
    }

    private void wireShortcuts() {
        WorkflowShortcutBinder.bindPreviewShortcut(getRootPane(), previewButton, "preview");
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
                    int selectionIndex = printerSelectionSupport.resolveSelectionIndex(
                            null,
                            framePrinterSelectionSupport.comboItems(model)
                    );
                    if (selectionIndex >= 0) {
                        printerCombo.setSelectedIndex(selectionIndex);
                    }
                    setReady(printerSelectionSupport.printerLoadStatusMessage(printerCount, model.getSize()));
                } catch (Exception ex) {
                    setReady("Failed to load printers.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void previewJob() {
        GuiPreviewExecutionSupport.PreviewRequest request;
        try {
            request = previewExecutionSupport.prepareRequest(shipmentField.getText(), isCarrierMoveMode());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        setBusy("Preparing preview...");
        printButton.setEnabled(false);
        preparedJob = null;
        preparedCarrierJob = null;

        SwingWorker<Object, Void> worker = new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                return previewExecutionSupport.execute(request, new GuiPreviewExecutionSupport.PreviewLoader() {
                    @Override
                    public LabelWorkflowService.PreparedJob prepareShipmentJob(String shipmentId) throws Exception {
                        return service.prepareJob(shipmentId);
                    }

                    @Override
                    public AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMoveJob(String carrierMoveId) throws Exception {
                        return advancedService.prepareCarrierMoveJob(carrierMoveId);
                    }
                });
            }

            @Override
            protected void done() {
                try {
                    GuiPreviewExecutionSupport.PreparedPreview prepared = (GuiPreviewExecutionSupport.PreparedPreview) get();
                    if (prepared.isCarrierMove()) {
                        preparedCarrierJob = prepared.carrierMoveJob();
                        renderCarrierMovePreview(preparedCarrierJob);
                    } else {
                        preparedJob = prepared.shipmentJob();
                        renderPreview(preparedJob);
                    }
                    applyDbStatus(dbStatusSupport.connected(config.oracleService()));
                    setReady(previewExecutionSupport.buildSuccessOutcome().statusMessage());
                } catch (Exception ex) {
                    dbStatusSupport.failure(config.oracleService(), ex).ifPresent(LabelGuiFrame.this::applyDbStatus);
                    GuiPreviewExecutionSupport.FailureOutcome outcome = previewExecutionSupport.buildFailureOutcome(ex);
                    setReady(outcome.statusMessage());
                    showError(outcome.errorMessage());
                }
            }
        };
        worker.execute();
    }

    private void renderPreview(LabelWorkflowService.PreparedJob job) {
        resetPreviewLabelSelection();
        previewRenderSupport.renderShipmentPreview(
                shipmentPreviewPanel,
                shipmentArea,
                buildLabelSelectionPanel(previewSelectionSupport.buildShipmentLabelOptions(job))
        );
        updatePreviewSelectionUi();
    }

    private void renderCarrierMovePreview(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        resetPreviewLabelSelection();
        previewRenderSupport.renderCarrierMovePreview(
                shipmentPreviewPanel,
                shipmentArea,
                buildLabelSelectionPanel(previewSelectionSupport.buildCarrierMoveLabelOptions(job)),
                job
        );
        updatePreviewSelectionUi();
    }

    private JComponent buildLabelSelectionPanel(List<PreviewSelectionSupport.LabelOption> options) {
        PreviewSelectionPanelSupport.PanelBuildResult result = previewSelectionPanelSupport.buildPanel(
                labelSelectionPanel,
                labelSelectionContentPanel,
                labelSelectionToggleButton,
                labelSelectionCollapseButton,
                includeInfoTagsCheckBox,
                labelSelectionStatusLabel,
                options,
                this::updatePreviewSelectionUi
        );
        previewLabelOptions = result.options();
        previewLabelCheckboxes = result.checkboxes();
        updateLabelSelectionCollapseUi();
        return labelSelectionPanel;
    }

    private void togglePreviewLabelSelection() {
        boolean selectAll = previewLabelCheckboxes.stream().anyMatch(box -> !box.isSelected());
        for (JCheckBox checkbox : previewLabelCheckboxes) {
            checkbox.setSelected(selectAll);
        }
        updatePreviewSelectionUi();
    }

    private void updatePreviewSelectionUi() {
        PreviewSelectionSupport.SelectionSnapshot selection = snapshotPreviewSelection();
        int total = previewLabelCheckboxes.size();
        int selected = selection.selectedLabelCount();
        int infoTags = selection.infoTagCount();
        int totalDocuments = selection.totalDocuments();
        labelSelectionStatusLabel.setText(previewSelectionUiSupport.selectionStatusText(selected, total, infoTags, totalDocuments));
        labelSelectionToggleButton.setText(previewSelectionUiSupport.selectionToggleText(selected, total));
        refreshPreviewText(selection);
        updatePrintButtonEnabled(selection);
        refreshGeneratedLabelPreviewDialog(selection);
    }

    private void updateLabelSelectionCollapseUi() {
        boolean expanded = labelSelectionCollapseButton.isSelected();
        labelSelectionCollapseButton.setText(previewSelectionUiSupport.collapseButtonText(expanded));
        labelSelectionContentPanel.setVisible(expanded);
        labelSelectionPanel.revalidate();
        labelSelectionPanel.repaint();
    }

    private void refreshPreviewText(PreviewSelectionSupport.SelectionSnapshot selection) {
        if (preparedCarrierJob != null && isCarrierMoveMode()) {
            GuiPreviewDisplaySupport.PreviewText previewText = previewDisplaySupport.buildCarrierMovePreview(
                    preparedCarrierJob,
                    selection.selectedCarrierLabels(),
                    selection.infoTagCount()
            );
            shipmentArea.setText(previewText.summaryText());
            mathArea.setText(previewText.mathText());
            return;
        }
        if (preparedJob != null && !isCarrierMoveMode()) {
            GuiPreviewDisplaySupport.PreviewText previewText = previewDisplaySupport.buildShipmentPreview(preparedJob, selection);
            shipmentArea.setText(previewText.summaryText());
            mathArea.setText(previewText.mathText());
        }
    }

    private int countSelectedCarrierMoveStops(List<LabelSelectionRef> selectedLabels) {
        return previewSelectionSupport.countSelectedCarrierMoveStops(selectedLabels);
    }

    private int currentInfoTagCount() {
        return snapshotPreviewSelection().infoTagCount();
    }

    private PreviewSelectionSupport.SelectionSnapshot snapshotPreviewSelection() {
        return previewSelectionSupport.snapshotSelection(
                previewLabelCheckboxes,
                previewLabelOptions,
                includeInfoTagsCheckBox.isSelected(),
                preparedCarrierJob != null && isCarrierMoveMode(),
                preparedJob != null && !isCarrierMoveMode()
        );
    }

    private void resetPreviewLabelSelection() {
        previewLabelCheckboxes = List.of();
        previewLabelOptions = List.of();
        previewSelectionPanelSupport.resetPanel(
                labelSelectionPanel,
                labelSelectionContentPanel,
                labelSelectionStatusLabel,
                labelSelectionToggleButton,
                labelSelectionCollapseButton,
                includeInfoTagsCheckBox
        );
        clearGeneratedLabelPreviewDialog();
    }

    private void confirmAndPrint() {
        GuiPrintExecutionSupport.PreparedExecution execution;
        try {
            execution = printExecutionSupport.prepareExecution(new GuiPrintExecutionSupport.PrintRequest(
                    isCarrierMoveMode(),
                    preparedJob,
                    preparedCarrierJob,
                    (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem(),
                    isPrintToFileSelected((LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem()),
                    snapshotPreviewSelection(),
                    includeInfoTagsCheckBox.isSelected(),
                    defaultPrintToFileOutputDir(),
                    OUTPUT_TS.format(LocalDateTime.now())
            ));
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        if (execution.requiresConfirmation()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    execution.confirmationMessage(),
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
                return printExecutionSupport.execute(execution, new GuiPrintExecutionSupport.PrintRunner() {
                    @Override
                    public AdvancedPrintWorkflowService.PrintResult printShipment(
                            LabelWorkflowService.PreparedJob preparedJob,
                            List<Lpn> selectedLpns,
                            String printerId,
                            Path outputDir,
                            boolean printToFile,
                            boolean includeInfoTags
                    ) throws Exception {
                        return advancedService.printShipmentJob(
                                preparedJob,
                                selectedLpns,
                                printerId,
                                outputDir,
                                printToFile,
                                includeInfoTags
                        );
                    }

                    @Override
                    public AdvancedPrintWorkflowService.PrintResult printCarrierMove(
                            AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                            List<LabelSelectionRef> selectedLabels,
                            String printerId,
                            Path outputDir,
                            boolean printToFile,
                            boolean includeInfoTags
                    ) throws Exception {
                        return advancedService.printCarrierMoveJob(
                                preparedCarrierJob,
                                selectedLabels,
                                printerId,
                                outputDir,
                                printToFile,
                                includeInfoTags
                        );
                    }
                });
            }

            @Override
            protected void done() {
                previewButton.setEnabled(true);
                printButton.setEnabled(true);
                clearButton.setEnabled(true);
                try {
                    AdvancedPrintWorkflowService.PrintResult result = get();
                    applyDbStatus(dbStatusSupport.connected(config.oracleService()));
                    GuiPrintExecutionSupport.CompletionOutcome outcome = printExecutionSupport.buildCompletionOutcome(result);
                    setReady(outcome.statusMessage());
                    JOptionPane.showMessageDialog(
                            LabelGuiFrame.this,
                            outcome.dialogMessage(),
                            "Print Complete",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    dbStatusSupport.failure(config.oracleService(), ex).ifPresent(LabelGuiFrame.this::applyDbStatus);
                    GuiPrintExecutionSupport.FailureOutcome outcome = printExecutionSupport.buildFailureOutcome(ex);
                    setReady(outcome.statusMessage());
                    showError(outcome.errorMessage());
                }
            }
        };
        worker.execute();
    }

    private void refreshDbStatusAsync() {
        applyDbStatus(dbStatusSupport.checking(config.oracleService()));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (com.tbg.wms.db.DbConnectionPool pool = new com.tbg.wms.db.DbConnectionPool(config)) {
                    pool.testConnectivity();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    applyDbStatus(dbStatusSupport.connected(config.oracleService()));
                } catch (Exception ex) {
                    dbStatusSupport.failure(config.oracleService(), ex).ifPresent(LabelGuiFrame.this::applyDbStatus);
                }
            }
        };
        worker.execute();
    }

    private void applyDbStatus(GuiDbStatusSupport.StatusState state) {
        dbStatusLedLabel.setIcon(state.icon());
        dbStatusLedLabel.setToolTipText(state.tooltip());
        dbStatusLabel.setText(state.text());
        dbStatusLabel.setToolTipText(state.tooltip());
    }

    private void setBusy(String message) {
        statusLabel.setText(message);
        previewButton.setEnabled(false);
        clearButton.setEnabled(false);
        showLabelsButton.setEnabled(false);
        printButton.setEnabled(false);
    }

    private void setReady(String message) {
        statusLabel.setText(message);
        previewButton.setEnabled(true);
        clearButton.setEnabled(true);
        updatePrintButtonEnabled(snapshotPreviewSelection());
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
                statusOutput.setText(updateExecutionSupport.alreadyInProgressMessage());
            }
            return;
        }
        updateCheckInProgress = true;
        if (statusOutput != null) {
            statusOutput.setText(updateExecutionSupport.checkingForUpdatesMessage());
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
                    GuiUpdateExecutionSupport.CheckCompletionOutcome outcome =
                            updateExecutionSupport.buildCheckCompletion(latestReleaseInfo);
                    refreshUpdateAvailabilityUi(outcome);
                    if (statusOutput != null) {
                        statusOutput.setText(outcome.statusMessage());
                    }
                    if (userInitiated) {
                        showUpdatePrompt(latestReleaseInfo);
                    }
                } catch (Exception ex) {
                    GuiUpdateExecutionSupport.FailureOutcome outcome = updateExecutionSupport.buildCheckFailure(ex);
                    if (statusOutput != null) {
                        statusOutput.setText(outcome.statusMessage());
                    }
                    if (userInitiated) {
                        showError(outcome.errorMessage());
                    }
                }
            }
        };
        worker.execute();
    }

    private String formatUpdateStatus() {
        return latestReleaseInfo == null
                ? updateFlowSupport.formatUpdateStatus(null)
                : updateExecutionSupport.buildCheckCompletion(latestReleaseInfo).statusMessage();
    }

    private void refreshUpdateAvailabilityUi(GuiUpdateExecutionSupport.CheckCompletionOutcome outcome) {
        toolsButton.setIcon(outcome.updateAvailable() ? UPDATE_AVAILABLE_ICON : null);
        toolsButton.setToolTipText(outcome.tooltip());
    }

    private void showUpdatePrompt(ReleaseCheckService.ReleaseInfo releaseInfo) {
        if (!releaseInfo.updateAvailable()) {
            JOptionPane.showMessageDialog(
                    this,
                    updateFlowSupport.updatePromptMessage(releaseInfo),
                    "Updates",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Object[] options = updateFlowSupport.updatePromptOptions(releaseInfo);
        int choice = JOptionPane.showOptionDialog(
                this,
                updateFlowSupport.updatePromptMessage(releaseInfo),
                "Update Available",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );
        GuiUpdateExecutionSupport.PromptAction action = updateExecutionSupport.resolvePromptAction(releaseInfo, choice);
        if (action == GuiUpdateExecutionSupport.PromptAction.GUIDED_UPGRADE) {
            performGuidedUpgrade(releaseInfo);
            return;
        }
        if (action == GuiUpdateExecutionSupport.PromptAction.OPEN_RELEASE_PAGE) {
            openReleaseUrl(releaseInfo.releaseUrl());
        }
    }

    private void performGuidedUpgrade(ReleaseCheckService.ReleaseInfo releaseInfo) {
        GuiUpdateExecutionSupport.GuidedUpgradePlan plan = updateExecutionSupport.planGuidedUpgrade(
                releaseInfo,
                installMaintenanceService.findInstallScript(LabelGuiFrame.class).orElse(null)
        );
        if (plan.openReleasePageFallback()) {
            openReleaseUrl(plan.releaseUrl());
            return;
        }

        setBusy(plan.busyMessage());
        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return guidedUpdateService.downloadInstaller(LabelGuiFrame.class, releaseInfo);
            }

            @Override
            protected void done() {
                try {
                    Path installerPath = get();
                    installMaintenanceService.launchInstaller(plan.installScript(), installerPath);
                    dispose();
                    System.exit(0);
                } catch (Exception ex) {
                    GuiUpdateExecutionSupport.FailureOutcome outcome = updateExecutionSupport.buildGuidedUpgradeFailure(ex);
                    setReady(outcome.statusMessage());
                    showError(outcome.errorMessage());
                }
            }
        };
        worker.execute();
    }

    private void openReleaseUrl(String releaseUrl) {
        GuiUpdateExecutionSupport.ReleasePagePlan plan = updateExecutionSupport.planReleasePageOpen(
                releaseUrl,
                Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
        );
        if (!plan.shouldOpenBrowser()) {
            showError(plan.errorMessage());
            return;
        }
        try {
            Desktop.getDesktop().browse(java.net.URI.create(plan.releaseUrl()));
        } catch (Exception ex) {
            showError("Could not open release page: " + rootMessage(ex));
        }
    }

    private void openUninstallDialog() {
        Object[] options = updateFlowSupport.uninstallOptions();
        int choice = JOptionPane.showOptionDialog(
                this,
                updateFlowSupport.uninstallMessage(),
                "Uninstall / Clean Install Prep",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );
        GuiUpdateExecutionSupport.UninstallPlan plan = updateExecutionSupport.planUninstall(
                installMaintenanceService.findUninstallScript(LabelGuiFrame.class).orElse(null),
                choice
        );
        if (plan.errorMessage() != null) {
            showError(plan.errorMessage());
            return;
        }
        if (!plan.shouldLaunch()) {
            return;
        }
        try {
            installMaintenanceService.launchUninstall(plan.uninstallScript(), plan.wipeInstallRoot());
            dispose();
            System.exit(0);
        } catch (Exception ex) {
            showError(updateExecutionSupport.buildUninstallFailure(ex).errorMessage());
        }
    }

    private void clearForm() {
        resetPreviewLabelSelection();
        preparedJob = null;
        preparedCarrierJob = null;
        previewShellSupport.applyClearedState(
                shipmentField,
                shipmentPreviewPanel,
                shipmentArea,
                mathArea,
                printButton,
                isCarrierMoveMode(),
                this::setReady
        );
    }

    private void updateInputModeUi() {
        preparedJob = null;
        preparedCarrierJob = null;
        resetPreviewLabelSelection();
        previewShellSupport.applyInputModeUi(
                inputLabel,
                isCarrierMoveMode(),
                shipmentPreviewPanel,
                shipmentArea,
                mathArea,
                printButton
        );
    }

    private boolean isCarrierMoveMode() {
        return carrierMoveModeButton.isSelected();
    }

    private void updatePrintButtonEnabled(PreviewSelectionSupport.SelectionSnapshot selection) {
        printButton.setEnabled(previewSelectionUiSupport.shouldEnablePrint(
                isCarrierMoveMode(),
                preparedCarrierJob != null,
                preparedJob != null,
                selection.selectedCarrierLabels().size(),
                selection.selectedShipmentLpns().size()
        ));
        showLabelsButton.setEnabled(printButton.isEnabled());
    }

    private void openGeneratedLabelPreview() {
        try {
            PreviewSelectionSupport.SelectionSnapshot selection = snapshotPreviewSelection();
            List<GuiZplPreviewSupport.PreviewDocument> documents = buildGeneratedLabelPreviewDocuments(selection);
            if (documents.isEmpty()) {
                showError("Preview at least one label before opening the ZPL preview.");
                return;
            }
            generatedLabelsPreviewDialog = ensureGeneratedLabelsPreviewDialog();
            generatedLabelsPreviewDialog.setPreviewDocuments(generatedLabelPreviewTitle(), documents);
            generatedLabelsPreviewDialog.setVisible(true);
            generatedLabelsPreviewDialog.toFront();
        } catch (Exception ex) {
            showError("Could not open label preview: " + rootMessage(ex));
        }
    }

    private void refreshGeneratedLabelPreviewDialog(PreviewSelectionSupport.SelectionSnapshot selection) {
        if (generatedLabelsPreviewDialog == null || !generatedLabelsPreviewDialog.isDisplayable() || !generatedLabelsPreviewDialog.isVisible()) {
            return;
        }
        try {
            List<GuiZplPreviewSupport.PreviewDocument> documents = buildGeneratedLabelPreviewDocuments(selection);
            if (documents.isEmpty()) {
                generatedLabelsPreviewDialog.clearPreviewDocuments();
                generatedLabelsPreviewDialog.setTitle(generatedLabelPreviewTitle());
                return;
            }
            generatedLabelsPreviewDialog.setPreviewDocuments(generatedLabelPreviewTitle(), documents);
        } catch (Exception ex) {
            generatedLabelsPreviewDialog.clearPreviewDocuments();
        }
    }

    private List<GuiZplPreviewSupport.PreviewDocument> buildGeneratedLabelPreviewDocuments(
            PreviewSelectionSupport.SelectionSnapshot selection
    ) {
        return isCarrierMoveMode()
                ? zplPreviewSupport.buildCarrierMoveDocuments(
                Objects.requireNonNull(preparedCarrierJob, "preparedCarrierJob"),
                selection.selectedCarrierLabels(),
                includeInfoTagsCheckBox.isSelected()
        )
                : zplPreviewSupport.buildShipmentDocuments(
                Objects.requireNonNull(preparedJob, "preparedJob"),
                selection.selectedShipmentLpns(),
                includeInfoTagsCheckBox.isSelected()
        );
    }

    private String generatedLabelPreviewTitle() {
        return isCarrierMoveMode()
                ? "Carrier Move Label Preview"
                : "Shipment Label Preview";
    }

    private ZplPreviewToolDialog ensureGeneratedLabelsPreviewDialog() {
        if (generatedLabelsPreviewDialog == null || !generatedLabelsPreviewDialog.isDisplayable()) {
            generatedLabelsPreviewDialog = new ZplPreviewToolDialog(this);
        }
        return generatedLabelsPreviewDialog;
    }

    private void clearGeneratedLabelPreviewDialog() {
        if (generatedLabelsPreviewDialog == null || !generatedLabelsPreviewDialog.isDisplayable()) {
            return;
        }
        generatedLabelsPreviewDialog.clearPreviewDocuments();
        generatedLabelsPreviewDialog.setTitle(generatedLabelPreviewTitle());
    }

    private void openQueueDialog() {
        queueResumeDialogSupport.openQueueDialog();
    }

    private void openResumeDialog() {
        queueResumeDialogSupport.openResumeDialog();
    }

    private void openRailLabelsDialog() {
        RailLabelsDialog dialog = new RailLabelsDialog(this, config);
        dialog.setVisible(true);
    }

    private void autoResumeIfFound() {
        queueResumeDialogSupport.autoResumeIfFound();
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
        return GuiExceptionMessageSupport.rootMessage(throwable);
    }

    private void openBarcodeDialog() {
        barcodeDialogFactory.open(this);
    }

    private void openZplPreviewDialog() {
        ZplPreviewToolDialog dialog = new ZplPreviewToolDialog(this);
        dialog.setVisible(true);
    }

    private void openAnalyzersDialog() {
        JOptionPane.showMessageDialog(
                this,
                "Analyzers are not implemented yet.",
                "Analyzers",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildMainPrintTargetModel(boolean includeFileOption) {
        return settingsDialogSupport.buildMainPrintTargetModel(includeFileOption);
    }

    private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(boolean includeFileOption) {
        return settingsDialogSupport.buildPrintTargetModel(includeFileOption);
    }

    private Path defaultPrintToFileOutputDir() {
        return settingsDialogSupport.defaultPrintToFileOutputDir();
    }

    private void openSettingsDialog() {
        settingsDialogSupport.openSettingsDialog();
    }

    private void restoreSelection(LabelWorkflowService.PrinterOption previousSelection) {
        framePrinterSelectionSupport.restoreSelection(printerCombo, previousSelection, printerSelectionSupport);
    }

    private void installTerminalLikeMouseClipboardBehavior(JTextComponent... fields) {
        clipboardController.install(fields);
    }

    private BarcodeDialogFactory.Dependencies buildBarcodeDependencies() {
        return new LabelGuiFrameBarcodeDependencies(
                this::buildPrintTargetModel,
                LabelGuiFrame::isPrintToFileSelected,
                this::defaultPrintToFileOutputDir,
                this::installTerminalLikeMouseClipboardBehavior,
                this::showError,
                this::rootMessage,
                service::resolvePrinter,
                this::printBarcodeZpl
        );
    }

    private void printBarcodeZpl(PrinterConfig printerConfig, String zpl) throws Exception {
        new NetworkPrintService().print(printerConfig, zpl, "barcode");
    }

    private LabelGuiFrameToolMenuSupport.MenuActions buildToolMenuActions() {
        return new LabelGuiFrameToolMenuActions(
                this::openRailLabelsDialog,
                this::openQueueDialog,
                this::openBarcodeDialog,
                this::openZplPreviewDialog,
                this::openAnalyzersDialog,
                this::openResumeDialog,
                this::openSettingsDialog
        );
    }

    private QueueResumeDialogSupport.Dependencies buildQueueResumeDependencies() {
        return new LabelGuiFrameQueueResumeDependencies(
                this,
                advancedService,
                previewFormatter,
                () -> (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem(),
                LabelGuiFrame::isPrintToFileSelected,
                this::installTerminalLikeMouseClipboardBehavior,
                this::showError,
                this::rootMessage,
                this::setReady
        );
    }

    private GuiSettingsDialogSupport.Dependencies buildSettingsDependencies() {
        return new LabelGuiFrameSettingsDependencies(
                this,
                preferences,
                runtimeSettings,
                config,
                () -> loadedPrinters,
                () -> (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem(),
                printerCombo::setModel,
                this::applyTopRowSizing,
                this::restoreSelection,
                this::showError,
                this::installTerminalLikeMouseClipboardBehavior,
                this::formatUpdateStatus,
                statusOutput -> checkForUpdatesAsync(true, statusOutput),
                this::openUninstallDialog,
                this::setReady,
                service::clearCaches,
                advancedService::clearCaches,
                () -> loadedPrinters = List.of(),
                this::loadPrintersAsync
        );
    }

    private void saveMainSettings(Path configuredPath, int retentionDays) {
        settingsDialogSupport.saveMainSettings(configuredPath, retentionDays);
    }

    private void runOutDirectoryCleanup(Component owner, int retentionDays) {
        settingsDialogSupport.runOutDirectoryCleanup(owner, retentionDays);
    }

    private void openAdvancedSettingsDialog(Component owner) {
        settingsDialogSupport.openAdvancedSettingsDialog(owner);
    }

    private void reloadRuntimeConfigArtifacts() {
        settingsDialogSupport.reloadRuntimeConfigArtifacts();
    }

}

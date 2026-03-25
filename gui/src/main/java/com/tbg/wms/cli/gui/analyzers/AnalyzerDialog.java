package com.tbg.wms.cli.gui.analyzers;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.DefaultListCellRenderer;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.FlowLayout;
import java.io.Serial;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardPanel;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSnapshot;

@SuppressWarnings("serial")
public final class AnalyzerDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;

    private final AnalyzerRegistry registry;
    private final AnalyzerContext context;
    private final JComboBox<AnalyzerDefinition<?>> analyzerCombo = new JComboBox<>();
    private final JButton refreshButton = new JButton("Refresh");
    private final JCheckBox autoRefreshCheckBox = new JCheckBox("Auto refresh");
    private final JComboBox<Duration> intervalCombo = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JLabel lastUpdatedLabel = new JLabel(" ");
    private final JTable table = new JTable();
    private final JScrollPane tableScrollPane = new JScrollPane(table);
    private final AnalyzerDashboardPanel dashboardPanel = new AnalyzerDashboardPanel();
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final AnalyzerRefreshScheduler refreshScheduler = new AnalyzerRefreshScheduler(this::loadSelectedAnalyzer);
    private final Executor loaderExecutor;
    private final ExecutorService ownedExecutorService;
    private final Map<String, AnalyzerLoadSessionState<?>> sessionStates = new HashMap<>();
    private String activePresentationId = "table";
    private long requestSequence;
    private long activeRequestId;
    private String activeAnalyzerId = "";

    public AnalyzerDialog(Frame owner, AnalyzerRegistry registry, AnalyzerContext context) {
        this(owner, registry, context,
                Executors.newSingleThreadExecutor(r -> {
                    Thread thread = new Thread(r, "analyzer-dialog-loader");
                    thread.setDaemon(true);
                    return thread;
                }),
                true);
    }

    AnalyzerDialog(Frame owner, AnalyzerRegistry registry, AnalyzerContext context, Executor loaderExecutor) {
        this(owner, registry, context, loaderExecutor, false);
    }

    private AnalyzerDialog(Frame owner, AnalyzerRegistry registry, AnalyzerContext context, Executor loaderExecutor, boolean ownExecutor) {
        super(owner, "Analyzers", false);
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.loaderExecutor = Objects.requireNonNull(loaderExecutor, "loaderExecutor cannot be null");
        this.ownedExecutorService = ownExecutor && loaderExecutor instanceof ExecutorService executorService ? executorService : null;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(960, 540);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        DefaultComboBoxModel<AnalyzerDefinition<?>> comboModel = new DefaultComboBoxModel<>();
        for (AnalyzerDefinition<?> definition : registry.definitions()) {
            comboModel.addElement(definition);
        }
        analyzerCombo.setModel(comboModel);
        analyzerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AnalyzerDefinition<?> definition) {
                    label.setText(definition.displayName());
                } else {
                    label.setText("");
                }
                return label;
            }
        });
        analyzerCombo.addActionListener(e -> loadSelectedAnalyzer());
        refreshButton.addActionListener(e -> requestRefresh());
        autoRefreshCheckBox.addActionListener(e -> refreshScheduler.setEnabled(autoRefreshCheckBox.isSelected()));
        intervalCombo.addActionListener(e -> {
            Duration selected = (Duration) intervalCombo.getSelectedItem();
            refreshScheduler.setInterval(selected == null ? Duration.ZERO : selected);
        });
        intervalCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Duration duration) {
                    label.setText(duration.toMinutes() + " min");
                }
                return label;
            }
        });
        DefaultComboBoxModel<Duration> intervalModel = new DefaultComboBoxModel<>();
        intervalModel.addElement(Duration.ofMinutes(1));
        intervalModel.addElement(Duration.ofMinutes(5));
        intervalModel.addElement(Duration.ofMinutes(15));
        intervalCombo.setModel(intervalModel);
        intervalCombo.setSelectedItem(Duration.ofMinutes(1));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(analyzerCombo);
        toolbar.add(refreshButton);
        toolbar.add(autoRefreshCheckBox);
        toolbar.add(intervalCombo);
        toolbar.add(lastUpdatedLabel);
        contentPanel.add(tableScrollPane, "table");
        contentPanel.add(dashboardPanel, "dashboard");
        add(toolbar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    void openForTest() {
        if (analyzerCombo.getSelectedItem() == null) {
            analyzerCombo.setSelectedItem(registry.defaultAnalyzer());
        }
        loadSelectedAnalyzer();
    }

    String selectedAnalyzerNameForTest() {
        AnalyzerDefinition<?> definition = (AnalyzerDefinition<?>) analyzerCombo.getSelectedItem();
        return definition == null ? "" : definition.displayName();
    }

    int tableRowCountForTest() {
        return table.getRowCount();
    }

    String activePresentationForTest() {
        return activePresentationId;
    }

    String statusTextForTest() {
        return statusLabel.getText();
    }

    String firstTableValueForTest() {
        if (table.getRowCount() == 0 || table.getColumnCount() == 0) {
            return "";
        }
        Object value = table.getValueAt(0, 0);
        return value == null ? "" : value.toString();
    }

    void selectAnalyzerForTest(int index) {
        analyzerCombo.setSelectedIndex(index);
    }

    void triggerManualRefreshForTest() {
        requestRefresh();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            openForTest();
        }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        if (ownedExecutorService != null) {
            ownedExecutorService.shutdownNow();
        }
        super.dispose();
    }

    private void loadSelectedAnalyzer() {
        AnalyzerDefinition<?> definition = (AnalyzerDefinition<?>) analyzerCombo.getSelectedItem();
        if (definition == null) {
            statusLabel.setText("No analyzer selected.");
            return;
        }
        loadDefinition(definition);
    }

    private <R> void loadDefinition(AnalyzerDefinition<R> definition) {
        statusLabel.setText("Loading...");
        AnalyzerLoadSessionState<R> state = sessionState(definition);
        if (state.hasSnapshot()) {
            renderSnapshot(definition, state.lastSuccessfulSnapshot());
        }
        long requestId = state.beginLoad(++requestSequence);
        activeRequestId = requestId;
        activeAnalyzerId = definition.id();
        loaderExecutor.execute(() -> {
            try {
                AnalyzerResult<R> result = definition.createProvider(context).load(context);
                AnalyzerLoadSnapshot<R> snapshot = AnalyzerLoadSnapshot.fromResult(result);
                state.recordSuccess(requestId, snapshot);
                if (!shouldApplyCompletion(definition, state, requestId)) {
                    return;
                }
                runOnUiThread(() -> {
                    if (!shouldApplyCompletion(definition, state, requestId)) {
                        return;
                    }
                    renderSnapshot(definition, snapshot);
                    statusLabel.setText("Loaded " + definition.displayName() + ".");
                });
            } catch (Exception ex) {
                state.recordFailure(requestId);
                if (!shouldApplyCompletion(definition, state, requestId)) {
                    return;
                }
                runOnUiThread(() -> {
                    if (shouldApplyCompletion(definition, state, requestId)) {
                        statusLabel.setText("Load failed: " + ex.getMessage());
                    }
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <R> AnalyzerLoadSessionState<R> sessionState(AnalyzerDefinition<R> definition) {
        return (AnalyzerLoadSessionState<R>) sessionStates.computeIfAbsent(definition.id(), ignored -> new AnalyzerLoadSessionState<>());
    }

    private <R> void renderSnapshot(AnalyzerDefinition<R> definition, AnalyzerLoadSnapshot<R> snapshot) {
        AnalyzerPresentation<R> presentation = definition.presentation();
        if (presentation instanceof DashboardAnalyzerPresentation<R>) {
            activePresentationId = "dashboard";
            contentLayout.show(contentPanel, "dashboard");
            @SuppressWarnings("unchecked")
            java.util.List<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot> sections =
                    (java.util.List<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot>) snapshot.rows();
            dashboardPanel.showSnapshot(new AnalyzerDashboardSnapshot(sections));
        } else {
            AnalyzerTableModel<R> model = new AnalyzerTableModel<>(definition.columns());
            model.setRows(snapshot.rows());
            table.setModel(model);
            table.setDefaultRenderer(Object.class, new AnalyzerTableCellRenderer<>(model, definition.rowStyler()));
            activePresentationId = "table";
            contentLayout.show(contentPanel, "table");
        }
        refreshScheduler.markRefreshCompleted(snapshot.fetchedAt());
        lastUpdatedLabel.setText("Last updated: " + snapshot.fetchedAt());
    }

    private <R> boolean shouldApplyCompletion(AnalyzerDefinition<R> definition, AnalyzerLoadSessionState<R> state, long requestId) {
        return state.isLatestRequest(requestId)
                && activeRequestId == requestId
                && activeAnalyzerId.equals(definition.id());
    }

    private void runOnUiThread(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void requestRefresh() {
        refreshScheduler.requestImmediateRefresh();
    }
}

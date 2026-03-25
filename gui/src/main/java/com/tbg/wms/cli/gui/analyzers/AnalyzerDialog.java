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
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.FlowLayout;
import java.io.Serial;
import java.time.Duration;
import java.util.Objects;

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
    private String activePresentationId = "table";

    public AnalyzerDialog(Frame owner, AnalyzerRegistry registry, AnalyzerContext context) {
        super(owner, "Analyzers", false);
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.context = Objects.requireNonNull(context, "context cannot be null");
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
        try {
            AnalyzerPresentation<R> presentation = definition.presentation();
            if (presentation instanceof DashboardAnalyzerPresentation<R>) {
                AnalyzerResult<R> result = definition.createProvider(context).load(context);
                activePresentationId = "dashboard";
                contentLayout.show(contentPanel, "dashboard");
                @SuppressWarnings("unchecked")
                java.util.List<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot> sections =
                        (java.util.List<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot>) result.rows();
                dashboardPanel.showSnapshot(new AnalyzerDashboardSnapshot(sections));
                refreshScheduler.markRefreshCompleted(result.fetchedAt());
                lastUpdatedLabel.setText("Last updated: " + result.fetchedAt());
                statusLabel.setText("Loaded " + definition.displayName() + ".");
                return;
            }

            AnalyzerResult<R> result = definition.createProvider(context).load(context);
            AnalyzerTableModel<R> model = new AnalyzerTableModel<>(definition.columns());
            model.setRows(result.rows());
            table.setModel(model);
            table.setDefaultRenderer(Object.class, new AnalyzerTableCellRenderer<>(model, definition.rowStyler()));
            refreshScheduler.markRefreshCompleted(result.fetchedAt());
            lastUpdatedLabel.setText("Last updated: " + result.fetchedAt());
            activePresentationId = "table";
            contentLayout.show(contentPanel, "table");
            statusLabel.setText("Loaded " + definition.displayName() + ".");
        } catch (Exception ex) {
            statusLabel.setText("Load failed: " + ex.getMessage());
        }
    }

    private void requestRefresh() {
        refreshScheduler.requestImmediateRefresh();
    }
}

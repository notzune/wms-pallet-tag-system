package com.tbg.wms.cli.gui.analyzers;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.DefaultListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.io.Serial;
import java.util.Objects;

@SuppressWarnings("serial")
public final class AnalyzerDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;

    private final AnalyzerRegistry registry;
    private final AnalyzerContext context;
    private final JComboBox<AnalyzerDefinition<?>> analyzerCombo = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JTable table = new JTable();

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

        add(analyzerCombo, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
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
        try {
            definition.createProvider(context).load(context);
            statusLabel.setText("Loaded " + definition.displayName() + ".");
        } catch (Exception ex) {
            statusLabel.setText("Load failed: " + ex.getMessage());
        }
    }
}

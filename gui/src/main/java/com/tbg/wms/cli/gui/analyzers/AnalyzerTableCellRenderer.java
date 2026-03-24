package com.tbg.wms.cli.gui.analyzers;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.io.Serial;
import java.util.Objects;

@SuppressWarnings("serial")
public final class AnalyzerTableCellRenderer<R> extends DefaultTableCellRenderer {
    @Serial
    private static final long serialVersionUID = 1L;

    private final AnalyzerTableModel<R> model;
    private final AnalyzerRowStyler<R> styler;

    public AnalyzerTableCellRenderer(AnalyzerTableModel<R> model, AnalyzerRowStyler<R> styler) {
        this.model = Objects.requireNonNull(model, "model cannot be null");
        this.styler = Objects.requireNonNull(styler, "styler cannot be null");
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        R modelRow = model.rowAt(table.convertRowIndexToModel(row));
        AnalyzerRowStyle style = styler.styleFor(modelRow);
        component.setBackground(style.background());
        component.setForeground(style.foreground());
        return component;
    }
}

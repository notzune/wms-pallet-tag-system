package com.tbg.wms.cli.gui.analyzers;

import javax.swing.table.AbstractTableModel;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings("serial")
public final class AnalyzerTableModel<R> extends AbstractTableModel {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Function<R, Object> valueAccessor;
    private final List<String> columnNames;
    private List<R> rows = List.of();

    public AnalyzerTableModel(Function<R, Object> valueAccessor, List<String> columnNames) {
        this.valueAccessor = Objects.requireNonNull(valueAccessor, "valueAccessor cannot be null");
        this.columnNames = List.copyOf(Objects.requireNonNull(columnNames, "columnNames cannot be null"));
    }

    public void setRows(List<R> rows) {
        this.rows = List.copyOf(new ArrayList<>(Objects.requireNonNull(rows, "rows cannot be null")));
        fireTableDataChanged();
    }

    public R rowAt(int rowIndex) {
        return rows.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return valueAccessor.apply(rows.get(rowIndex));
    }
}

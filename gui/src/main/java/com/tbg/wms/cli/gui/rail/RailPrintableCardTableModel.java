/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.core.rail.RailCarCard;

import javax.swing.table.AbstractTableModel;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for rail preview rows and their explicit print-inclusion state.
 */
final class RailPrintableCardTableModel extends AbstractTableModel {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "PRINT", "TRAIN", "SEQ", "VEHICLE", "CAN", "DOM", "KEV", "LOAD_NBR"
    };

    private final transient List<RailCarCard> cards = new ArrayList<>();
    private final transient List<Boolean> printable = new ArrayList<>();

    void setCards(List<RailCarCard> newCards) {
        cards.clear();
        printable.clear();
        if (newCards != null) {
            cards.addAll(newCards);
            for (int i = 0; i < newCards.size(); i++) {
                printable.add(Boolean.TRUE);
            }
        }
        fireTableDataChanged();
    }

    RailCarCard cardAt(int modelRow) {
        return cards.get(modelRow);
    }

    List<RailCarCard> selectedCards() {
        List<RailCarCard> selected = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if (Boolean.TRUE.equals(printable.get(i))) {
                selected.add(cards.get(i));
            }
        }
        return List.copyOf(selected);
    }

    int selectedCount() {
        int count = 0;
        for (Boolean value : printable) {
            if (Boolean.TRUE.equals(value)) {
                count++;
            }
        }
        return count;
    }

    int totalCount() {
        return cards.size();
    }

    void setAllPrintable() {
        setPrintableRange(true);
    }

    void clearAllPrintable() {
        setPrintableRange(false);
    }

    void invertPrintable() {
        for (int i = 0; i < printable.size(); i++) {
            printable.set(i, !Boolean.TRUE.equals(printable.get(i)));
        }
        firePrintableColumnUpdated();
    }

    void togglePrintableRows(int[] modelRows) {
        if (modelRows == null) {
            return;
        }
        for (int modelRow : modelRows) {
            if (modelRow >= 0 && modelRow < printable.size()) {
                printable.set(modelRow, !Boolean.TRUE.equals(printable.get(modelRow)));
            }
        }
        firePrintableColumnUpdated();
    }

    private void setPrintableRange(boolean value) {
        for (int i = 0; i < printable.size(); i++) {
            printable.set(i, value);
        }
        firePrintableColumnUpdated();
    }

    private void firePrintableColumnUpdated() {
        if (!printable.isEmpty()) {
            fireTableRowsUpdated(0, printable.size() - 1);
        }
    }

    @Override
    public int getRowCount() {
        return cards.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RailCarCard card = cards.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> printable.get(rowIndex);
            case 1 -> card.getTrainId();
            case 2 -> card.getSequence();
            case 3 -> card.getVehicleId();
            case 4 -> card.getCanPallets();
            case 5 -> card.getDomPallets();
            case 6 -> card.getKevPallets();
            case 7 -> card.getLoadNumbers();
            default -> throw new IllegalArgumentException("Column index out of range: " + columnIndex);
        };
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && rowIndex >= 0 && rowIndex < printable.size()) {
            printable.set(rowIndex, Boolean.TRUE.equals(value));
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}

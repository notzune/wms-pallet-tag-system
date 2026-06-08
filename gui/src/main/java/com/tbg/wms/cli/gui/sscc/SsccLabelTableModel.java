package com.tbg.wms.cli.gui.sscc;

import com.tbg.wms.core.sscc.SsccLabelRow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Table model for staged raw SSCC rows.
 */
final class SsccLabelTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "Sales Order #",
            "Purchase Order #",
            "Shipment #",
            "Carrier Code",
            "Trailer ID",
            "Destination",
            "Destination Address",
            "Customer Name",
            "Facility",
            "Item #",
            "Level 2 Reference #",
            "Originally Shipped LPN",
            "Sum of Ship Cases",
            "New Received LPN"
    };

    private final transient List<SsccLabelRow> rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return rows.size();
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        SsccLabelRow row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.salesOrder();
            case 1 -> row.purchaseOrder();
            case 2 -> row.shipment();
            case 3 -> row.carrierCode();
            case 4 -> row.trailerId();
            case 5 -> row.destination();
            case 6 -> row.destinationAddress();
            case 7 -> row.customerName();
            case 8 -> row.facility();
            case 9 -> row.itemNumber();
            case 10 -> row.level2Reference();
            case 11 -> row.originallyShippedLpn();
            case 12 -> row.sumOfShipCases();
            case 13 -> row.newReceivedLpn();
            default -> "";
        };
    }

    void addRow(SsccLabelRow row) {
        rows.add(Objects.requireNonNull(row, "row cannot be null"));
        int index = rows.size() - 1;
        fireTableRowsInserted(index, index);
    }

    void addRows(List<SsccLabelRow> incomingRows) {
        if (incomingRows == null || incomingRows.isEmpty()) {
            return;
        }
        int start = rows.size();
        for (SsccLabelRow row : incomingRows) {
            if (row != null) {
                rows.add(row);
            }
        }
        if (rows.size() > start) {
            fireTableRowsInserted(start, rows.size() - 1);
        }
    }

    void removeRows(int[] selectedRows) {
        if (selectedRows == null || selectedRows.length == 0) {
            return;
        }
        int[] modelRows = selectedRows.clone();
        java.util.Arrays.sort(modelRows);
        for (int i = modelRows.length - 1; i >= 0; i--) {
            int index = modelRows[i];
            if (index >= 0 && index < rows.size()) {
                rows.remove(index);
            }
        }
        fireTableDataChanged();
    }

    void clear() {
        if (rows.isEmpty()) {
            return;
        }
        rows.clear();
        fireTableDataChanged();
    }

    List<SsccLabelRow> rows() {
        return List.copyOf(rows);
    }
}

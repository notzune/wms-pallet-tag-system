package com.tbg.wms.cli.gui.sscc;

import com.tbg.wms.core.sscc.SsccLabelGroup;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Table model for grouped SSCC labels.
 */
final class SsccGroupTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "Sales Order #",
            "New Received LPN",
            "SKU Mode",
            "Item / SKU",
            "Lot/Date",
            "Cases",
            "Shipment #",
            "Carrier Code",
            "Trailer ID",
            "Purchase Order #",
            "Ship To"
    };

    private final transient List<SsccLabelGroup> groups = new ArrayList<>();

    @Override
    public int getRowCount() {
        return groups.size();
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
        SsccLabelGroup group = groups.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> group.salesOrder();
            case 1 -> group.newReceivedLpn();
            case 2 -> group.mixedSku() ? "Mixed SKU" : "Single SKU";
            case 3 -> group.itemLineText();
            case 4 -> group.level2ReferenceCodes().isEmpty() ? "" : String.join(", ", group.level2ReferenceCodes());
            case 5 -> group.cartonText();
            case 6 -> group.shipment();
            case 7 -> group.carrierCode();
            case 8 -> group.trailerId();
            case 9 -> group.purchaseOrder();
            case 10 -> summarizeShipTo(group);
            default -> "";
        };
    }

    void setGroups(List<SsccLabelGroup> incomingGroups) {
        groups.clear();
        if (incomingGroups != null) {
            for (SsccLabelGroup group : incomingGroups) {
                if (group != null) {
                    groups.add(group);
                }
            }
        }
        fireTableDataChanged();
    }

    SsccLabelGroup groupAt(int rowIndex) {
        return groups.get(rowIndex);
    }

    List<SsccLabelGroup> groups() {
        return List.copyOf(groups);
    }

    private String summarizeShipTo(SsccLabelGroup group) {
        String destination = Objects.toString(group.destination(), "").trim();
        String address = Objects.toString(group.destinationAddress(), "").trim();
        if (destination.isBlank()) {
            return address;
        }
        if (address.isBlank()) {
            return destination;
        }
        return destination + " | " + address;
    }
}

package com.tbg.wms.v2.oracle.shipment;

import com.tbg.wms.v2.domain.label.LabelSelectionRef;

import java.util.Locale;

/**
 * Carries shipment label row data across WMS 2.0 module boundaries.
 *
 * @param shipmentId the shipment id.
 * @param label the label.
 */
public record ShipmentLabelRow(String shipmentId, LabelSelectionRef label) {
    public ShipmentLabelRow {
        shipmentId = upper(shipmentId);
        if (shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId is required.");
        }
        if (label == null) {
            throw new IllegalArgumentException("label is required.");
        }
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

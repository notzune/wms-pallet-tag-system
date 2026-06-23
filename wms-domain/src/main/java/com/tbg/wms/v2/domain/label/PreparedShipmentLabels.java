package com.tbg.wms.v2.domain.label;

import java.util.List;

/**
 * Shipment label data that has already been loaded and is ready for print planning.
 */
public record PreparedShipmentLabels(
        String shipmentId,
        List<LabelSelectionRef> palletLabels,
        boolean includeShipmentInfoTag
) {
    public PreparedShipmentLabels {
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId is required");
        }
        if (palletLabels == null) {
            throw new IllegalArgumentException("palletLabels is required");
        }
        shipmentId = shipmentId.trim();
        palletLabels = List.copyOf(palletLabels);
    }

    public static PreparedShipmentLabels of(
            String shipmentId,
            List<LabelSelectionRef> palletLabels,
            boolean includeShipmentInfoTag
    ) {
        return new PreparedShipmentLabels(shipmentId, palletLabels, includeShipmentInfoTag);
    }
}

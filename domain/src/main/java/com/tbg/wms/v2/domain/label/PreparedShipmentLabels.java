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

    /**
     * Creates a normalized domain value.
     *
     * @param shipmentId the shipment id.
     * @param palletLabels the pallet labels.
     * @param includeShipmentInfoTag the include shipment info tag.
     * @return the created value.
     */
    public static PreparedShipmentLabels of(
            String shipmentId,
            List<LabelSelectionRef> palletLabels,
            boolean includeShipmentInfoTag
    ) {
        return new PreparedShipmentLabels(shipmentId, palletLabels, includeShipmentInfoTag);
    }
}

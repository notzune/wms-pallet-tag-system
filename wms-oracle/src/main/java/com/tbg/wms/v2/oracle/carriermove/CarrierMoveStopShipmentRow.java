package com.tbg.wms.v2.oracle.carriermove;

import java.util.Locale;

public record CarrierMoveStopShipmentRow(
        String carrierMoveId,
        String stopId,
        int stopSequence,
        String shipmentId
) {
    public CarrierMoveStopShipmentRow {
        carrierMoveId = upper(carrierMoveId);
        stopId = upper(stopId);
        shipmentId = upper(shipmentId);
        if (carrierMoveId.isBlank()) {
            throw new IllegalArgumentException("carrierMoveId is required.");
        }
        if (stopId.isBlank()) {
            throw new IllegalArgumentException("stopId is required.");
        }
        if (stopSequence < 1) {
            throw new IllegalArgumentException("stopSequence must be positive.");
        }
        if (shipmentId.isBlank()) {
            throw new IllegalArgumentException("shipmentId is required.");
        }
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

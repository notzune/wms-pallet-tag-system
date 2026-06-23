package com.tbg.wms.v2.domain.carriermove;

import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;

import java.util.List;

/**
 * One carrier-move stop with its prepared shipment label groups.
 */
public record PreparedStopGroup(
        int stopPosition,
        Integer stopSequence,
        List<PreparedShipmentLabels> shipments
) {
    public PreparedStopGroup {
        if (stopPosition < 1) {
            throw new IllegalArgumentException("stopPosition must be positive");
        }
        if (shipments == null) {
            throw new IllegalArgumentException("shipments is required");
        }
        shipments = List.copyOf(shipments);
    }

    public static PreparedStopGroup of(
            int stopPosition,
            Integer stopSequence,
            List<PreparedShipmentLabels> shipments
    ) {
        return new PreparedStopGroup(stopPosition, stopSequence, shipments);
    }
}

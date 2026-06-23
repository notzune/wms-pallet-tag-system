package com.tbg.wms.v2.app.queue;

import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;

public record PreparedQueueItem(
        QueueItemType type,
        String sourceId,
        PreparedShipmentLabels shipment,
        CarrierMoveLabels carrierMove
) {
    public PreparedQueueItem {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId is required");
        }
        if (type == QueueItemType.SHIPMENT && shipment == null) {
            throw new IllegalArgumentException("shipment is required");
        }
        if (type == QueueItemType.CARRIER_MOVE && carrierMove == null) {
            throw new IllegalArgumentException("carrierMove is required");
        }
        sourceId = sourceId.trim();
    }

    public static PreparedQueueItem forShipment(String sourceId, PreparedShipmentLabels shipment) {
        return new PreparedQueueItem(QueueItemType.SHIPMENT, sourceId, shipment, null);
    }

    public static PreparedQueueItem forCarrierMove(String sourceId, CarrierMoveLabels carrierMove) {
        return new PreparedQueueItem(QueueItemType.CARRIER_MOVE, sourceId, null, carrierMove);
    }
}

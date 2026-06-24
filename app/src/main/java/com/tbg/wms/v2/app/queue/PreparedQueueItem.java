package com.tbg.wms.v2.app.queue;

import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;

/**
 * Carries prepared queue item data across WMS 2.0 module boundaries.
 *
 * @param type the type.
 * @param sourceId the source id.
 * @param shipment the shipment.
 * @param carrierMove the carrier move.
 */
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

    /**
     * Creates a prepared queue item for a shipment.
     *
     * @param sourceId the source ID.
     * @param shipment the shipment.
     * @return the prepared shipment queue item.
     */
    public static PreparedQueueItem forShipment(String sourceId, PreparedShipmentLabels shipment) {
        return new PreparedQueueItem(QueueItemType.SHIPMENT, sourceId, shipment, null);
    }

    /**
     * Creates a prepared queue item for a carrier move.
     *
     * @param sourceId the source ID.
     * @param carrierMove the carrier move.
     * @return the prepared carrier-move queue item.
     */
    public static PreparedQueueItem forCarrierMove(String sourceId, CarrierMoveLabels carrierMove) {
        return new PreparedQueueItem(QueueItemType.CARRIER_MOVE, sourceId, null, carrierMove);
    }
}

package com.tbg.wms.v2.app.queue;

import com.tbg.wms.v2.app.ports.CarrierMoveRepository;
import com.tbg.wms.v2.app.ports.ShipmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves parsed queue items into prepared shipment and carrier-move label data.
 */
public final class PrepareQueue {
    private final ShipmentRepository shipmentRepository;
    private final CarrierMoveRepository carrierMoveRepository;
    private final int maxQueueItems;

    /**
     * Creates a Prepare Queue instance.
     *
     * @param shipmentRepository the shipment repository.
     * @param carrierMoveRepository the carrier move repository.
     * @param maxQueueItems the max queue items.
     */
    public PrepareQueue(
            ShipmentRepository shipmentRepository,
            CarrierMoveRepository carrierMoveRepository,
            int maxQueueItems
    ) {
        this.shipmentRepository = Objects.requireNonNull(shipmentRepository, "shipmentRepository");
        this.carrierMoveRepository = Objects.requireNonNull(carrierMoveRepository, "carrierMoveRepository");
        if (maxQueueItems <= 0) {
            throw new IllegalArgumentException("maxQueueItems must be > 0.");
        }
        this.maxQueueItems = maxQueueItems;
    }

    /**
     * Prepares workflow input for later execution.
     *
     * @param requests the requests.
     * @return the prepared queue.
     */
    public PreparedQueue prepare(List<QueueRequestItem> requests) {
        List<QueueRequestItem> normalized = normalizeRequests(requests);
        List<PreparedQueueItem> prepared = new ArrayList<>(normalized.size());
        for (QueueRequestItem request : normalized) {
            if (request.type() == QueueItemType.SHIPMENT) {
                prepared.add(PreparedQueueItem.forShipment(
                        request.id(),
                        shipmentRepository.findByShipmentId(request.id())
                ));
            } else {
                prepared.add(PreparedQueueItem.forCarrierMove(
                        request.id(),
                        carrierMoveRepository.findByCarrierMoveId(request.id())
                ));
            }
        }
        return new PreparedQueue(prepared);
    }

    private List<QueueRequestItem> normalizeRequests(List<QueueRequestItem> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty.");
        }
        if (requests.size() > maxQueueItems) {
            throw new IllegalArgumentException("Queue exceeds max size of " + maxQueueItems + " items.");
        }
        List<QueueRequestItem> normalized = new ArrayList<>();
        for (QueueRequestItem request : requests) {
            if (request != null && !request.id().isBlank()) {
                normalized.add(request);
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty after parsing.");
        }
        return List.copyOf(normalized);
    }
}

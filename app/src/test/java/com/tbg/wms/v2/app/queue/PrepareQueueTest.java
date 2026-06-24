package com.tbg.wms.v2.app.queue;

import com.tbg.wms.v2.app.ports.CarrierMoveRepository;
import com.tbg.wms.v2.app.ports.ShipmentRepository;
import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.carriermove.PreparedStopGroup;
import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrepareQueueTest {
    @Test
    void prepareQueue_resolvesItemsInInputOrder() {
        PrepareQueue prepareQueue = new PrepareQueue(
                shipmentRepository(Map.of("8001", shipment("8001"))),
                carrierRepository(Map.of("77", carrierMove("77"))),
                10
        );

        PreparedQueue queue = prepareQueue.prepare(List.of(
                new QueueRequestItem(QueueItemType.SHIPMENT, "8001"),
                new QueueRequestItem(QueueItemType.CARRIER_MOVE, "77")
        ));

        assertEquals(2, queue.items().size());
        assertEquals(QueueItemType.SHIPMENT, queue.items().get(0).type());
        assertEquals("8001", queue.items().get(0).sourceId());
        assertEquals(QueueItemType.CARRIER_MOVE, queue.items().get(1).type());
        assertEquals("77", queue.items().get(1).sourceId());
    }

    @Test
    void prepareQueue_rejectsEmptyAndOversizedRequests() {
        PrepareQueue prepareQueue = new PrepareQueue(id -> shipment(id), id -> carrierMove(id), 1);

        assertThrows(IllegalArgumentException.class, () -> prepareQueue.prepare(List.of()));
        assertThrows(IllegalArgumentException.class, () -> prepareQueue.prepare(List.of(
                new QueueRequestItem(QueueItemType.SHIPMENT, "8001"),
                new QueueRequestItem(QueueItemType.CARRIER_MOVE, "77")
        )));
    }

    private static ShipmentRepository shipmentRepository(Map<String, PreparedShipmentLabels> shipments) {
        return shipments::get;
    }

    private static CarrierMoveRepository carrierRepository(Map<String, CarrierMoveLabels> carrierMoves) {
        return carrierMoves::get;
    }

    private static PreparedShipmentLabels shipment(String shipmentId) {
        return new PreparedShipmentLabels(
                shipmentId,
                List.of(new LabelSelectionRef("LPN-" + shipmentId, 1)),
                true
        );
    }

    private static CarrierMoveLabels carrierMove(String carrierMoveId) {
        return new CarrierMoveLabels(
                carrierMoveId,
                List.of(PreparedStopGroup.of(1, 10, List.of(shipment("8001")))),
                true
        );
    }
}

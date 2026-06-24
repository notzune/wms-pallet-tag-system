package com.tbg.wms.v2.app.queue;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParseQueueInputTest {
    private final ParseQueueInput parser = new ParseQueueInput();

    @Test
    void parseQueueInput_honorsExplicitPrefixesAndSplitsSemicolonsAndLines() {
        List<QueueRequestItem> items = parser.parse(
                "S: 800123456; C: 12345\r\nS:800999999",
                QueueItemType.CARRIER_MOVE,
                10
        );

        assertEquals(List.of(
                new QueueRequestItem(QueueItemType.SHIPMENT, "800123456"),
                new QueueRequestItem(QueueItemType.CARRIER_MOVE, "12345"),
                new QueueRequestItem(QueueItemType.SHIPMENT, "800999999")
        ), items);
    }

    @Test
    void parseQueueInput_autoDetectsUnprefixedNumericIds() {
        List<QueueRequestItem> items = parser.parse(
                "800123456\n12345\nmanual-id",
                QueueItemType.SHIPMENT,
                10
        );

        assertEquals(List.of(
                new QueueRequestItem(QueueItemType.SHIPMENT, "800123456"),
                new QueueRequestItem(QueueItemType.CARRIER_MOVE, "12345"),
                new QueueRequestItem(QueueItemType.SHIPMENT, "manual-id")
        ), items);
    }

    @Test
    void parseQueueInput_rejectsEmptyInputAndMaxOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(" ; \n", QueueItemType.SHIPMENT, 10));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("8001;8002;8003", QueueItemType.SHIPMENT, 2));

        assertEquals("Queue input exceeds max size of 2 items.", ex.getMessage());
    }
}

package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class QueueInputParserTest {

    @Test
    void parseSupportsMixedPrefixes() {
        List<AdvancedPrintWorkflowService.QueueRequestItem> items = QueueInputParser.parse(
                "C:12345\nS:8000546666\n  8000999999  ",
                AdvancedPrintWorkflowService.QueueItemType.SHIPMENT,
                10
        );

        assertEquals(3, items.size());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, items.get(0).getType());
        assertEquals("12345", items.get(0).getId());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, items.get(1).getType());
        assertEquals("8000546666", items.get(1).getId());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, items.get(2).getType());
        assertEquals("8000999999", items.get(2).getId());
    }

    @Test
    void parseRejectsEmptyInput() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> QueueInputParser.parse(" \n \r\n ", AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, 10)
        );
        assertEquals("Queue input is empty.", ex.getMessage());
    }

    @Test
    void parseEnforcesMaximumQueueSize() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> QueueInputParser.parse("1\n2\n3", AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, 2)
        );
        assertEquals("Queue input exceeds max size of 2 items.", ex.getMessage());
    }
}

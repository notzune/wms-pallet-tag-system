/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests queue text parsing rules used by the GUI queue dialog.
 */
final class QueueInputParserTest {

    /**
     * Supports explicit mixed-item prefixes while preserving unprefixed default type.
     */
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

    /**
     * Rejects blank input payloads.
     */
    @Test
    void parseRejectsEmptyInput() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> QueueInputParser.parse(" \n \r\n ", AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, 10)
        );
        assertEquals("Queue input is empty.", ex.getMessage());
    }

    /**
     * Rejects input that exceeds configured queue capacity.
     */
    @Test
    void parseEnforcesMaximumQueueSize() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> QueueInputParser.parse("1\n2\n3", AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, 2)
        );
        assertEquals("Queue input exceeds max size of 2 items.", ex.getMessage());
    }
}

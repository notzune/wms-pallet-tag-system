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

    @Test
    void parseSupportsCarriageReturnSeparatedInput() {
        List<AdvancedPrintWorkflowService.QueueRequestItem> items = QueueInputParser.parse(
                "C:55555\rS:7000000001\r7000000002",
                AdvancedPrintWorkflowService.QueueItemType.SHIPMENT,
                10
        );

        assertEquals(3, items.size());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, items.get(0).getType());
        assertEquals("55555", items.get(0).getId());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, items.get(1).getType());
        assertEquals("7000000001", items.get(1).getId());
        assertEquals("7000000002", items.get(2).getId());
    }

    @Test
    void parseSupportsSemicolonSeparatedInputAndIgnoresWhitespace() {
        List<AdvancedPrintWorkflowService.QueueRequestItem> items = QueueInputParser.parse(
                " 8000574112 ; FREJCC1226;  S:8000575651 ; C:FREJCG3125 ",
                AdvancedPrintWorkflowService.QueueItemType.SHIPMENT,
                10
        );

        assertEquals(4, items.size());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, items.get(0).getType());
        assertEquals("8000574112", items.get(0).getId());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, items.get(1).getType());
        assertEquals("FREJCC1226", items.get(1).getId());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, items.get(2).getType());
        assertEquals("8000575651", items.get(2).getId());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, items.get(3).getType());
        assertEquals("FREJCG3125", items.get(3).getId());
    }

    @Test
    void parseUsesDefaultTypeForAmbiguousUnprefixedIds() {
        List<AdvancedPrintWorkflowService.QueueRequestItem> items = QueueInputParser.parse(
                "12345;67890",
                AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE,
                10
        );

        assertEquals(2, items.size());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, items.get(0).getType());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, items.get(1).getType());
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

    @Test
    void parseRejectsNullDefaultType() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> QueueInputParser.parse("123", null, 10)
        );
        assertEquals("defaultType cannot be null", ex.getMessage());
    }

    @Test
    void parseRejectsNonPositiveMaxItems() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> QueueInputParser.parse("123", AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, 0)
        );
        assertEquals("maxItems must be > 0.", ex.getMessage());
    }
}

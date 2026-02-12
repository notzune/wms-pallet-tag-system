/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for domain models: LineItem, Lpn, and Shipment.
 */
class DomainModelTest {

    @Test
    void testLineItemCreation() {
        LineItem item = new LineItem(
                "1", "0", "SKU123", "Product A", null,
                "ORD456", null, null,
                10, 0, "LB", 25.5,
                null, null, null
        );

        assertEquals("1", item.getLineNumber());
        assertEquals("SKU123", item.getSku());
        assertEquals("Product A", item.getDescription());
        assertEquals(10, item.getQuantity());
        assertEquals(25.5, item.getWeight());
        assertEquals("LB", item.getUom());
    }

    @Test
    void testLpnCreation() {
        LineItem item = new LineItem(
                "1", "0", "SKU123", "Product A", null,
                "ORD456", null, null,
                10, 0, "LB", 25.5,
                null, null, null
        );
        java.time.LocalDate mfgDate = java.time.LocalDate.now().minusDays(30);
        java.time.LocalDate expDate = java.time.LocalDate.now().plusMonths(6);

        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI",
                "LOT001", "SLOT001", mfgDate, expDate, List.of(item));

        assertEquals("LPN001", lpn.getLpnId());
        assertEquals("SHIP123", lpn.getShipmentId());
        assertEquals("123456789012", lpn.getSscc());
        assertEquals(5, lpn.getCaseCount());
        assertEquals(50, lpn.getUnitCount());
        assertEquals(127.5, lpn.getWeight());
        assertEquals("ROSSI", lpn.getStagingLocation());
        assertEquals(1, lpn.getLineItems().size());
    }

    @Test
    void testLpnImmutability() {
        LineItem item = new LineItem(
                "1", "0", "SKU123", "Product A", null,
                "ORD456", null, null,
                10, 0, "LB", 25.5,
                null, null, null
        );
        List<LineItem> originalList = new ArrayList<>(List.of(item));
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI",
                "LOT001", "SLOT001", java.time.LocalDate.now(), java.time.LocalDate.now(), originalList);

        originalList.clear();
        assertEquals(1, lpn.getLineItems().size(), "LPN should maintain copy of line items");

        assertThrows(UnsupportedOperationException.class, () -> lpn.getLineItems().add(item),
                "Line items should be immutable");
    }

    @Test
    void testShipmentCreation() {
        LineItem item = new LineItem(
                "1", "0", "SKU123", "Product A", null,
                "ORD456", null, null,
                10, 0, "LB", 25.5,
                null, null, null
        );
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI",
                "LOT001", "SLOT001", java.time.LocalDate.now(), java.time.LocalDate.now(), List.of(item));
        LocalDateTime now = LocalDateTime.now();

        Shipment shipment = new Shipment(
                "SHIP123", "EXT123", "ORD456", "3002",
                "Acme Corp", "123 Main St", null, null,
                "Springfield", "IL", "62701", "USA", "555-1234",
                "UPS", "GND", "BOL123", "TRACK123", "STAGE1",
                "PO123", "LOC123", "DEPT1",
                "STOP1", 1, "MOVE1", "PRO123", "BOL123",
                "R", now.minusDays(1), now.plusDays(3), now, List.of(lpn)
        );

        assertEquals("SHIP123", shipment.getShipmentId());
        assertEquals("ORD456", shipment.getOrderId());
        assertEquals("Acme Corp", shipment.getShipToName());
        assertEquals("123 Main St", shipment.getShipToAddress1());
        assertEquals("Springfield", shipment.getShipToCity());
        assertEquals("IL", shipment.getShipToState());
        assertEquals("62701", shipment.getShipToZip());
        assertEquals("UPS", shipment.getCarrierCode());
        assertEquals("GND", shipment.getServiceLevel());
        assertEquals(now, shipment.getCreatedDate());
        assertEquals(1, shipment.getLpnCount());
        assertEquals(1, shipment.getLpns().size());
    }

    @Test
    void testShipmentImmutability() {
        LineItem item = new LineItem(
                "1", "0", "SKU123", "Product A", null,
                "ORD456", null, null,
                10, 0, "LB", 25.5,
                null, null, null
        );
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI",
                "LOT001", "SLOT001", java.time.LocalDate.now(), java.time.LocalDate.now(), List.of(item));
        List<Lpn> originalList = new ArrayList<>(List.of(lpn));

        Shipment shipment = new Shipment(
                "SHIP123", "EXT123", "ORD456", "3002",
                "Acme Corp", "123 Main St", null, null,
                "Springfield", "IL", "62701", "USA", "555-1234",
                "UPS", "GND", "BOL123", "TRACK123", "STAGE1",
                "PO123", "LOC123", "DEPT1",
                "STOP1", 1, "MOVE1", "PRO123", "BOL123",
                "R", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(3), LocalDateTime.now(), originalList
        );

        originalList.clear();
        assertEquals(1, shipment.getLpnCount(), "Shipment should maintain copy of LPNs");

        assertThrows(UnsupportedOperationException.class, () -> shipment.getLpns().add(lpn),
                "LPNs should be immutable");
    }

    @Test
    void testShipmentWithNullLpns() {
        Shipment shipment = new Shipment(
                "SHIP123", "EXT123", "ORD456", "3002",
                "Acme Corp", "123 Main St", null, null,
                "Springfield", "IL", "62701", "USA", "555-1234",
                "UPS", "GND", "BOL123", "TRACK123", "STAGE1",
                "PO123", "LOC123", "DEPT1",
                "STOP1", 1, "MOVE1", "PRO123", "BOL123",
                "R", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(3), LocalDateTime.now(), null
        );

        assertEquals(0, shipment.getLpnCount());
        assertNotNull(shipment.getLpns());
    }

    @Test
    void testLineItemToString() {
        LineItem item = new LineItem(
                "1", "0", "SKU123", "Product A", null,
                "ORD456", null, null,
                10, 0, "LB", 25.5,
                null, null, null
        );
        String str = item.toString();

        assertTrue(str.contains("SKU123"));
        assertTrue(str.contains("Product A"));
    }

    @Test
    void testLpnToString() {
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI",
                "LOT001", "SLOT001", java.time.LocalDate.now(), java.time.LocalDate.now(), List.of());
        String str = lpn.toString();

        assertTrue(str.contains("LPN001"));
        assertTrue(str.contains("ROSSI"));
    }

    @Test
    void testShipmentToString() {
        Shipment shipment = new Shipment(
                "SHIP123", "EXT123", "ORD456", "3002",
                "Acme Corp", "123 Main St", null, null,
                "Springfield", "IL", "62701", "USA", "555-1234",
                "UPS", "GND", "BOL123", "TRACK123", "STAGE1",
                "PO123", "LOC123", "DEPT1",
                "STOP1", 1, "MOVE1", "PRO123", "BOL123",
                "R", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(3), LocalDateTime.now(), List.of()
        );
        String str = shipment.toString();

        assertTrue(str.contains("SHIP123"));
        assertTrue(str.contains("UPS"));
    }
}



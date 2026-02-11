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
        LineItem item = new LineItem("1", "SKU123", "Product A", 10, 25.5, "LB");

        assertEquals("1", item.getLineNumber());
        assertEquals("SKU123", item.getSku());
        assertEquals("Product A", item.getDescription());
        assertEquals(10, item.getQuantity());
        assertEquals(25.5, item.getWeight());
        assertEquals("LB", item.getUom());
    }

    @Test
    void testLpnCreation() {
        LineItem item = new LineItem("1", "SKU123", "Product A", 10, 25.5, "LB");
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI", List.of(item));

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
        LineItem item = new LineItem("1", "SKU123", "Product A", 10, 25.5, "LB");
        List<LineItem> originalList = new ArrayList<>(List.of(item));
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI", originalList);

        originalList.clear();
        assertEquals(1, lpn.getLineItems().size(), "LPN should maintain copy of line items");

        assertThrows(UnsupportedOperationException.class, () -> lpn.getLineItems().add(item),
                "Line items should be immutable");
    }

    @Test
    void testShipmentCreation() {
        LineItem item = new LineItem("1", "SKU123", "Product A", 10, 25.5, "LB");
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI", List.of(item));
        LocalDateTime now = LocalDateTime.now();

        Shipment shipment = new Shipment(
                "SHIP123", "ORD456", "Acme Corp", "123 Main St",
                "Springfield", "IL", "62701", "UPS", "GND", now, List.of(lpn)
        );

        assertEquals("SHIP123", shipment.getShipmentId());
        assertEquals("ORD456", shipment.getOrderId());
        assertEquals("Acme Corp", shipment.getShipToName());
        assertEquals("123 Main St", shipment.getShipToAddress());
        assertEquals("Springfield", shipment.getShipToCity());
        assertEquals("IL", shipment.getShipToState());
        assertEquals("62701", shipment.getShipToZip());
        assertEquals("UPS", shipment.getCarrierCode());
        assertEquals("GND", shipment.getServiceCode());
        assertEquals(now, shipment.getCreatedDate());
        assertEquals(1, shipment.getLpnCount());
        assertEquals(1, shipment.getLpns().size());
    }

    @Test
    void testShipmentImmutability() {
        LineItem item = new LineItem("1", "SKU123", "Product A", 10, 25.5, "LB");
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI", List.of(item));
        List<Lpn> originalList = new ArrayList<>(List.of(lpn));

        Shipment shipment = new Shipment(
                "SHIP123", "ORD456", "Acme Corp", "123 Main St",
                "Springfield", "IL", "62701", "UPS", "GND", LocalDateTime.now(), originalList
        );

        originalList.clear();
        assertEquals(1, shipment.getLpnCount(), "Shipment should maintain copy of LPNs");

        assertThrows(UnsupportedOperationException.class, () -> shipment.getLpns().add(lpn),
                "LPNs should be immutable");
    }

    @Test
    void testShipmentWithNullLpns() {
        Shipment shipment = new Shipment(
                "SHIP123", "ORD456", "Acme Corp", "123 Main St",
                "Springfield", "IL", "62701", "UPS", "GND", LocalDateTime.now(), null
        );

        assertEquals(0, shipment.getLpnCount());
        assertNotNull(shipment.getLpns());
    }

    @Test
    void testLineItemToString() {
        LineItem item = new LineItem("1", "SKU123", "Product A", 10, 25.5, "LB");
        String str = item.toString();

        assertTrue(str.contains("SKU123"));
        assertTrue(str.contains("Product A"));
    }

    @Test
    void testLpnToString() {
        Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI", List.of());
        String str = lpn.toString();

        assertTrue(str.contains("LPN001"));
        assertTrue(str.contains("ROSSI"));
    }

    @Test
    void testShipmentToString() {
        Shipment shipment = new Shipment(
                "SHIP123", "ORD456", "Acme Corp", "123 Main St",
                "Springfield", "IL", "62701", "UPS", "GND", LocalDateTime.now(), List.of()
        );
        String str = shipment.toString();

        assertTrue(str.contains("SHIP123"));
        assertTrue(str.contains("UPS"));
    }
}



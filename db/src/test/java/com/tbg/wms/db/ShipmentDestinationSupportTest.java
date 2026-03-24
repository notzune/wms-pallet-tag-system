package com.tbg.wms.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShipmentDestinationSupportTest {
    private final ShipmentDestinationSupport support = new ShipmentDestinationSupport();

    @Test
    void resolveLocationNumber_shouldPreferExplicitDestinationThenVcDestination() {
        assertEquals("12345", support.resolveLocationNumber("12345", "VC-100", "DC 999", "HOST-1"));
        assertEquals("VC-100", support.resolveLocationNumber(" ", "VC-100", "DC 999", "HOST-1"));
    }

    @Test
    void resolveLocationNumber_shouldExtractDcNumberThenFallbackToAddressHost() {
        assertEquals("3002", support.resolveLocationNumber("", "", "JERSEY CITY DC #3002", "HOST-1"));
        assertEquals("HOST-1", support.resolveLocationNumber("", "", "No DC Name", "HOST-1"));
    }

    @Test
    void extractDcNumber_shouldReturnNullWhenNoPatternExists() {
        assertNull(support.extractDcNumber("Retail Store"));
    }
}

/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class PrintRuntimeConfigSupportTest {

    @Test
    void returnsDefaultsWhenPrinterSettingsAreUnset() {
        ConfigValueSupport values = new ConfigValueSupport(Map.of(), Map.of(), Map.of(), "wms-tags.env");
        PrintRuntimeConfigSupport support = new PrintRuntimeConfigSupport(values);

        assertEquals("config/printer-routing.yaml", support.printerRoutingFile());
        assertEquals("DISPATCH", support.defaultPrinterId());
        assertEquals(0.125d, support.railLabelCenterGapInches());
        assertEquals(0.02d, support.railLabelOffsetXInches());
        assertEquals(0.02d, support.railLabelOffsetYInches());
        assertNull(support.railDefaultPrinterIdOrNull());
        assertNull(support.forcedPrinterIdOrNull());
    }

    @Test
    void trimsOptionalPrinterOverridesWhenPresent() {
        ConfigValueSupport values = new ConfigValueSupport(
                Map.of(
                        "RAIL_DEFAULT_PRINTER_ID", " RAIL-01 ",
                        "PRINTER_FORCE_ID", " OFFICE "
                ),
                Map.of(),
                Map.of(),
                "wms-tags.env"
        );
        PrintRuntimeConfigSupport support = new PrintRuntimeConfigSupport(values);

        assertEquals("RAIL-01", support.railDefaultPrinterIdOrNull());
        assertEquals("OFFICE", support.forcedPrinterIdOrNull());
    }
}

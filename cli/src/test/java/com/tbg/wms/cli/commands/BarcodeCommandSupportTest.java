/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class BarcodeCommandSupportTest {

    private final BarcodeCommandSupport support = new BarcodeCommandSupport();

    @Test
    void validateOptionsRejectsInvalidNumericValues() {
        assertEquals("Error: --copies must be > 0.",
                support.validateOptions("ABC", 812, 1218, 60, 60, 3, 3, 220, 0));
        assertEquals("Error: label dimensions must be > 0.",
                support.validateOptions("ABC", 0, 1218, 60, 60, 3, 3, 220, 1));
        assertEquals("Error: origin offsets must be >= 0.",
                support.validateOptions("ABC", 812, 1218, -1, 60, 3, 3, 220, 1));
        assertNull(support.validateOptions("ABC", 812, 1218, 60, 60, 3, 3, 220, 1));
    }

    @Test
    void safeSlugNormalizesAndTrimsEdgeDashes() {
        assertEquals("hello-world-123", BarcodeCommandSupport.safeSlug("  Hello World 123  "));
        assertEquals("data", BarcodeCommandSupport.safeSlug("!!!"));
    }
}

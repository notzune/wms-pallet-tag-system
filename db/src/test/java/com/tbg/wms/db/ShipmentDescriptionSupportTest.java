package com.tbg.wms.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShipmentDescriptionSupportTest {
    private final ShipmentDescriptionSupport support = new ShipmentDescriptionSupport();

    @Test
    void chooseBestDescription_shouldPreferPrtdscThenPrtmstThenFallback() {
        assertEquals("PRTDSC value", support.chooseBestDescription("PRTDSC value", "PRTMST value", "Fallback"));
        assertEquals("PRTMST value", support.chooseBestDescription("----", "PRTMST value", "Fallback"));
        assertEquals("Fallback", support.chooseBestDescription("1234", "5678", "Fallback"));
    }

    @Test
    void chooseBestDescription_shouldReturnNullWhenNothingReadable() {
        assertNull(support.chooseBestDescription("1234", "5678", ""));
    }
}

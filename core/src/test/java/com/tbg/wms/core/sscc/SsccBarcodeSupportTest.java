package com.tbg.wms.core.sscc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SsccBarcodeSupportTest {
    @Test
    void formatSsccCode128Data_prefixes18DigitSscc() {
        assertEquals(">;>800123456789012345678", SsccBarcodeSupport.formatSsccCode128Data("123456789012345678"));
    }

    @Test
    void formatSsccHumanReadable_preserves20DigitSscc() {
        assertEquals("(00) 123456789012345678", SsccBarcodeSupport.formatSsccHumanReadable("00123456789012345678"));
    }

    @Test
    void formatSsccCode128Data_rejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> SsccBarcodeSupport.formatSsccCode128Data("123"));
    }
}

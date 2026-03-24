package com.tbg.wms.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PalletMathSupportTest {

    @Test
    void calculateShouldHandleFullAndPartialPallets() {
        PalletMathSupport.PalletCounts counts = PalletMathSupport.calculate(61, 6);

        assertEquals(61, counts.units());
        assertEquals(10, counts.fullPallets());
        assertEquals(1, counts.partialPallets());
        assertEquals(11, counts.estimatedPallets());
    }

    @Test
    void calculateShouldTreatMissingFootprintAsZeroPalletMath() {
        PalletMathSupport.PalletCounts counts = PalletMathSupport.calculate(61, null);

        assertEquals(61, counts.units());
        assertEquals(0, counts.fullPallets());
        assertEquals(0, counts.partialPallets());
        assertEquals(0, counts.estimatedPallets());
    }
}

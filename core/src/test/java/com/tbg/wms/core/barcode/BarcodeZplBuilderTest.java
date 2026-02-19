/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.1.0
 */

package com.tbg.wms.core.barcode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for BarcodeZplBuilder.
 */
public class BarcodeZplBuilderTest {

    @Test
    public void testBuildCode128Portrait() {
        BarcodeZplBuilder.BarcodeRequest request = new BarcodeZplBuilder.BarcodeRequest(
                "ABC123",
                BarcodeZplBuilder.Symbology.CODE128,
                BarcodeZplBuilder.Orientation.PORTRAIT,
                812,
                1218,
                40,
                40,
                2,
                3,
                120,
                true,
                1
        );

        String zpl = BarcodeZplBuilder.build(request);
        assertTrue(zpl.contains("^PON"));
        assertTrue(zpl.contains("^BCN"));
        assertTrue(zpl.contains("^FDABC123^FS"));
    }

    @Test
    public void testBuildLandscapeRotation() {
        BarcodeZplBuilder.BarcodeRequest request = new BarcodeZplBuilder.BarcodeRequest(
                "ABC123",
                BarcodeZplBuilder.Symbology.CODE128,
                BarcodeZplBuilder.Orientation.LANDSCAPE,
                812,
                1218,
                40,
                40,
                2,
                3,
                120,
                false,
                1
        );

        String zpl = BarcodeZplBuilder.build(request);
        assertTrue(zpl.contains("^PON"));
        assertTrue(zpl.contains("^FWR"));
        assertTrue(zpl.contains("^BCR"));
    }

    @Test
    public void testBuildGs1Prefix() {
        BarcodeZplBuilder.BarcodeRequest request = new BarcodeZplBuilder.BarcodeRequest(
                "001234567890123456",
                BarcodeZplBuilder.Symbology.GS1_128,
                BarcodeZplBuilder.Orientation.PORTRAIT,
                812,
                1218,
                40,
                40,
                2,
                3,
                120,
                true,
                1
        );

        String zpl = BarcodeZplBuilder.build(request);
        assertTrue(zpl.contains("^FD>;001234567890123456^FS"));
    }

    @Test
    public void testEscapeZplCharacters() {
        BarcodeZplBuilder.BarcodeRequest request = new BarcodeZplBuilder.BarcodeRequest(
                "A^B~C",
                BarcodeZplBuilder.Symbology.CODE128,
                BarcodeZplBuilder.Orientation.PORTRAIT,
                812,
                1218,
                40,
                40,
                2,
                3,
                120,
                true,
                1
        );

        String zpl = BarcodeZplBuilder.build(request);
        assertTrue(zpl.contains("^FDA~~^B~~C^FS"));
    }
}

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
        assertTrue(zpl.contains("^FO285,489"));
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
        assertTrue(zpl.contains("^FO346,452"));
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

    @Test
    public void testBuildRespectsMinimumSafeMarginsWhenCentering() {
        BarcodeZplBuilder.BarcodeRequest request = new BarcodeZplBuilder.BarcodeRequest(
                "A",
                BarcodeZplBuilder.Symbology.CODE128,
                BarcodeZplBuilder.Orientation.PORTRAIT,
                200,
                300,
                80,
                90,
                2,
                3,
                80,
                false,
                1
        );

        String zpl = BarcodeZplBuilder.build(request);
        assertTrue(zpl.contains("^FO80,90"));
    }

    @Test
    public void testBuildDualContainsBothCaptionsAndBarcodes() {
        BarcodeZplBuilder.BarcodeRequest start = new BarcodeZplBuilder.BarcodeRequest(
                "\u001B[18~03BREAK\tSTART\r",
                BarcodeZplBuilder.Symbology.CODE128,
                BarcodeZplBuilder.Orientation.PORTRAIT,
                812,
                1218,
                60,
                60,
                3,
                3,
                220,
                false,
                1,
                "BREAK START",
                true
        );
        BarcodeZplBuilder.BarcodeRequest stop = new BarcodeZplBuilder.BarcodeRequest(
                "\u001B[18~03BREAK\tSTOP\r",
                BarcodeZplBuilder.Symbology.CODE128,
                BarcodeZplBuilder.Orientation.PORTRAIT,
                812,
                1218,
                60,
                650,
                3,
                3,
                220,
                false,
                1,
                "BREAK STOP",
                true
        );

        String zpl = BarcodeZplBuilder.buildDual(start, stop);
        assertTrue(zpl.contains("BREAK START"));
        assertTrue(zpl.contains("BREAK STOP"));
        assertTrue(zpl.indexOf("^BCN") != zpl.lastIndexOf("^BCN"));
    }
}

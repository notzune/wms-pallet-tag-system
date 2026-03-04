/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests deterministic info-tag ZPL rendering helpers.
 */
final class InfoTagZplBuilderTest {

    @Test
    void buildStopInfoTagIncludesStopMetadataAndShipmentList() {
        String zpl = InfoTagZplBuilder.buildStopInfoTag(
                "CM1234",
                2,
                5,
                70,
                List.of("8000001", "8000002"),
                List.of()
        );

        assertTrue(zpl.contains("STOP 2 OF 5"));
        assertTrue(zpl.contains("SHIPMENTS: 8000001, 8000002"));
        assertTrue(zpl.contains("SEQ 70"));
    }

}

/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for {@link AppConfig}.
 */
class AppConfigTest {

    @Test
    void testMissingRequiredKey() {
        // With .env missing required keys, this should throw.
        assertThrows(IllegalStateException.class, () -> {
            new AppConfig().activeSiteCode();
        }, "Should throw if ACTIVE_SITE is not configured");
    }

    @Test
    void testDefaultValues() {
        // These should not throw even without .env, since they have defaults.
        AppConfig cfg = new AppConfig();

        // WMS_ENV defaults to QA
        assertEquals("QA", cfg.wmsEnvironment());

        // ORACLE_PORT defaults to 1521
        assertEquals(1521, cfg.oraclePort());

        // ORACLE_SERVICE defaults to WMSP
        assertEquals("WMSP", cfg.oracleService());

        // PRINTER_DEFAULT_ID defaults to DISPATCH
        assertEquals("DISPATCH", cfg.defaultPrinterId());

        // forcedPrinterIdOrNull should return null when not set
        assertNull(cfg.forcedPrinterIdOrNull());
    }
}


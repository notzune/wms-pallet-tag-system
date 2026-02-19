/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for {@link AppConfig}.
 */
class AppConfigTest {

    @Test
    void testMissingRequiredKey() {
        // Use a deterministic missing key independent of local .env presence.
        assertThrows(IllegalStateException.class, () -> {
            new AppConfig().siteName("NON_EXISTENT_SITE");
        }, "Should throw for unknown required site key");
    }

    @Test
    void testDefaultValues() {
        // These should not throw even without .env, since they have defaults.
        AppConfig cfg = new AppConfig();

        // WMS_ENV defaults to PROD
        assertEquals("PROD", cfg.wmsEnvironment());

        // ORACLE_PORT defaults to 1521
        assertEquals(1521, cfg.oraclePort());

        // ORACLE_SERVICE defaults to WMSP
        assertEquals("WMSP", cfg.oracleService());

        // PRINTER_DEFAULT_ID defaults to DISPATCH
        assertEquals("DISPATCH", cfg.defaultPrinterId());

        // forcedPrinterIdOrNull should return null when not set
        assertNull(cfg.forcedPrinterIdOrNull());
    }

    @Test
    void testLoadsExplicitConfigFileFromSystemProperty() throws Exception {
        Path tempConfig = Files.createTempFile("wms-tags-test", ".env");
        Files.writeString(tempConfig, String.join("\n",
                "ACTIVE_SITE=TBG9999",
                "SITE_TBG9999_NAME=Test Site",
                "SITE_TBG9999_PROD_HOST=10.0.0.9",
                "ORACLE_USERNAME=test_user",
                "ORACLE_PASSWORD=test_pass"
        ), StandardCharsets.UTF_8);

        String key = "wms.config.file";
        String previous = System.getProperty(key);
        System.setProperty(key, tempConfig.toString());
        try {
            AppConfig cfg = new AppConfig();
            assertEquals("TBG9999", cfg.activeSiteCode());
            assertEquals("Test Site", cfg.siteName("TBG9999"));
            assertEquals("10.0.0.9", cfg.siteHost("TBG9999"));
            assertEquals("test_user", cfg.oracleUsername());
            assertEquals("test_pass", cfg.oraclePassword());
            assertNotNull(cfg.loadedConfigFileOrNull());
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
            Files.deleteIfExists(tempConfig);
        }
    }
}


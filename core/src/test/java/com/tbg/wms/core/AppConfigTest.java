/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for {@link AppConfig}.
 */
class AppConfigTest {

    @Test
    void testMissingRequiredKey(@TempDir Path tempDir) throws Exception {
        Path emptyConfig = Files.createFile(tempDir.resolve("empty.env"));

        // Use a deterministic missing key independent of local .env presence.
        AppConfig cfg = new AppConfig(Map.of(), emptyConfig);
        assertThrows(IllegalStateException.class, () -> {
            cfg.siteName("NON_EXISTENT_SITE");
        }, "Should throw for unknown required site key");
    }

    @Test
    void testDefaultValues(@TempDir Path tempDir) throws Exception {
        Path emptyConfig = Files.createFile(tempDir.resolve("empty.env"));

        // These should not throw even without .env, since they have defaults.
        AppConfig cfg = new AppConfig(Map.of(), emptyConfig);

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
    void testLoadsExplicitConfigFileFromConstructor() throws Exception {
        Path tempConfig = Files.createTempFile("wms-tags-test", ".env");
        Files.writeString(tempConfig, String.join("\n",
                "ACTIVE_SITE=TBG9999",
                "SITE_TBG9999_NAME=Test Site",
                "SITE_TBG9999_PROD_HOST=10.0.0.9",
                "ORACLE_USERNAME=test_user",
                "ORACLE_PASSWORD=test_pass"
        ), StandardCharsets.UTF_8);

        try {
            AppConfig cfg = new AppConfig(Map.of(), tempConfig);
            assertEquals("TBG9999", cfg.activeSiteCode());
            assertEquals("Test Site", cfg.siteName("TBG9999"));
            assertEquals("10.0.0.9", cfg.siteHost("TBG9999"));
            assertEquals("test_user", cfg.oracleUsername());
            assertEquals("test_pass", cfg.oraclePassword());
            assertNotNull(cfg.loadedConfigFileOrNull());
        } finally {
            Files.deleteIfExists(tempConfig);
        }
    }

    @Test
    void testJdbcCandidateOrderingIncludesOdbcFallback() throws Exception {
        Path tempConfig = Files.createTempFile("wms-tags-test", ".env");
        Files.writeString(tempConfig, String.join("\n",
                "ACTIVE_SITE=TBG3002",
                "SITE_TBG3002_NAME=Jersey City",
                "SITE_TBG3002_PROD_HOST=10.19.68.61",
                "ORACLE_USERNAME=RPTADM",
                "ORACLE_PASSWORD=test_pass",
                "ORACLE_SERVICE=WMSP",
                "ORACLE_PORT=1521",
                "ORACLE_DSN=(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.19.96.121)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=jcnwmsdbd01)))",
                "ORACLE_ODBC_DSN=TBG3002"
        ), StandardCharsets.UTF_8);

        try {
            AppConfig cfg = new AppConfig(Map.of(), tempConfig);
            List<String> urls = cfg.oracleJdbcUrlCandidates();
            assertEquals("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.19.96.121)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=jcnwmsdbd01)))", urls.get(0));
            assertEquals("jdbc:oracle:thin:@TBG3002", urls.get(1));
            assertEquals("jdbc:oracle:thin:@//10.19.68.61:1521/WMSP", urls.get(2));
        } finally {
            Files.deleteIfExists(tempConfig);
        }
    }

    @Test
    void testOdbcAliasIsNullWhenUnset() throws Exception {
        Path tempConfig = Files.createTempFile("wms-tags-test", ".env");
        Files.writeString(tempConfig, String.join("\n",
                "ACTIVE_SITE=TBG3002",
                "SITE_TBG3002_NAME=Jersey City",
                "SITE_TBG3002_PROD_HOST=10.19.68.61",
                "ORACLE_USERNAME=RPTADM",
                "ORACLE_PASSWORD=test_pass"
        ), StandardCharsets.UTF_8);

        try {
            AppConfig cfg = new AppConfig(Map.of(), tempConfig);
            assertNull(cfg.oracleOdbcDsnOrNull());
        } finally {
            Files.deleteIfExists(tempConfig);
        }
    }
}


/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleConnectionConfigSupportTest {

    @Test
    void resolveWmsEnvironment_shouldPreferEnvOrFileValuesThenFallbackToDefaults() {
        OracleConnectionConfigSupport support = support(
                Map.of("WMS_ENV", "qa"),
                Map.of("ACTIVE_ENV", "prod"),
                Map.of()
        );

        assertEquals("QA", support.resolveWmsEnvironment());
    }

    @Test
    void resolveSiteHost_shouldPreferScopedHostThenFallbackHost() {
        OracleConnectionConfigSupport support = support(
                Map.of(
                        "WMS_ENV", "qa",
                        "SITE_TBG3002_QA_HOST", "10.0.0.10",
                        "SITE_TBG3002_HOST", "10.0.0.20"
                ),
                Map.of(),
                Map.of("SITE_TBG3002_HOST", "10.0.0.20")
        );

        assertEquals("10.0.0.10", support.resolveSiteHost("TBG3002"));
    }

    @Test
    void resolveJdbcUrl_shouldPreferExplicitJdbcThenDsn() {
        OracleConnectionConfigSupport explicitSupport = support(
                Map.of("ORACLE_JDBC_URL", "jdbc:oracle:thin:@//db-host:1521/WMSP"),
                Map.of(),
                Map.of("SITE_TBG3002_HOST", "10.0.0.20")
        );
        OracleConnectionConfigSupport dsnSupport = support(
                Map.of("ORACLE_DSN", "(DESCRIPTION=(HOST=db-host))"),
                Map.of(),
                Map.of("SITE_TBG3002_HOST", "10.0.0.20")
        );

        assertEquals(
                "jdbc:oracle:thin:@//db-host:1521/WMSP",
                explicitSupport.resolveJdbcUrl("TBG3002", () -> 1521, () -> "WMSP")
        );
        assertEquals(
                "jdbc:oracle:thin:@(DESCRIPTION=(HOST=db-host))",
                dsnSupport.resolveJdbcUrl("TBG3002", () -> 1521, () -> "WMSP")
        );
    }

    @Test
    void resolveJdbcUrlCandidates_shouldIncludeAliasAndAvoidDuplicates() {
        OracleConnectionConfigSupport support = support(
                Map.of(
                        "ORACLE_ODBC_DSN", "TBG3002",
                        "SITE_TBG3002_HOST", "10.19.68.61"
                ),
                Map.of(),
                Map.of("SITE_TBG3002_HOST", "10.19.68.61")
        );

        assertEquals(
                List.of(
                        "jdbc:oracle:thin:@//10.19.68.61:1521/WMSP",
                        "jdbc:oracle:thin:@TBG3002"
                ),
                support.resolveJdbcUrlCandidates("TBG3002", () -> 1521, () -> "WMSP")
        );
    }

    private OracleConnectionConfigSupport support(
            Map<String, String> envOrFileValues,
            Map<String, String> rawValues,
            Map<String, String> requiredValues
    ) {
        return new OracleConnectionConfigSupport(
                key -> rawValues.getOrDefault(key, envOrFileValues.get(key)),
                envOrFileValues::get,
                requiredValues::get
        );
    }
}

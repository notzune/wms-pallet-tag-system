package com.tbg.wms.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;

class OracleHikariConfigSupportTest {

    @Test
    void build_shouldApplyCommonOraclePoolSettings() {
        HikariConfig config = OracleHikariConfigSupport.build(
                new OracleHikariConfigSupport.Settings(
                        "jdbc:oracle:thin:@//warehouse.example:1521/WMS",
                        "wms_user",
                        "secret",
                        7,
                        8_000L,
                        3_000L,
                        "wms-tags-oracle",
                        false,
                        null,
                        null,
                        null));

        assertEquals("jdbc:oracle:thin:@//warehouse.example:1521/WMS", config.getJdbcUrl());
        assertEquals("wms_user", config.getUsername());
        assertEquals("secret", config.getPassword());
        assertEquals(7, config.getMaximumPoolSize());
        assertEquals(8_000L, config.getConnectionTimeout());
        assertEquals(3_000L, config.getValidationTimeout());
        assertEquals("wms-tags-oracle", config.getPoolName());
        assertTrue(config.isAutoCommit());
        assertFalse(config.isReadOnly());
        assertEquals("SELECT 1 FROM dual", config.getConnectionTestQuery());
    }

    @Test
    void build_shouldApplyReadOnlyProbePoolSettingsWhenProvided() {
        HikariConfig config = OracleHikariConfigSupport.build(
                new OracleHikariConfigSupport.Settings(
                        "jdbc:oracle:thin:@//fallback.example:1521/WMS",
                        "readonly_user",
                        "readonly_secret",
                        4,
                        10_000L,
                        5_000L,
                        "wms-tags-oracle-ATL",
                        true,
                        0,
                        -1L,
                        60_000L));

        assertTrue(config.isReadOnly());
        assertEquals(0, config.getMinimumIdle());
        assertEquals(-1L, config.getInitializationFailTimeout());
        assertEquals(60_000L, config.getLeakDetectionThreshold());
    }
}

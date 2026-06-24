package com.tbg.wms.v2.oracle;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleDataSourceFactoryTest {
    @Test
    void hikariConfig_preservesOracleTimeoutsReadOnlyModeAndValidationQuery() {
        HikariConfig config = OracleDataSourceFactory.hikariConfig(new OracleSettings(
                "jdbc:oracle:thin:@//host:1521/WMSP",
                "RPTADM",
                "secret",
                4,
                5_000,
                2_000,
                "wms-v2-oracle",
                true,
                1,
                -1L,
                60_000L
        ));

        assertEquals("jdbc:oracle:thin:@//host:1521/WMSP", config.getJdbcUrl());
        assertEquals("RPTADM", config.getUsername());
        assertEquals("secret", config.getPassword());
        assertEquals(4, config.getMaximumPoolSize());
        assertEquals(5_000, config.getConnectionTimeout());
        assertEquals(2_000, config.getValidationTimeout());
        assertEquals("wms-v2-oracle", config.getPoolName());
        assertTrue(config.isReadOnly());
        assertTrue(config.isAutoCommit());
        assertEquals("SELECT 1 FROM dual", config.getConnectionTestQuery());
        assertEquals(1, config.getMinimumIdle());
        assertEquals(-1L, config.getInitializationFailTimeout());
        assertEquals(60_000L, config.getLeakDetectionThreshold());
    }
}

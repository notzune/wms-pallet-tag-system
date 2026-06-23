package com.tbg.wms.core.db;

import com.zaxxer.hikari.HikariConfig;

/**
 * Builds shared Oracle HikariCP configuration used by CLI and GUI database clients.
 */
public final class OracleHikariConfigSupport {

    private OracleHikariConfigSupport() {
    }

    public static HikariConfig build(Settings settings) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(settings.jdbcUrl());
        hc.setUsername(settings.username());
        hc.setPassword(settings.password());

        hc.setMaximumPoolSize(settings.maximumPoolSize());
        hc.setConnectionTimeout(settings.connectionTimeoutMs());
        hc.setValidationTimeout(settings.validationTimeoutMs());

        hc.setPoolName(settings.poolName());
        hc.setAutoCommit(true);
        hc.setReadOnly(settings.readOnly());
        hc.setConnectionTestQuery("SELECT 1 FROM dual");

        if (settings.minimumIdle() != null) {
            hc.setMinimumIdle(settings.minimumIdle());
        }
        if (settings.initializationFailTimeoutMs() != null) {
            hc.setInitializationFailTimeout(settings.initializationFailTimeoutMs());
        }
        if (settings.leakDetectionThresholdMs() != null) {
            hc.setLeakDetectionThreshold(settings.leakDetectionThresholdMs());
        }

        return hc;
    }

    public record Settings(
            String jdbcUrl,
            String username,
            String password,
            int maximumPoolSize,
            long connectionTimeoutMs,
            long validationTimeoutMs,
            String poolName,
            boolean readOnly,
            Integer minimumIdle,
            Long initializationFailTimeoutMs,
            Long leakDetectionThresholdMs) {
    }
}

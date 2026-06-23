package com.tbg.wms.v2.oracle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class OracleDataSourceFactory {
    private OracleDataSourceFactory() {
    }

    public static HikariConfig hikariConfig(OracleSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.maximumPoolSize());
        config.setConnectionTimeout(settings.connectionTimeoutMs());
        config.setValidationTimeout(settings.validationTimeoutMs());
        config.setPoolName(settings.poolName());
        config.setAutoCommit(true);
        config.setReadOnly(settings.readOnly());
        config.setConnectionTestQuery("SELECT 1 FROM dual");

        if (settings.minimumIdle() != null) {
            config.setMinimumIdle(settings.minimumIdle());
        }
        if (settings.initializationFailTimeoutMs() != null) {
            config.setInitializationFailTimeout(settings.initializationFailTimeoutMs());
        }
        if (settings.leakDetectionThresholdMs() != null) {
            config.setLeakDetectionThreshold(settings.leakDetectionThresholdMs());
        }
        return config;
    }

    public static DataSource create(OracleSettings settings) {
        return new HikariDataSource(hikariConfig(settings));
    }
}

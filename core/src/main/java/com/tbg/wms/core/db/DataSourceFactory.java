package com.tbg.wms.core.db;

import com.tbg.wms.core.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Creates a pooled {@link DataSource} for Oracle using HikariCP.
 */
public final class DataSourceFactory {

    private final AppConfig config;

    public DataSourceFactory(AppConfig config) {
        this.config = config;
    }

    public DataSource create() {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.oracleJdbcUrl());
        hc.setUsername(config.oracleUsername());
        hc.setPassword(config.oraclePassword());

        hc.setMaximumPoolSize(config.dbPoolMaxSize());
        hc.setConnectionTimeout(config.dbPoolConnectionTimeoutMs());
        hc.setValidationTimeout(config.dbPoolValidationTimeoutMs());

        // Conservative defaults for a CLI tool.
        hc.setPoolName("wms-tags-oracle");
        hc.setAutoCommit(true);

        // Oracle best practice: lightweight validation query.
        hc.setConnectionTestQuery("SELECT 1 FROM dual");

        return new HikariDataSource(hc);
    }
}

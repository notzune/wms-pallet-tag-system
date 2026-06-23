/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.db;

import com.tbg.wms.core.AppConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Creates a pooled {@link DataSource} for Oracle using HikariCP.
 *
 * <p>The factory reads configuration from {@link AppConfig} and sets up a connection pool
 * with conservative defaults suitable for CLI usage. Connection pool is managed by HikariCP
 * and should be closed via {@link javax.sql.DataSource#getConnection()} when done.</p>
 *
 * <p><strong>Note:</strong> The returned {@link HikariDataSource} implements {@link DataSource}
 * and should be cast if you need to call {@link HikariDataSource#close()} directly.</p>
 */
public final class DataSourceFactory {

    private final AppConfig config;

    /**
     * Creates a new factory with the given configuration.
     *
     * @param config the application configuration containing Oracle connection details
     */
    public DataSourceFactory(AppConfig config) {
        this.config = config;
    }

    /**
     * Creates and configures a HikariCP connection pool for Oracle.
     *
     * <p>Pool settings are determined by the following configuration keys:</p>
     * <ul>
     *   <li>JDBC URL (via {@link AppConfig#oracleJdbcUrl()})</li>
     *   <li>Username (via {@link AppConfig#oracleUsername()})</li>
     *   <li>Password (via {@link AppConfig#oraclePassword()})</li>
     *   <li>Max pool size (via {@link AppConfig#dbPoolMaxSize()})</li>
     *   <li>Connection timeout (via {@link AppConfig#dbPoolConnectionTimeoutMs()})</li>
     *   <li>Validation timeout (via {@link AppConfig#dbPoolValidationTimeoutMs()})</li>
     * </ul>
     *
     * @return a configured {@link DataSource} (actually a {@link HikariDataSource})
     * @throws IllegalStateException if required configuration is missing
     */
    public DataSource create() {
        return new HikariDataSource(OracleHikariConfigSupport.build(
                new OracleHikariConfigSupport.Settings(
                        config.oracleJdbcUrl(),
                        config.oracleUsername(),
                        config.oraclePassword(),
                        config.dbPoolMaxSize(),
                        config.dbPoolConnectionTimeoutMs(),
                        config.dbPoolValidationTimeoutMs(),
                        "wms-tags-oracle",
                        false,
                        null,
                        null,
                        null)));
    }
}

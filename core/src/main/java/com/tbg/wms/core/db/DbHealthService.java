/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Utility service for validating Oracle database connectivity.
 *
 * <p>Used by the CLI during initialization and troubleshooting to confirm that
 * the database is reachable and responding normally. Uses a lightweight
 * {@code SELECT 1 FROM dual} query to validate.</p>
 */
public final class DbHealthService {

    private static final Logger log = LoggerFactory.getLogger(DbHealthService.class);

    private final DataSource ds;

    /**
     * Creates a new health check service.
     *
     * @param ds the data source to test (typically a HikariCP pool)
     */
    public DbHealthService(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Performs a simple connectivity check against the database.
     *
     * <p>This method executes {@code SELECT 1 FROM dual} and checks that the result
     * is received. Errors are logged but not thrown, so callers can easily determine
     * success/failure without exception handling.</p>
     *
     * @return {@code true} if the database responds correctly, {@code false} otherwise
     */
    public boolean ping() {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM dual");
             ResultSet rs = ps.executeQuery()) {

            boolean ok = rs.next() && rs.getInt(1) == 1;
            if (!ok) {
                log.warn("DB ping returned unexpected result");
            }
            return ok;
        } catch (Exception e) {
            log.error("DB ping failed", e);
            return false;
        }
    }
}

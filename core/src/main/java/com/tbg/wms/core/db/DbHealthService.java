package com.tbg.wms.core.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Small utility service used by the CLI to validate DB connectivity.
 */
public final class DbHealthService {

    private static final Logger log = LoggerFactory.getLogger(DbHealthService.class);

    private final DataSource ds;

    public DbHealthService(DataSource ds) {
        this.ds = ds;
    }

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

/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves and caches available human-readable description columns from WMSP.PRTMST.
 */
final class PrtmstDescriptionColumnResolver {

    private static final Logger log = LoggerFactory.getLogger(PrtmstDescriptionColumnResolver.class);
    private static final List<String> PREFERRED_ORDER = List.of("SHORT_DSC", "LNGDSC", "PRT_DISP", "PRT_DISPTN");

    private volatile List<String> cache;

    List<String> getColumns(Connection conn) {
        List<String> cached = cache;
        if (cached != null) {
            return cached;
        }
        List<String> resolved = resolveColumns(conn);
        cache = resolved;
        return resolved;
    }

    private List<String> resolveColumns(Connection conn) {
        final String sql = "SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = 'WMSP' AND TABLE_NAME = 'PRTMST'";
        Set<String> columns = new LinkedHashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                if (column != null) {
                    columns.add(column.toUpperCase());
                }
            }
        } catch (SQLException e) {
            log.warn("Could not inspect PRTMST columns via ALL_TAB_COLUMNS: {}", e.getMessage());
            return List.of();
        }

        List<String> available = new ArrayList<>();
        for (String candidate : PREFERRED_ORDER) {
            if (columns.contains(candidate)) {
                available.add(candidate);
            }
        }
        if (!available.isEmpty()) {
            return available;
        }

        // Fallback for restricted dictionary visibility: probe each candidate with a direct SELECT.
        for (String candidate : PREFERRED_ORDER) {
            if (canSelectPrtmstColumn(conn, candidate)) {
                available.add(candidate);
            }
        }
        return available;
    }

    private boolean canSelectPrtmstColumn(Connection conn, String column) {
        String sql = "SELECT " + column + " FROM WMSP.PRTMST WHERE ROWNUM = 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            // If execution succeeds, the column is accessible even if the table currently has no rows.
            return rs.next() || !rs.isBeforeFirst();
        } catch (SQLException e) {
            return false;
        }
    }
}

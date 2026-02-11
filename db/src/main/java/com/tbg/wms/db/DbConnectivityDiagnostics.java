/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.db;

import java.util.Objects;

/**
 * Diagnostic information returned by database connectivity tests.
 *
 * <p>Provides detailed metrics and status information about the database connection pool
 * and the database itself.</p>
 */
public final class DbConnectivityDiagnostics {
    private final boolean isConnected;
    private final long durationMs;
    private final int activeConnections;
    private final int idleConnections;
    private final String databaseVersion;
    private final String errorMessage;

    /**
     * Creates a new connectivity diagnostic record.
     *
     * @param isConnected whether the connection test succeeded
     * @param durationMs duration of the connection test in milliseconds
     * @param activeConnections number of active connections in the pool
     * @param idleConnections number of idle connections in the pool
     * @param databaseVersion database version string from metadata
     * @param errorMessage error message if connection failed, null if successful
     */
    public DbConnectivityDiagnostics(
            boolean isConnected,
            long durationMs,
            int activeConnections,
            int idleConnections,
            String databaseVersion,
            String errorMessage) {
        this.isConnected = isConnected;
        this.durationMs = durationMs;
        this.activeConnections = activeConnections;
        this.idleConnections = idleConnections;
        this.databaseVersion = databaseVersion;
        this.errorMessage = errorMessage;
    }

    /**
     * Gets whether the connection test succeeded.
     *
     * @return true if connected successfully, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Gets the duration of the connection test in milliseconds.
     *
     * @return duration in milliseconds
     */
    public long durationMs() {
        return durationMs;
    }

    /**
     * Gets the number of active connections in the pool.
     *
     * @return number of active connections
     */
    public int activeConnections() {
        return activeConnections;
    }

    /**
     * Gets the number of idle connections in the pool.
     *
     * @return number of idle connections
     */
    public int idleConnections() {
        return idleConnections;
    }

    /**
     * Gets the database version string from metadata.
     *
     * @return database version string, or null if not available
     */
    public String databaseVersion() {
        return databaseVersion;
    }

    /**
     * Gets the error message from a failed connection test.
     *
     * @return error message, or null if connection succeeded
     */
    public String errorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DbConnectivityDiagnostics)) return false;
        DbConnectivityDiagnostics that = (DbConnectivityDiagnostics) o;
        return isConnected == that.isConnected &&
                durationMs == that.durationMs &&
                activeConnections == that.activeConnections &&
                idleConnections == that.idleConnections &&
                Objects.equals(databaseVersion, that.databaseVersion) &&
                Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isConnected, durationMs, activeConnections, idleConnections, databaseVersion, errorMessage);
    }

    @Override
    public String toString() {
        return "DbConnectivityDiagnostics{" +
                "isConnected=" + isConnected +
                ", durationMs=" + durationMs +
                ", activeConnections=" + activeConnections +
                ", idleConnections=" + idleConnections +
                ", databaseVersion='" + databaseVersion + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}


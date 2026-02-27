/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.db;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Oracle database connection pool using HikariCP.
 *
 * <p>This service is responsible for:</p>
 * <ul>
 *   <li>Creating and configuring the HikariCP connection pool</li>
 *   <li>Validating connectivity with detailed error diagnostics</li>
 *   <li>Mapping raw SQL exceptions to {@link WmsDbConnectivityException} with remediation hints</li>
 *   <li>Providing connection lifecycle management (open/close)</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * AppConfig config = new AppConfig();
 * DbConnectionPool pool = new DbConnectionPool(config);
 * pool.testConnectivity(); // throws WmsDbConnectivityException on failure
 *
 * // Later, obtain connections via DataSource
 * DataSource ds = pool.getDataSource();
 * try (Connection c = ds.getConnection()) {
 *     // use connection
 * }
 *
 * // Finally, close pool at shutdown
 * pool.close();
 * }</pre>
 */
public final class DbConnectionPool implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DbConnectionPool.class);

    private final AppConfig config;
    private final HikariDataSource dataSource;
    private final String activeJdbcUrl;

    /**
     * Creates a new connection pool with the given configuration.
     *
     * <p>The pool is created immediately but not validated. Call {@link #testConnectivity()}
     * to verify the database is reachable.</p>
     *
     * @param config the application configuration containing Oracle connection details
     * @throws WmsDbConnectivityException if pool creation fails
     */
    public DbConnectionPool(AppConfig config) {
        this.config = config;
        List<String> errors = new ArrayList<>();

        HikariDataSource selectedDataSource = null;
        String selectedJdbcUrl = null;

        for (String jdbcUrlCandidate : config.oracleJdbcUrlCandidates()) {
            HikariDataSource candidate = null;
            try {
                candidate = createDataSource(config, jdbcUrlCandidate);
                Connection validationConnection = candidate.getConnection();
                validationConnection.close();
                selectedDataSource = candidate;
                selectedJdbcUrl = jdbcUrlCandidate;
                break;
            } catch (Exception e) {
                errors.add(summarizeAttemptFailure(jdbcUrlCandidate, e));
                log.warn("Database connection attempt failed for JDBC URL candidate: {}", jdbcUrlCandidate);
                if (isAuthenticationFailure(e)) {
                    // Prevent account lockout amplification by stopping URL fallbacks on auth failures.
                    break;
                }
                if (candidate != null && !candidate.isClosed()) {
                    candidate.close();
                }
            }
        }

        if (selectedDataSource == null || selectedJdbcUrl == null) {
            String details = errors.isEmpty() ? "No connection attempts were executed." : String.join(" | ", errors);
            throw new WmsDbConnectivityException(
                    "Failed to create connection pool: " + details,
                    "Verify Oracle credentials, DSN/TNS alias, and site endpoint configuration. " +
                            "If the account is locked, contact DB admin to unlock ORA-28000."
            );
        }

        this.dataSource = selectedDataSource;
        this.activeJdbcUrl = selectedJdbcUrl;
        log.info("Connection pool created: poolName={}, maxSize={}, jdbcUrl={}",
                config.activeSiteCode(), config.dbPoolMaxSize(), activeJdbcUrl);
    }

    /**
     * Creates and configures a HikariCP data source.
     *
     * @param config the application configuration
     * @return a configured HikariDataSource
     */
    private HikariDataSource createDataSource(AppConfig config, String jdbcUrl) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(jdbcUrl);
        hc.setUsername(config.oracleUsername());
        hc.setPassword(config.oraclePassword());

        hc.setMaximumPoolSize(config.dbPoolMaxSize());
        // Avoid eager prefill so invalid credentials don't fan out into multiple rapid login failures.
        hc.setMinimumIdle(0);
        hc.setConnectionTimeout(config.dbPoolConnectionTimeoutMs());
        hc.setValidationTimeout(config.dbPoolValidationTimeoutMs());
        // Defer physical connection creation until first explicit borrow in constructor validation.
        hc.setInitializationFailTimeout(-1);

        hc.setPoolName("wms-tags-oracle-" + config.activeSiteCode());
        hc.setAutoCommit(true);
        hc.setReadOnly(true);

        // Oracle best practice: lightweight validation query
        hc.setConnectionTestQuery("SELECT 1 FROM dual");

        // Optional: leak detection (logs warnings if connection held > 60s)
        hc.setLeakDetectionThreshold(60000);

        return new HikariDataSource(hc);
    }

    private String summarizeAttemptFailure(String jdbcUrl, Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return jdbcUrl + " -> " + root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private boolean isAuthenticationFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowered = message.toLowerCase();
                if (lowered.contains("ora-01017")
                        || lowered.contains("invalid username/password")
                        || lowered.contains("ora-28000")
                        || lowered.contains("account is locked")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Returns the underlying DataSource.
     *
     * @return the HikariDataSource for obtaining connections
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Tests connectivity to the database and returns detailed diagnostics.
     *
     * <p>This method executes a simple query (SELECT 1 FROM dual) to verify:</p>
     * <ul>
     *   <li>Host is reachable (DNS, network, firewall)</li>
     *   <li>Port is open</li>
     *   <li>Service/SID exists</li>
     *   <li>Credentials are valid</li>
     *   <li>Connection pool can create connections</li>
     * </ul>
     *
     * @return diagnostic information (pool stats, driver version, etc.)
     * @throws WmsDbConnectivityException if connectivity test fails
     */
    public DbConnectivityDiagnostics testConnectivity() {
        log.info("Testing database connectivity: site={}, env={}, host={}",
            config.activeSiteCode(), config.wmsEnvironment(), config.siteHost(config.activeSiteCode()));

        long startMs = System.currentTimeMillis();
        try (Connection c = dataSource.getConnection()) {
            long durationMs = System.currentTimeMillis() - startMs;

            DbConnectivityDiagnostics diag = new DbConnectivityDiagnostics(
                true,
                durationMs,
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                c.getMetaData().getDatabaseProductVersion(),
                null
            );

            log.info("Database connectivity verified: durationMs={}, poolActive={}, poolIdle={}",
                durationMs, diag.activeConnections(), diag.idleConnections());

            return diag;
        } catch (SQLException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            String error = e.getSQLState() + ": " + e.getMessage();

            String remediation = mapSqlErrorToRemediationHint(e);

            log.error("Database connectivity test failed: durationMs={}, sqlState={}, message={}",
                durationMs, e.getSQLState(), e.getMessage());

            throw new WmsDbConnectivityException(
                "Database connectivity test failed: " + error,
                e,
                remediation
            );
        }
    }

    /**
     * Maps SQL exceptions to actionable remediation hints.
     *
     * @param e the SQLException
     * @return a remediation hint string
     */
    private String mapSqlErrorToRemediationHint(SQLException e) {
        String sqlState = e.getSQLState();
        String message = e.getMessage() == null ? "" : e.getMessage();

        if (sqlState == null) {
            return "Check network connectivity, firewall, and VPN settings.";
        }

        // Oracle-specific SQL states
        if ("17002".equals(sqlState) || message.contains("Connection refused")) {
            return "Connection refused: check DB_HOST, ORACLE_PORT, and firewall. " +
                   "Verify VPN is connected and database service is running.";
        }
        if ("17004".equals(sqlState) || message.contains("Cannot create JDBC driver")) {
            return "Cannot load Oracle JDBC driver: check ojdbc11 is on classpath and version matches DB.";
        }
        if ("12514".equals(sqlState) || message.contains("listener does not currently know")) {
            return "Service not found: verify ORACLE_SERVICE=" + config.oracleService() + " is correct. " +
                   "Query DB admin for available services.";
        }
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("invalid username/password") || lowerMessage.contains("ora-01017")) {
            return "Authentication failed: verify ORACLE_USERNAME and ORACLE_PASSWORD are correct.";
        }
        if (lowerMessage.contains("ora-28000") || lowerMessage.contains("account is locked")) {
            return "Oracle account is locked (ORA-28000). " +
                    "Avoid repeated retries, validate DSN/TNS endpoint, then ask DB admin to unlock the account.";
        }
        if (e.getCause() != null
                && e.getCause().getMessage() != null
                && e.getCause().getMessage().contains("connection timed out")) {
            return "Connection timed out: check DB_HOST is reachable (ping), port " + config.oraclePort() +
                   " is open, and firewall allows traffic. Increase DB_POOL_CONN_TIMEOUT_MS if needed.";
        }

        return "Check database host, port, service name, and credentials. See INSTRUCTIONS.md for troubleshooting.";
    }

    /**
     * Closes the connection pool and releases all resources.
     * Should be called on application shutdown.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Closing database connection pool");
            dataSource.close();
        }
    }

    /**
     * Returns the JDBC URL that successfully created the pool.
     *
     * @return active JDBC URL
     */
    public String activeJdbcUrl() {
        return activeJdbcUrl;
    }
}


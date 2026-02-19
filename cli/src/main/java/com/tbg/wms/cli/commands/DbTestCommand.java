/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.exception.WmsConfigException;
import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.DbConnectivityDiagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine.Command;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Validates database connectivity for the active site and WMS environment.
 *
 * <p>This command is used during initial setup and troubleshooting to verify that:</p>
 * <ul>
 *   <li>Database host is reachable (DNS, network, firewall)</li>
 *   <li>Port is open and listening</li>
 *   <li>Service name/SID exists</li>
 *   <li>Credentials are valid</li>
 *   <li>Connection pool can be created</li>
 * </ul>
 *
 * <p>Exit codes:</p>
 * <ul>
 *   <li>0 - DB connectivity verified</li>
 *   <li>2 - Configuration error (missing env vars, invalid settings)</li>
 *   <li>3 - Database connectivity error (DNS, port, service, auth)</li>
 *   <li>10 - Unexpected internal error</li>
 * </ul>
 */
@Command(
        name = "db-test",
        description = "Validate DB connectivity for the active site + environment"
)
public final class DbTestCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(DbTestCommand.class);

    @Override
    public Integer call() {
        // Generate a unique job ID for this command execution
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("jobId", jobId);

        try {
            // Load configuration
            AppConfig config = RootCommand.config();
            String site = config.activeSiteCode();
            String env = config.wmsEnvironment();

            // Add to MDC for structured logging
            MDC.put("site", site);
            MDC.put("env", env);

            log.info("Starting database connectivity test");

            printHeader();
            printConfiguration(config, site);

            // Create connection pool (may fail with configuration errors)
            System.out.println("Attempting to create connection pool...");
            DbConnectionPool pool = new DbConnectionPool(config);

            // Test connectivity
            System.out.println("Testing connectivity...");
            DbConnectivityDiagnostics diag = pool.testConnectivity();
            String activeJdbcUrl = pool.activeJdbcUrl();

            pool.close();

            // Success
            printSuccess(diag, activeJdbcUrl);
            return 0;

        } catch (WmsConfigException e) {
            printConfigError(e);
            return e.getExitCode();

        } catch (WmsDbConnectivityException e) {
            printConnectivityError(e);
            return e.getExitCode();

        } catch (Exception e) {
            log.error("Unexpected error during db-test", e);
            System.err.println();
            System.err.println("ERROR");
            System.err.println();
            System.err.println("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            System.err.println();
            System.err.println("Contact the WMS team with this jobId: " + MDC.get("jobId"));
            System.err.println();
            return 10;

        } finally {
            MDC.remove("jobId");
            MDC.remove("site");
            MDC.remove("env");
        }
    }

    private void printHeader() {
        System.out.println();
        System.out.println("==========================================================");
        System.out.println("         Database Connectivity Test                       ");
        System.out.println("==========================================================");
        System.out.println();
    }

    private void printConfiguration(AppConfig config, String site) {
        System.out.println("Configuration:");
        System.out.println("  Active Site:     " + site + " (" + config.siteName(site) + ")");
        System.out.println("  WMS Environment: " + config.wmsEnvironment());
        System.out.println("  Database Host:   " + config.siteHost(site));
        System.out.println("  Database Port:   " + config.oraclePort());
        System.out.println("  Service Name:    " + config.oracleService());
        System.out.println("  Username:        " + config.oracleUsername());
        System.out.println("  JDBC URL:        " + config.oracleJdbcUrl());
        System.out.println("  ODBC/TNS Alias:  " + valueOrDash(config.oracleOdbcDsnOrNull()));
        System.out.println("  JDBC Candidates: " + String.join(" | ", config.oracleJdbcUrlCandidates()));
        System.out.println();
        System.out.println("Pool Configuration:");
        System.out.println("  Max Size:        " + config.dbPoolMaxSize());
        System.out.println("  Connect Timeout: " + config.dbPoolConnectionTimeoutMs() + " ms");
        System.out.println("  Validation Timeout: " + config.dbPoolValidationTimeoutMs() + " ms");
        System.out.println();
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private void printSuccess(DbConnectivityDiagnostics diag, String activeJdbcUrl) {
        System.out.println();
        System.out.println("Database connectivity verified!");
        System.out.println();
        System.out.println("Test Results:");
        System.out.println("  Duration:        " + diag.durationMs() + " ms");
        System.out.println("  Database Version: " + diag.databaseVersion());
        System.out.println("  Connected URL:   " + activeJdbcUrl);
        System.out.println("  Pool Statistics: " + diag.activeConnections() + " active, " +
                          diag.idleConnections() + " idle");
        System.out.println();
        System.out.println("The database is reachable and responding. You may proceed with label operations.");
        System.out.println();

        log.info("Database connectivity test succeeded: durationMs={}, dbVersion={}",
            diag.durationMs(), diag.databaseVersion());
    }

    private void printConfigError(WmsConfigException e) {
        System.err.println();
        System.err.println("Configuration Error");
        System.err.println();
        System.err.println("Error: " + e.getMessage());
        System.err.println();
        if (e.getRemediationHint() != null) {
            System.err.println("Remediation:");
            System.err.println("  " + e.getRemediationHint());
            System.err.println();
        }
        System.err.println("Verify your .env file or environment variables with:");
        System.err.println("  java -jar cli/target/cli-*.jar config");
        System.err.println();

        log.error("Database test failed due to configuration error: {}", e.getMessage(), e);
    }

    private void printConnectivityError(WmsDbConnectivityException e) {
        System.err.println();
        System.err.println("Database Connectivity Failed");
        System.err.println();
        System.err.println("Error: " + e.getMessage());
        System.err.println();
        if (e.getRemediationHint() != null) {
            System.err.println("Remediation:");
            System.err.println("  " + e.getRemediationHint());
            System.err.println();
        }
        System.err.println("Troubleshooting steps:");
        System.err.println("  1. Verify database host is reachable (ping, DNS)");
        System.err.println("  2. Check firewall/VPN and port is open");
        System.err.println("  3. Confirm ORACLE_SERVICE is correct");
        System.err.println("  4. Validate credentials with DB admin");
        System.err.println("  5. Increase DB_POOL_CONN_TIMEOUT_MS if network is slow");
        System.err.println();

        log.error("Database test failed: {}", e.getMessage(), e.getCause());
    }
}

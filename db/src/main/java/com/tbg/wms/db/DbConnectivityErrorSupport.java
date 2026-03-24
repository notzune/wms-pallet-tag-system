/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.db;

import com.tbg.wms.core.AppConfig;

import java.sql.SQLException;
import java.util.Locale;

/**
 * Maps Oracle/JDBC connection failures to retry policy and operator guidance.
 */
final class DbConnectivityErrorSupport {

    String summarizeAttemptFailure(String jdbcUrl, Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return jdbcUrl + " -> " + root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    boolean isAuthenticationFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowered = message.toLowerCase(Locale.ROOT);
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

    String remediationHint(SQLException exception, AppConfig config) {
        String sqlState = exception.getSQLState();
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        String lowerMessage = message.toLowerCase(Locale.ROOT);

        if (sqlState == null) {
            return "Check network connectivity, firewall, and VPN settings.";
        }
        if ("17002".equals(sqlState) || lowerMessage.contains("connection refused")) {
            return "Connection refused: check DB_HOST, ORACLE_PORT, and firewall. "
                    + "Verify VPN is connected and database service is running.";
        }
        if ("17004".equals(sqlState) || lowerMessage.contains("cannot create jdbc driver")) {
            return "Cannot load Oracle JDBC driver: check ojdbc11 is on classpath and version matches DB.";
        }
        if ("12514".equals(sqlState) || lowerMessage.contains("listener does not currently know")) {
            return "Service not found: verify ORACLE_SERVICE=" + config.oracleService() + " is correct. "
                    + "Query DB admin for available services.";
        }
        if (lowerMessage.contains("invalid username/password") || lowerMessage.contains("ora-01017")) {
            return "Authentication failed: verify ORACLE_USERNAME and ORACLE_PASSWORD are correct.";
        }
        if (lowerMessage.contains("ora-28000") || lowerMessage.contains("account is locked")) {
            return "Oracle account is locked (ORA-28000). "
                    + "Avoid repeated retries, validate DSN/TNS endpoint, then ask DB admin to unlock the account.";
        }
        if (exception.getCause() != null
                && exception.getCause().getMessage() != null
                && exception.getCause().getMessage().contains("connection timed out")) {
            return "Connection timed out: check DB_HOST is reachable (ping), port " + config.oraclePort()
                    + " is open, and firewall allows traffic. Increase DB_POOL_CONN_TIMEOUT_MS if needed.";
        }
        return "Check database host, port, service name, and credentials. See INSTRUCTIONS.md for troubleshooting.";
    }
}

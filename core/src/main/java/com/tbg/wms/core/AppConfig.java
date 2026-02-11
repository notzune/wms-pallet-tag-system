/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads and manages runtime configuration from environment variables and `.env` file.
 *
 * <p><strong>Configuration Precedence (highest to lowest):</strong></p>
 * <ol>
 *   <li>Environment variables (system environment)</li>
 *   <li>.env file in the working directory</li>
 *   <li>Code defaults (if any)</li>
 * </ol>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * AppConfig config = new AppConfig();
 * String site = config.activeSiteCode();
 * String host = config.siteHost(site);
 * }</pre>
 *
 * <p>If a required configuration key is missing, an {@link IllegalStateException} is thrown
 * with a clear error message indicating which key is missing.</p>
 *
 * @since 1.0.0
 */
public final class AppConfig {

    private final Dotenv env;

    /**
     * Creates a new configuration instance, loading values from environment and `.env` file.
     * If `.env` does not exist, configuration falls back to environment variables and defaults.
     */
    public AppConfig() {
        this.env = Dotenv.configure().ignoreIfMissing().load();
    }

    /**
     * Returns the active site code (required).
     * <p>Example: {@code TBG3002}</p>
     *
     * @return the site code from {@code ACTIVE_SITE} environment variable
     * @throws IllegalStateException if {@code ACTIVE_SITE} is not configured
     */
    public String activeSiteCode() {
        return required("ACTIVE_SITE");
    }

    /**
     * Returns the WMS environment (QA or PROD).
     *
     * @return the WMS environment from {@code WMS_ENV} (default: {@code QA})
     */
    public String wmsEnvironment() {
        return get("WMS_ENV", "QA").toUpperCase();
    }

    /**
     * Returns the Oracle database username (required).
     *
     * @return the username from {@code ORACLE_USERNAME}
     * @throws IllegalStateException if not configured
     */
    public String oracleUsername() {
        return required("ORACLE_USERNAME");
    }

    /**
     * Returns the Oracle database password (required, sensitive).
     *
     * @return the password from {@code ORACLE_PASSWORD}
     * @throws IllegalStateException if not configured
     */
    public String oraclePassword() {
        return required("ORACLE_PASSWORD");
    }

    /**
     * Returns the Oracle database port.
     *
     * @return the port from {@code ORACLE_PORT} (default: {@code 1521})
     */
    public int oraclePort() {
        return Integer.parseInt(get("ORACLE_PORT", "1521"));
    }

    /**
     * Returns the Oracle database service name or SID.
     *
     * @return the service from {@code ORACLE_SERVICE} (default: {@code WMSP})
     */
    public String oracleService() {
        return get("ORACLE_SERVICE", "WMSP");
    }

    /**
     * Returns the human-readable site name.
     * <p>Key format: {@code SITE_<CODE>_NAME}, e.g., {@code SITE_TBG3002_NAME}</p>
     *
     * @param siteCode the site code (e.g., {@code TBG3002})
     * @return the display name for the site
     * @throws IllegalStateException if the site name is not configured
     */
    public String siteName(String siteCode) {
        return required("SITE_" + siteCode + "_NAME");
    }

    /**
     * Returns the Oracle database host for a given site and environment.
     *
     * <p><strong>Key lookup order:</strong></p>
     * <ol>
     *   <li>Environment-scoped: {@code SITE_<CODE>_<ENV>_HOST} (e.g., {@code SITE_TBG3002_QA_HOST})</li>
     *   <li>Fallback: {@code SITE_<CODE>_HOST} (e.g., {@code SITE_TBG3002_HOST})</li>
     * </ol>
     *
     * @param siteCode the site code (e.g., {@code TBG3002})
     * @return the database host (IP or hostname)
     * @throws IllegalStateException if no host is configured for the site
     */
    public String siteHost(String siteCode) {
        String wmsEnv = wmsEnvironment();

        String scopedKey = "SITE_" + siteCode + "_" + wmsEnv + "_HOST";
        String scoped = env.get(scopedKey);
        if (scoped != null && !scoped.isBlank()) {
            return scoped.trim();
        }

        return required("SITE_" + siteCode + "_HOST");
    }

    /**
     * Returns the fully constructed JDBC URL for Oracle Thin driver.
     * <p>Format: {@code jdbc:oracle:thin:@//<host>:<port>/<service>}</p>
     *
     * @return the JDBC connection string
     */
    public String oracleJdbcUrl() {
        String site = activeSiteCode();
        return "jdbc:oracle:thin:@//" + siteHost(site) + ":" + oraclePort() + "/" + oracleService();
    }

    /**
     * Returns the maximum number of database connections in the pool.
     *
     * @return the max pool size from {@code DB_POOL_MAX_SIZE} (default: {@code 5})
     */
    public int dbPoolMaxSize() {
        return Integer.parseInt(get("DB_POOL_MAX_SIZE", "5"));
    }

    /**
     * Returns the connection timeout in milliseconds.
     *
     * @return the timeout from {@code DB_POOL_CONN_TIMEOUT_MS} (default: {@code 3000} ms)
     */
    public long dbPoolConnectionTimeoutMs() {
        return Long.parseLong(get("DB_POOL_CONN_TIMEOUT_MS", "3000"));
    }

    /**
     * Returns the validation query timeout in milliseconds.
     *
     * @return the timeout from {@code DB_POOL_VALIDATION_TIMEOUT_MS} (default: {@code 2000} ms)
     */
    public long dbPoolValidationTimeoutMs() {
        return Long.parseLong(get("DB_POOL_VALIDATION_TIMEOUT_MS", "2000"));
    }

    /**
     * Returns the path to the printer routing configuration file (YAML).
     *
     * @return the path from {@code PRINTER_ROUTING_FILE} (default: {@code config/printer-routing.yaml})
     */
    public String printerRoutingFile() {
        return get("PRINTER_ROUTING_FILE", "config/printer-routing.yaml");
    }

    /**
     * Returns the default printer ID to use when no routing rule matches.
     *
     * @return the printer ID from {@code PRINTER_DEFAULT_ID} (default: {@code DISPATCH})
     */
    public String defaultPrinterId() {
        return get("PRINTER_DEFAULT_ID", "DISPATCH");
    }

    /**
     * Returns the forced printer ID (used for testing), or {@code null} if not set.
     * <p>When set, this printer ID overrides all routing rules.</p>
     *
     * @return the forced printer ID from {@code PRINTER_FORCE_ID}, or {@code null}
     */
    public String forcedPrinterIdOrNull() {
        String v = env.get("PRINTER_FORCE_ID");
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /**
     * Internal helper: retrieves a required configuration value.
     *
     * @param key the configuration key
     * @return the trimmed value
     * @throws IllegalStateException if the key is not set or is blank
     */
    private String required(String key) {
        String v = env.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required .env key: " + key);
        }
        return v.trim();
    }

    /**
     * Internal helper: retrieves an optional configuration value with a default.
     *
     * @param key the configuration key
     * @param def the default value if key is not set
     * @return the trimmed value, or the default
     */
    private String get(String key, String def) {
        String v = env.get(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }
}

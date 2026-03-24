/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private static final String CONFIG_FILE_ENV = "WMS_CONFIG_FILE";
    private static final String CONFIG_FILE_PROP = "wms.config.file";
    private static final String DEFAULT_FILE_NAME = "wms-tags.env";

    private final Map<String, String> envVars;
    private final Map<String, String> fileValues;
    private final Map<String, String> classpathDefaults;
    private final ConfigValueSupport valueSupport;
    private final OracleConnectionConfigSupport oracleConnectionSupport;
    private final PrintRuntimeConfigSupport printRuntimeSupport;
    private final String loadedConfigFile;

    /**
     * Creates a new configuration instance, loading values from environment and `.env` file.
     * If `.env` does not exist, configuration falls back to environment variables and defaults.
     */
    public AppConfig() {
        this(System.getenv(), null);
    }

    /**
     * Creates a configuration instance using a provided environment map and optional explicit config file.
     *
     * <p>Primarily intended for deterministic tests where host process environment and working-directory
     * `.env` files must be ignored.</p>
     *
     * @param envVars            environment key/value pairs to use for resolution precedence
     * @param explicitConfigFile explicit env-style config file, or {@code null} to use normal discovery
     */
    AppConfig(Map<String, String> envVars, Path explicitConfigFile) {
        this.envVars = Map.copyOf(Objects.requireNonNull(envVars, "envVars cannot be null"));
        this.classpathDefaults = ConfigSourceLoader.loadClasspathDefaults(AppConfig.class, "wms-defaults.properties");

        Path explicitPath = explicitConfigFile != null
                ? ConfigFileLocator.validateConfigFile(explicitConfigFile)
                : ConfigFileLocator.resolveExplicitConfigPath(CONFIG_FILE_PROP, CONFIG_FILE_ENV);
        Path selectedFile = explicitPath != null
                ? explicitPath
                : ConfigFileLocator.discoverConfigFile(DEFAULT_FILE_NAME, AppConfig.class, this.envVars);
        this.fileValues = selectedFile == null ? Map.of() : ConfigSourceLoader.loadEnvStyleFile(selectedFile);
        this.valueSupport = new ConfigValueSupport(this.envVars, this.fileValues, this.classpathDefaults, DEFAULT_FILE_NAME);
        this.oracleConnectionSupport = new OracleConnectionConfigSupport(
                valueSupport::raw,
                valueSupport::rawFromEnvOrFile,
                valueSupport::required
        );
        this.printRuntimeSupport = new PrintRuntimeConfigSupport(valueSupport);
        this.loadedConfigFile = selectedFile == null ? null : selectedFile.toAbsolutePath().toString();
    }

    /**
     * Returns the active site code (required).
     * <p>Example: {@code TBG3002}</p>
     *
     * @return the site code from {@code ACTIVE_SITE} environment variable
     * @throws IllegalStateException if {@code ACTIVE_SITE} is not configured
     */
    public String activeSiteCode() {
        return valueSupport.required("ACTIVE_SITE");
    }

    /**
     * Returns the WMS environment (QA or PROD).
     *
     * @return the WMS environment from {@code WMS_ENV} (default: {@code PROD})
     */
    public String wmsEnvironment() {
        return oracleConnectionSupport.resolveWmsEnvironment();
    }

    /**
     * Returns the Oracle database username (required).
     *
     * @return the username from {@code ORACLE_USERNAME}
     * @throws IllegalStateException if not configured
     */
    public String oracleUsername() {
        return valueSupport.required("ORACLE_USERNAME");
    }

    /**
     * Returns the Oracle database password (required, sensitive).
     *
     * @return the password from {@code ORACLE_PASSWORD}
     * @throws IllegalStateException if not configured
     */
    public String oraclePassword() {
        return valueSupport.required("ORACLE_PASSWORD");
    }

    /**
     * Returns the Oracle database port.
     *
     * @return the port from {@code ORACLE_PORT} (default: {@code 1521})
     */
    public int oraclePort() {
        return valueSupport.parseInt("ORACLE_PORT", "1521");
    }

    /**
     * Returns the Oracle database service name or SID.
     *
     * @return the service from {@code ORACLE_SERVICE} (default: {@code WMSP})
     */
    public String oracleService() {
        return valueSupport.get("ORACLE_SERVICE", "WMSP");
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
        return valueSupport.required("SITE_" + siteCode + "_NAME");
    }

    /**
     * Returns the ship-from name for the given site.
     *
     * @param siteCode the site code (e.g., {@code TBG3002})
     * @return ship-from company name
     */
    public String siteShipFromName(String siteCode) {
        return valueSupport.get("SITE_" + siteCode + "_SHIP_FROM_NAME", "TROPICANA PRODUCTS, INC.");
    }

    /**
     * Returns the ship-from street address for the given site.
     *
     * @param siteCode the site code (e.g., {@code TBG3002})
     * @return ship-from street address
     */
    public String siteShipFromAddress(String siteCode) {
        return valueSupport.get("SITE_" + siteCode + "_SHIP_FROM_ADDRESS", "9 Linden Ave E");
    }

    /**
     * Returns the ship-from city/state/zip for the given site.
     *
     * @param siteCode the site code (e.g., {@code TBG3002})
     * @return ship-from city/state/zip line
     */
    public String siteShipFromCityStateZip(String siteCode) {
        return valueSupport.get("SITE_" + siteCode + "_SHIP_FROM_CITY_STATE_ZIP", "Jersey City, NJ 07305");
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
        return oracleConnectionSupport.resolveSiteHost(siteCode);
    }

    /**
     * Returns the fully constructed JDBC URL for Oracle Thin driver.
     * <p>Format: {@code jdbc:oracle:thin:@//<host>:<port>/<service>}</p>
     *
     * @return the JDBC connection string
     */
    public String oracleJdbcUrl() {
        return oracleConnectionSupport.resolveJdbcUrl(activeSiteCode(), this::oraclePort, this::oracleService);
    }

    /**
     * Returns JDBC URL candidates ordered for resilient connection fallback.
     *
     * <p>Order:</p>
     * <ol>
     *   <li>Primary JDBC URL (same value as {@link #oracleJdbcUrl()})</li>
     *   <li>Oracle Net alias from {@code ORACLE_ODBC_DSN}/{@code ORACLE_NET_SERVICE}/{@code ORACLE_TNS_ALIAS}</li>
     *   <li>Host/port/service URL fallback</li>
     * </ol>
     *
     * <p>The Oracle Net alias path relies on workstation Oracle client/TNS setup used by ODBC analyzers.</p>
     *
     * @return distinct, ordered JDBC URL candidates
     */
    public List<String> oracleJdbcUrlCandidates() {
        return oracleConnectionSupport.resolveJdbcUrlCandidates(activeSiteCode(), this::oraclePort, this::oracleService);
    }

    /**
     * Returns the configured Oracle Net alias (ODBC/TNS-style), or {@code null}.
     *
     * <p>Lookup order:</p>
     * <ol>
     *   <li>{@code ORACLE_ODBC_DSN}</li>
     *   <li>{@code ORACLE_NET_SERVICE}</li>
     *   <li>{@code ORACLE_TNS_ALIAS}</li>
     * </ol>
     *
     * @return Oracle Net alias used for fallback connectivity
     */
    public String oracleOdbcDsnOrNull() {
        return oracleConnectionSupport.resolveOdbcAliasOrNull();
    }

    /**
     * Returns the maximum number of database connections in the pool.
     *
     * @return the max pool size from {@code DB_POOL_MAX_SIZE} (default: {@code 5})
     */
    public int dbPoolMaxSize() {
        return valueSupport.parseInt("DB_POOL_MAX_SIZE", "5");
    }

    /**
     * Returns the connection timeout in milliseconds.
     *
     * @return the timeout from {@code DB_POOL_CONN_TIMEOUT_MS} (default: {@code 3000} ms)
     */
    public long dbPoolConnectionTimeoutMs() {
        return valueSupport.parseLong("DB_POOL_CONN_TIMEOUT_MS", "3000");
    }

    /**
     * Returns the validation query timeout in milliseconds.
     *
     * @return the timeout from {@code DB_POOL_VALIDATION_TIMEOUT_MS} (default: {@code 2000} ms)
     */
    public long dbPoolValidationTimeoutMs() {
        return valueSupport.parseLong("DB_POOL_VALIDATION_TIMEOUT_MS", "2000");
    }

    /**
     * Returns the path to the printer routing configuration file (YAML).
     *
     * @return the path from {@code PRINTER_ROUTING_FILE} (default: {@code config/printer-routing.yaml})
     */
    public String printerRoutingFile() {
        return printRuntimeSupport.printerRoutingFile();
    }

    /**
     * Returns the default printer ID to use when no routing rule matches.
     *
     * @return the printer ID from {@code PRINTER_DEFAULT_ID} (default: {@code DISPATCH})
     */
    public String defaultPrinterId() {
        return printRuntimeSupport.defaultPrinterId();
    }

    /**
     * Returns the default rail printer ID override, or {@code null} if not set.
     * <p>When set, rail PDF printing uses this printer target first.</p>
     *
     * @return the rail default printer ID from {@code RAIL_DEFAULT_PRINTER_ID}, or {@code null}
     */
    public String railDefaultPrinterIdOrNull() {
        return printRuntimeSupport.railDefaultPrinterIdOrNull();
    }

    /**
     * Horizontal center-gap between the two rail label columns, in inches.
     *
     * @return center gap from {@code RAIL_LABEL_CENTER_GAP_IN} (default: {@code 0.125})
     */
    public double railLabelCenterGapInches() {
        return printRuntimeSupport.railLabelCenterGapInches();
    }

    /**
     * Rail label horizontal calibration offset in inches.
     * Positive values move the entire 2x5 grid to the right.
     *
     * @return offset from {@code RAIL_LABEL_OFFSET_X_IN} (default: {@code 0.02})
     */
    public double railLabelOffsetXInches() {
        return printRuntimeSupport.railLabelOffsetXInches();
    }

    /**
     * Rail label vertical calibration offset in inches.
     * Positive values move the entire 2x5 grid downward.
     *
     * @return offset from {@code RAIL_LABEL_OFFSET_Y_IN} (default: {@code 0.02})
     */
    public double railLabelOffsetYInches() {
        return printRuntimeSupport.railLabelOffsetYInches();
    }

    /**
     * Returns the forced printer ID (used for testing), or {@code null} if not set.
     * <p>When set, this printer ID overrides all routing rules.</p>
     *
     * @return the forced printer ID from {@code PRINTER_FORCE_ID}, or {@code null}
     */
    public String forcedPrinterIdOrNull() {
        return printRuntimeSupport.forcedPrinterIdOrNull();
    }

    /**
     * Returns the resolved external configuration file path, if one was found.
     *
     * @return absolute path of the loaded config file, or {@code null} if none
     */
    public String loadedConfigFileOrNull() {
        return loadedConfigFile;
    }
}

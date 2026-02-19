/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final String loadedConfigFile;

    /**
     * Creates a new configuration instance, loading values from environment and `.env` file.
     * If `.env` does not exist, configuration falls back to environment variables and defaults.
     */
    public AppConfig() {
        this.envVars = System.getenv();
        this.classpathDefaults = loadClasspathDefaults();

        Path explicitPath = resolveExplicitConfigPath();
        Path selectedFile = explicitPath != null ? explicitPath : discoverConfigFile();
        this.fileValues = selectedFile == null ? Map.of() : loadEnvStyleFile(selectedFile);
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
        return required("ACTIVE_SITE");
    }

    /**
     * Returns the WMS environment (QA or PROD).
     *
     * @return the WMS environment from {@code WMS_ENV} (default: {@code PROD})
     */
    public String wmsEnvironment() {
        String value = rawFromEnvOrFile("WMS_ENV");
        if (value == null || value.isBlank()) {
            value = rawFromEnvOrFile("ACTIVE_ENV");
        }
        if (value == null || value.isBlank()) {
            value = raw("WMS_ENV");
        }
        if (value == null || value.isBlank()) {
            value = raw("ACTIVE_ENV");
        }
        if (value == null || value.isBlank()) {
            value = "PROD";
        }
        return value.trim().toUpperCase(Locale.ROOT);
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
     * Returns the ship-from name for the given site.
     *
     * @param siteCode the site code (e.g., {@code TBG3002})
     * @return ship-from company name
     */
    public String siteShipFromName(String siteCode) {
        return get("SITE_" + siteCode + "_SHIP_FROM_NAME", "TROPICANA PRODUCTS, INC.");
    }

    /**
     * Returns the ship-from street address for the given site.
     *
     * @param siteCode the site code (e.g., {@code TBG3002})
     * @return ship-from street address
     */
    public String siteShipFromAddress(String siteCode) {
        return get("SITE_" + siteCode + "_SHIP_FROM_ADDRESS", "9 Linden Ave E");
    }

    /**
     * Returns the ship-from city/state/zip for the given site.
     *
     * @param siteCode the site code (e.g., {@code TBG3002})
     * @return ship-from city/state/zip line
     */
    public String siteShipFromCityStateZip(String siteCode) {
        return get("SITE_" + siteCode + "_SHIP_FROM_CITY_STATE_ZIP", "Jersey City, NJ 07305");
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
        String scoped = raw(scopedKey);
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
        String explicitJdbc = raw("ORACLE_JDBC_URL");
        if (explicitJdbc != null && !explicitJdbc.isBlank()) {
            return explicitJdbc.trim();
        }

        String dsn = raw("ORACLE_DSN");
        if (dsn != null && !dsn.isBlank()) {
            String trimmed = dsn.trim();
            if (trimmed.startsWith("jdbc:")) {
                return trimmed;
            }
            return "jdbc:oracle:thin:@" + trimmed;
        }

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
        String v = raw("PRINTER_FORCE_ID");
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /**
     * Returns the resolved external configuration file path, if one was found.
     *
     * @return absolute path of the loaded config file, or {@code null} if none
     */
    public String loadedConfigFileOrNull() {
        return loadedConfigFile;
    }

    /**
     * Internal helper: retrieves a required configuration value.
     *
     * @param key the configuration key
     * @return the trimmed value
     * @throws IllegalStateException if the key is not set or is blank
     */
    private String required(String key) {
        String v = raw(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required config key: " + key
                    + " (set env var, or define in " + DEFAULT_FILE_NAME + "/.env)");
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
        String v = raw(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private String raw(String key) {
        String fromEnv = envVars.get(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String fromFile = fileValues.get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }

        String fromDefaults = classpathDefaults.get(key);
        return (fromDefaults == null || fromDefaults.isBlank()) ? null : fromDefaults.trim();
    }

    private String rawFromEnvOrFile(String key) {
        String fromEnv = envVars.get(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String fromFile = fileValues.get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return null;
    }

    private Path resolveExplicitConfigPath() {
        String explicit = System.getProperty(CONFIG_FILE_PROP);
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv(CONFIG_FILE_ENV);
        }
        if (explicit == null || explicit.isBlank()) {
            return null;
        }

        Path path = Paths.get(explicit.trim());
        if (Files.exists(path) && Files.isRegularFile(path)) {
            return path;
        }
        throw new IllegalStateException("Configured file not found: " + explicit.trim());
    }

    private Path discoverConfigFile() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(DEFAULT_FILE_NAME));
        candidates.add(Paths.get(".env"));
        candidates.add(Paths.get("config", DEFAULT_FILE_NAME));

        Path executableDir = resolveExecutableDirectory();
        if (executableDir != null) {
            candidates.add(executableDir.resolve(DEFAULT_FILE_NAME));
            candidates.add(executableDir.resolve(".env"));
            candidates.add(executableDir.resolve("config").resolve(DEFAULT_FILE_NAME));
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Path resolveExecutableDirectory() {
        try {
            CodeSource codeSource = AppConfig.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }

            URI locationUri = codeSource.getLocation().toURI();
            Path location = Paths.get(locationUri);
            if (Files.isDirectory(location)) {
                return location;
            }
            Path parent = location.getParent();
            return parent == null ? null : parent;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, String> loadClasspathDefaults() {
        InputStream stream = AppConfig.class.getClassLoader().getResourceAsStream("wms-defaults.properties");
        if (stream == null) {
            return Map.of();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return loadEnvStyleReader(reader);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Map<String, String> loadEnvStyleFile(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return loadEnvStyleLines(lines);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config file: " + path, e);
        }
    }

    private Map<String, String> loadEnvStyleReader(BufferedReader reader) throws IOException {
        Map<String, String> values = new HashMap<>();
        String rawLine;
        while ((rawLine = reader.readLine()) != null) {
            parseEnvStyleLine(values, rawLine);
        }
        return values;
    }

    private Map<String, String> loadEnvStyleLines(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String rawLine : lines) {
            parseEnvStyleLine(values, rawLine);
        }

        return values;
    }

    private void parseEnvStyleLine(Map<String, String> values, String rawLine) {
        if (rawLine == null) {
            return;
        }

        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        if (line.startsWith("export ")) {
            // Accept shell-style .env files that prefix entries with `export`.
            line = line.substring("export ".length()).trim();
        }

        int sep = line.indexOf('=');
        if (sep <= 0) {
            return;
        }

        String key = line.substring(0, sep).trim();
        String value = line.substring(sep + 1).trim();
        if (key.isEmpty()) {
            return;
        }

        values.put(key, stripQuotes(value));
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}

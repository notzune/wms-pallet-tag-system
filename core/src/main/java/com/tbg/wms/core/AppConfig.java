package com.tbg.wms.core;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads runtime configuration from a {@code .env} file.
 * <p>
 * The goal is to keep the config format simple for local prototyping, while still
 * supporting future-proof keys (site + environment scoping).
 */
public final class AppConfig {

    private final Dotenv env;

    public AppConfig() {
        this.env = Dotenv.configure().ignoreIfMissing().load();
    }

    public String activeSiteCode() {
        return required("ACTIVE_SITE");
    }

    /**
     * Which WMS environment to use (QA or PROD).
     */
    public String wmsEnvironment() {
        return get("WMS_ENV", "QA").toUpperCase();
    }

    public String oracleUsername() {
        return required("ORACLE_USERNAME");
    }

    public String oraclePassword() {
        return required("ORACLE_PASSWORD");
    }

    public int oraclePort() {
        return Integer.parseInt(get("ORACLE_PORT", "1521"));
    }

    public String oracleService() {
        return get("ORACLE_SERVICE", "WMSP");
    }

    public String siteName(String siteCode) {
        return required("SITE_" + siteCode + "_NAME");
    }

    /**
     * Returns the Oracle host for a site.
     * <p>
     * Preferred keys (environment-scoped):
     * <ul>
     *   <li>{@code SITE_TBG3002_QA_HOST}</li>
     *   <li>{@code SITE_TBG3002_PROD_HOST}</li>
     * </ul>
     * Fallback key (legacy):
     * <ul>
     *   <li>{@code SITE_TBG3002_HOST}</li>
     * </ul>
     */
    public String siteHost(String siteCode) {
        String wmsEnv = wmsEnvironment();

        String scopedKey = "SITE_" + siteCode + "_" + env + "_HOST";
        String scoped = env.get(scopedKey);
        if (scoped != null && !scoped.isBlank()) {
            return scoped.trim();
        }

        return required("SITE_" + siteCode + "_HOST");
    }

    /**
     * Canonical JDBC URL for Oracle Thin driver.
     */
    public String oracleJdbcUrl() {
        String site = activeSiteCode();
        return "jdbc:oracle:thin:@//" + siteHost(site) + ":" + oraclePort() + "/" + oracleService();
    }

    public int dbPoolMaxSize() {
        return Integer.parseInt(get("DB_POOL_MAX_SIZE", "5"));
    }

    public long dbPoolConnectionTimeoutMs() {
        return Long.parseLong(get("DB_POOL_CONN_TIMEOUT_MS", "3000"));
    }

    public long dbPoolValidationTimeoutMs() {
        return Long.parseLong(get("DB_POOL_VALIDATION_TIMEOUT_MS", "2000"));
    }

    public String printerRoutingFile() {
        return get("PRINTER_ROUTING_FILE", "config/printer-routing.yaml");
    }

    public String defaultPrinterId() {
        return get("PRINTER_DEFAULT_ID", "DISPATCH");
    }

    public String forcedPrinterIdOrNull() {
        String v = env.get("PRINTER_FORCE_ID");
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private String required(String key) {
        String v = env.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required .env key: " + key);
        }
        return v.trim();
    }

    private String get(String key, String def) {
        String v = env.get(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }
}

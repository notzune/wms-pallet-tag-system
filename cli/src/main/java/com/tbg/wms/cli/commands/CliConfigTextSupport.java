package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;

/**
 * Shared text rendering for CLI commands that display resolved runtime configuration.
 */
final class CliConfigTextSupport {

    ConfigSnapshot snapshot(AppConfig config, String site) {
        return new ConfigSnapshot(
                site,
                config.siteName(site),
                config.wmsEnvironment(),
                config.loadedConfigFileOrNull() == null ? "(none)" : config.loadedConfigFileOrNull(),
                config.siteHost(site),
                config.oraclePort(),
                config.oracleService(),
                config.oracleUsername(),
                redact(config.oraclePassword()),
                config.oracleJdbcUrl(),
                valueOrDash(config.oracleOdbcDsnOrNull()),
                String.join(" | ", config.oracleJdbcUrlCandidates()),
                config.dbPoolMaxSize(),
                config.dbPoolConnectionTimeoutMs(),
                config.dbPoolValidationTimeoutMs(),
                config.printerRoutingFile(),
                config.defaultPrinterId(),
                config.forcedPrinterIdOrNull() == null ? "(none)" : config.forcedPrinterIdOrNull()
        );
    }

    String buildConfigReport(ConfigSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        sb.append("=== WMS Pallet Tag System Configuration ===\n\n");
        appendSiteConfiguration(sb, snapshot);
        appendEnvironmentSection(sb, snapshot);
        appendDatabaseSection(sb, snapshot, "Database Configuration:");
        appendPoolSection(sb, snapshot, "Connection Pool:");
        appendPrinterSection(sb, snapshot);
        sb.append("=== Configuration Verified ===\n\n");
        return sb.toString();
    }

    String buildDbTestConfiguration(ConfigSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        appendDbTestHeader(sb);
        appendDbTestConfiguration(sb, snapshot);
        return sb.toString();
    }

    private void appendSiteConfiguration(StringBuilder sb, ConfigSnapshot snapshot) {
        sb.append("Site Configuration:\n");
        sb.append("  Active Site:     ").append(snapshot.site()).append('\n');
        sb.append("  Site Name:       ").append(snapshot.siteName()).append("\n\n");
    }

    private void appendEnvironmentSection(StringBuilder sb, ConfigSnapshot snapshot) {
        sb.append("WMS Environment:\n");
        sb.append("  Environment:     ").append(snapshot.environment()).append('\n');
        sb.append("  Config File:     ").append(snapshot.configFile()).append("\n\n");
    }

    private void appendDatabaseSection(StringBuilder sb, ConfigSnapshot snapshot, String heading) {
        sb.append(heading).append('\n');
        sb.append("  Host:            ").append(snapshot.host()).append('\n');
        sb.append("  Port:            ").append(snapshot.port()).append('\n');
        sb.append("  Service:         ").append(snapshot.service()).append('\n');
        sb.append("  Username:        ").append(snapshot.username()).append('\n');
        sb.append("  Password:        ").append(snapshot.redactedPassword()).append('\n');
        sb.append("  JDBC URL:        ").append(snapshot.jdbcUrl()).append('\n');
        sb.append("  ODBC/TNS Alias:  ").append(snapshot.odbcAlias()).append('\n');
        sb.append("  JDBC Candidates: ").append(snapshot.jdbcCandidates()).append("\n\n");
    }

    private void appendPoolSection(StringBuilder sb, ConfigSnapshot snapshot, String heading) {
        sb.append(heading).append('\n');
        sb.append("  Max Size:        ").append(snapshot.poolMaxSize()).append('\n');
        sb.append("  Connect Timeout: ").append(snapshot.connectTimeoutMs()).append(" ms\n");
        sb.append("  Validation Timeout: ").append(snapshot.validationTimeoutMs()).append(" ms\n\n");
    }

    private void appendPrinterSection(StringBuilder sb, ConfigSnapshot snapshot) {
        sb.append("Printer Configuration:\n");
        sb.append("  Routing File:    ").append(snapshot.printerRoutingFile()).append('\n');
        sb.append("  Default Printer: ").append(snapshot.defaultPrinter()).append('\n');
        sb.append("  Forced Printer:  ").append(snapshot.forcedPrinter()).append("\n\n");
    }

    private void appendDbTestHeader(StringBuilder sb) {
        sb.append('\n');
        sb.append("==========================================================\n");
        sb.append("         Database Connectivity Test                       \n");
        sb.append("==========================================================\n\n");
    }

    private void appendDbTestConfiguration(StringBuilder sb, ConfigSnapshot snapshot) {
        sb.append("Configuration:\n");
        sb.append("  Active Site:     ").append(snapshot.site())
                .append(" (").append(snapshot.siteName()).append(")\n");
        sb.append("  WMS Environment: ").append(snapshot.environment()).append('\n');
        sb.append("  Database Host:   ").append(snapshot.host()).append('\n');
        sb.append("  Database Port:   ").append(snapshot.port()).append('\n');
        sb.append("  Service Name:    ").append(snapshot.service()).append('\n');
        sb.append("  Username:        ").append(snapshot.username()).append('\n');
        sb.append("  JDBC URL:        ").append(snapshot.jdbcUrl()).append('\n');
        sb.append("  ODBC/TNS Alias:  ").append(snapshot.odbcAlias()).append('\n');
        sb.append("  JDBC Candidates: ").append(snapshot.jdbcCandidates()).append("\n\n");
        appendPoolSection(sb, snapshot, "Pool Configuration:");
    }

    private String redact(String value) {
        if (value == null || value.isEmpty()) {
            return "(not set)";
        }
        return "***" + (value.length() > 3 ? value.substring(value.length() - 3) : "***");
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    record ConfigSnapshot(
            String site,
            String siteName,
            String environment,
            String configFile,
            String host,
            int port,
            String service,
            String username,
            String redactedPassword,
            String jdbcUrl,
            String odbcAlias,
            String jdbcCandidates,
            int poolMaxSize,
            long connectTimeoutMs,
            long validationTimeoutMs,
            String printerRoutingFile,
            String defaultPrinter,
            String forcedPrinter
    ) {
    }
}

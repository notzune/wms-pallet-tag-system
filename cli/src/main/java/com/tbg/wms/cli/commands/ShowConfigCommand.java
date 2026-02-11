/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Displays the resolved runtime configuration with secrets redacted.
 * Useful for troubleshooting configuration precedence and verifying settings
 * without exposing sensitive values in logs.
 */
@Command(
        name = "config",
        description = "Print resolved runtime config (secrets redacted)."
)
public final class ShowConfigCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            AppConfig cfg = RootCommand.config();
            String site = cfg.activeSiteCode();

            System.out.println();
            System.out.println("=== WMS Pallet Tag System Configuration ===");
            System.out.println();
            System.out.println("Site Configuration:");
            System.out.println("  Active Site:     " + site);
            System.out.println("  Site Name:       " + cfg.siteName(site));
            System.out.println();
            System.out.println("WMS Environment:");
            System.out.println("  Environment:     " + cfg.wmsEnvironment());
            System.out.println();
            System.out.println("Database Configuration:");
            System.out.println("  Host:            " + cfg.siteHost(site));
            System.out.println("  Port:            " + cfg.oraclePort());
            System.out.println("  Service:         " + cfg.oracleService());
            System.out.println("  Username:        " + cfg.oracleUsername());
            System.out.println("  Password:        " + redact(cfg.oraclePassword()));
            System.out.println("  JDBC URL:        " + cfg.oracleJdbcUrl());
            System.out.println();
            System.out.println("Connection Pool:");
            System.out.println("  Max Size:        " + cfg.dbPoolMaxSize());
            System.out.println("  Connect Timeout: " + cfg.dbPoolConnectionTimeoutMs() + " ms");
            System.out.println("  Validation Timeout: " + cfg.dbPoolValidationTimeoutMs() + " ms");
            System.out.println();
            System.out.println("Printer Configuration:");
            System.out.println("  Routing File:    " + cfg.printerRoutingFile());
            System.out.println("  Default Printer: " + cfg.defaultPrinterId());
            String forcedId = cfg.forcedPrinterIdOrNull();
            System.out.println("  Forced Printer:  " + (forcedId == null ? "(none)" : forcedId));
            System.out.println();
            System.out.println("=== Configuration Verified ===");
            System.out.println();

            return 0;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 2; // User input/config error
        }
    }

    /**
     * Redacts sensitive values for safe display in logs.
     * Returns a placeholder string for any non-null, non-empty value.
     *
     * @param value the value to redact
     * @return a redacted representation, or "(not set)" if value is null/empty
     */
    private static String redact(String value) {
        if (value == null || value.isEmpty()) {
            return "(not set)";
        }
        return "***" + (value.length() > 3 ? value.substring(value.length() - 3) : "***");
    }
}

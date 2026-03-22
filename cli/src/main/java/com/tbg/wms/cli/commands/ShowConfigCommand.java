/*
 * Copyright (c) 2026 Tropicana Brands Group
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
    private final CliConfigTextSupport configTextSupport = new CliConfigTextSupport();

    /**
     * Prints effective runtime configuration values with secrets redacted.
     *
     * @return exit code (0 success, 10 unexpected failure)
     */
    @Override
    public Integer call() {
        try {
            AppConfig cfg = RootCommand.config();
            String site = cfg.activeSiteCode();
            System.out.print(configTextSupport.buildConfigReport(configTextSupport.snapshot(cfg, site)));

            return 0;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 2; // User input/config error
        }
    }
}

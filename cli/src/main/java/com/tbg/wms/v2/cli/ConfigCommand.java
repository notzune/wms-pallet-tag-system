package com.tbg.wms.v2.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Provides config command behavior for WMS 2.0 workflows.
 */
@Command(name = "config", description = "Show effective runtime configuration")
final class ConfigCommand implements Callable<Integer> {
    @ParentCommand
    private WmsCli root;

    @Spec
    private CommandSpec spec;

    /**
     * Runs the command and returns a process-style exit code.
     *
     * @return process-style exit code
     */
    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        RuntimeConfigView config = root.dependencies().config();
        out.println("=== WMS Pallet Tag System 2.0 Configuration ===");
        out.println("Site: " + config.siteCode());
        out.println("Environment: " + config.environment());
        out.println("Loaded config files:");
        if (config.loadedConfigFiles().isEmpty()) {
            out.println("  (none)");
        } else {
            for (Path file : config.loadedConfigFiles()) {
                out.println("  " + file);
            }
        }
        out.println();
        out.println("Database Configuration:");
        out.println("  JDBC URL: " + config.oracleJdbcUrl());
        out.println("  Username: " + config.oracleUsername());
        out.println("  Password: " + redact(config.oraclePassword()));
        out.println("=== Configuration Verified ===");
        return 0;
    }

    private static String redact(String value) {
        return value == null || value.isBlank() ? "(not configured)" : "********";
    }
}

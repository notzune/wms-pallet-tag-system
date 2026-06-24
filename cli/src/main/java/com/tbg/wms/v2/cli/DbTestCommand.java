package com.tbg.wms.v2.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * Provides db test command behavior for WMS 2.0 workflows.
 */
@Command(name = "db-test", description = "Test database connectivity")
final class DbTestCommand implements Callable<Integer> {
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
        PrintWriter err = spec.commandLine().getErr();
        out.println("=== Database Connectivity Test ===");
        out.println("Site: " + root.dependencies().config().siteCode());
        out.println("JDBC URL: " + root.dependencies().config().oracleJdbcUrl());
        try {
            DbHealthCheck.Result result = root.dependencies().dbHealthCheck().check();
            if (result.connected()) {
                out.println("Database connectivity: OK");
                out.println(result.message());
                return 0;
            }
            err.println("Database connectivity failed: " + result.message());
            return 3;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            err.println("Configuration Error: " + ex.getMessage());
            return 2;
        } catch (Exception ex) {
            err.println("Database connectivity failed: " + ex.getMessage());
            return 3;
        }
    }

}

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
 * Root command for the WMS Pallet Tag System CLI.
 *
 * <p>This is the main entry point for all CLI operations. Subcommands are registered here
 * to handle specific tasks such as configuration display, database testing, label generation,
 * and printing.</p>
 *
 * <p>Exit codes:</p>
 * <ul>
 *   <li>0 – Success</li>
 *   <li>2 – User input/configuration error</li>
 *   <li>3 – Database connectivity error</li>
 *   <li>4 – Database query/data error</li>
 *   <li>5 – Validation error</li>
 *   <li>6 – Print/network error</li>
 *   <li>10 – Unexpected internal error</li>
 * </ul>
 */
@Command(
        name = "wms-tags",
        mixinStandardHelpOptions = true,
        version = "1.2.3",
        description = "WMS Pallet Tag System – Generate and print shipping labels from WMS data",
        subcommands = {
                ShowConfigCommand.class,
                DbTestCommand.class,
                RunCommand.class,
                GuiCommand.class,
                BarcodeCommand.class,
                VersionCommand.class
        }
)
public final class RootCommand implements Callable<Integer> {

    @picocli.CommandLine.Option(
            names = {"-v", "--version"},
            versionHelp = true,
            description = "Print version information and exit"
    )
    boolean versionRequested;

    @Override
    public Integer call() {
        return 0;
    }

    /**
     * Factory method for creating the application configuration.
     * <p>
     * Loads configuration from `.env` file and environment variables with precedence:
     * <ol>
     *   <li>Environment variables (highest precedence)</li>
     *   <li>.env file</li>
     *   <li>Code defaults (lowest precedence)</li>
     * </ol>
     *
     * @return a configured {@link AppConfig} instance
     * @throws IllegalStateException if required configuration keys are missing
     */
    static AppConfig config() {
        return new AppConfig();
    }
}

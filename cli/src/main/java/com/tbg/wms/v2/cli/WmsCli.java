package com.tbg.wms.v2.cli;

import picocli.CommandLine.Command;

/**
 * Defines the Picocli root command for WMS 2.0 command-line workflows.
 */
@Command(
        name = "wms",
        mixinStandardHelpOptions = true,
        version = "WMS Pallet Tag System 2.0",
        subcommands = {
                ConfigCommand.class,
                DbTestCommand.class,
                RunCommand.class,
                BarcodeCommand.class,
                RailPrintCommand.class
        }
)
public final class WmsCli implements Runnable {
    private final CliDependencies dependencies;

    /**
     * Creates a WMS CLI instance.
     */
    public WmsCli() {
        this(CliDependencies.unsupportedDefaults());
    }

    /**
     * Creates a WMS CLI instance.
     *
     * @param dependencies the dependencies.
     */
    public WmsCli(CliDependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Returns the dependency graph shared by CLI subcommands.
     *
     * @return configured CLI dependencies
     */
    CliDependencies dependencies() {
        return dependencies;
    }

    /**
     * Runs the root command when no subcommand is selected.
     */
    @Override
    public void run() {
        throw new picocli.CommandLine.ParameterException(
                new picocli.CommandLine(this),
                "Specify a command."
        );
    }
}

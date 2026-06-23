package com.tbg.wms.v2.cli;

import picocli.CommandLine.Command;

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

    public WmsCli() {
        this(CliDependencies.unsupportedDefaults());
    }

    public WmsCli(CliDependencies dependencies) {
        this.dependencies = dependencies;
    }

    CliDependencies dependencies() {
        return dependencies;
    }

    @Override
    public void run() {
        throw new picocli.CommandLine.ParameterException(
                new picocli.CommandLine(this),
                "Specify a command."
        );
    }
}

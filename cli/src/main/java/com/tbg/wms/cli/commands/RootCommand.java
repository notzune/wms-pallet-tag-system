package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "wms-tags",
        mixinStandardHelpOptions = true,
        description = "WMS Pallet Tag CLI",
        subcommands = {
                ShowConfigCommand.class,
                DbTestCommand.class
        }
)
public final class RootCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }

    static AppConfig config() {
        return new AppConfig();
    }
}
package com.tbg.wms.cli;

import com.tbg.wms.cli.commands.RootCommand;
import picocli.CommandLine;

/**
 * CLI entrypoint.
 */
public final class CliMain {

    public static void main(String[] args) {
        int code = new CommandLine(new RootCommand()).execute(args);
        System.exit(code);
    }
}
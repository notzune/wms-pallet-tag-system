/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.2.0
 */

package com.tbg.wms.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Prints the CLI version.
 */
@Command(
        name = "version",
        description = "Print the CLI version"
)
public final class VersionCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        String version = RootCommand.class.getAnnotation(Command.class).version()[0];
        System.out.println(version);
        return 0;
    }
}

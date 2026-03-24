/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @role WMS Analyst, Tropicana Brands Group
 * @manager Fredrico Sanchez
 * @since 1.0.0
 */

package com.tbg.wms.cli;

import com.tbg.wms.cli.commands.RootCommand;
import com.tbg.wms.cli.gui.LabelGuiFrame;
import com.tbg.wms.core.OutDirectoryRetentionService;
import picocli.CommandLine;

import javax.swing.*;

/**
 * Process bootstrap for CLI execution and zero-argument GUI launch.
 *
 * <p>This class intentionally stays narrow: it performs startup cleanup, launches the GUI when the
 * process is started without arguments, or delegates command execution to {@link RootCommand}.</p>
 *
 * @since 1.0.0
 */
public final class CliMain {

    /**
     * Main application entry point.
     *
     * <p>Supported exit-code bands are determined by command implementations:</p>
     * <ul>
     *   <li>0 - success</li>
     *   <li>2 - user input/configuration error</li>
     *   <li>3 - database connectivity error</li>
     *   <li>4 - database query/data error</li>
     *   <li>5 - validation error</li>
     *   <li>6 - print/network error</li>
     *   <li>10 - unexpected internal error</li>
     * </ul>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        new OutDirectoryRetentionService().pruneDefaultOutDirectory(CliMain.class);
        if (args == null || args.length == 0) {
            GuiLauncher.launchAndWait(LabelGuiFrame::new);
            return;
        }

        int code = new CommandLine(new RootCommand()).execute(args);
        System.exit(code);
    }
}

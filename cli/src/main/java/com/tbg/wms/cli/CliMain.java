/*
 * Copyright © 2026 Tropicana Brands Group
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
import picocli.CommandLine;

import javax.swing.SwingUtilities;

/**
 * CLI entry point for the WMS Pallet Tag System.
 *
 * <p>This class bootstraps the Picocli command-line interface and delegates all
 * command processing to {@link RootCommand} and its subcommands.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * java -jar cli-*.jar [COMMAND] [OPTIONS]
 * java -jar cli-*.jar config                           # Show configuration
 * java -jar cli-*.jar run <shipment_id> --dry-run      # Dry-run label generation
 * java -jar cli-*.jar template --count 5 --out out/    # Generate blank templates
 * }</pre>
 *
 * @since 1.0.0
 */
public final class CliMain {

    /**
     * Main entry point for the CLI.
     * <p>
     * Parses command-line arguments using Picocli and executes the appropriate command.
     * The exit code is determined by the command implementation:
     * </p>
     * <ul>
     *   <li>0 – Success</li>
     *   <li>2 – User input/configuration error</li>
     *   <li>3 – Database connectivity error</li>
     *   <li>4 – Database query/data error</li>
     *   <li>5 – Validation error</li>
     *   <li>6 – Print/network error</li>
     *   <li>10 – Unexpected internal error</li>
     * </ul>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            SwingUtilities.invokeLater(() -> {
                LabelGuiFrame frame = new LabelGuiFrame();
                frame.setVisible(true);
            });
            return;
        }

        int code = new CommandLine(new RootCommand()).execute(args);
        System.exit(code);
    }
}

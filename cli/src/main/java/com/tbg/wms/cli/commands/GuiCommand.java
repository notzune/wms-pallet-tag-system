/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.LabelGuiFrame;
import picocli.CommandLine.Command;

import javax.swing.SwingUtilities;
import java.util.concurrent.Callable;

/**
 * Launches the Swing desktop workflow.
 *
 * This command returns immediately after scheduling the GUI startup on the
 * EDT (Event Dispatch Thread).
 */
@Command(
        name = "gui",
        mixinStandardHelpOptions = true,
        description = "Launch the desktop GUI workflow for preview and confirmed printing"
)
public final class GuiCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        SwingUtilities.invokeLater(() -> {
            LabelGuiFrame frame = new LabelGuiFrame();
            frame.setVisible(true);
        });
        return 0;
    }
}

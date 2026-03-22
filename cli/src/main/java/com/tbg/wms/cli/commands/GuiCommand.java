/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.GuiLauncher;
import com.tbg.wms.cli.gui.LabelGuiFrame;
import picocli.CommandLine.Command;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.IntSupplier;

/**
 * Launches the Swing desktop workflow.
 * <p>
 * This command returns immediately after scheduling the GUI startup on the
 * EDT (Event Dispatch Thread).
 */
@Command(
        name = "gui",
        mixinStandardHelpOptions = true,
        description = "Launch the desktop GUI workflow for preview and confirmed printing"
)
public final class GuiCommand implements Callable<Integer> {
    private final IntSupplier launcher;

    public GuiCommand() {
        this(() -> {
            GuiLauncher.launchAndWait(LabelGuiFrame::new);
            return 0;
        });
    }

    GuiCommand(IntSupplier launcher) {
        this.launcher = Objects.requireNonNull(launcher, "launcher cannot be null");
    }

    /**
     * Schedules GUI launch on the Swing event thread.
     *
     * @return always {@code 0}
     */
    @Override
    public Integer call() {
        return launcher.getAsInt();
    }
}

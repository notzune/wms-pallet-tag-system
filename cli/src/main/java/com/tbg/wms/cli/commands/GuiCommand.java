package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.LabelGuiFrame;
import picocli.CommandLine.Command;

import javax.swing.SwingUtilities;
import java.util.concurrent.Callable;

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

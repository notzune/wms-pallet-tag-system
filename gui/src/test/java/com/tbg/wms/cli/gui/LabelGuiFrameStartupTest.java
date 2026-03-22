package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LabelGuiFrameStartupTest {

    @Test
    void constructorDoesNotFailDuringDependencyInitialization() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "GUI startup test requires a graphics environment");

        AtomicReference<LabelGuiFrame> frameRef = new AtomicReference<>();
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> frameRef.set(new LabelGuiFrame())));
        SwingUtilities.invokeLater(() -> {
            LabelGuiFrame frame = frameRef.get();
            if (frame != null) {
                frame.dispose();
            }
        });
    }
}

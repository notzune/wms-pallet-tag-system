/*
 * Copyright (c) 2026 Tropicana Brands Group
 */

package com.tbg.wms.cli;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * Starts the Swing GUI and keeps the calling process alive until the window closes.
 */
public final class GuiLauncher {

    private GuiLauncher() {
        // Utility class.
    }

    /**
     * Launches a frame on the EDT and blocks until it is closed.
     *
     * @param frameFactory factory that builds the GUI frame
     * @throws IllegalStateException when the GUI cannot be started
     */
    public static void launchAndWait(Supplier<? extends JFrame> frameFactory) {
        Objects.requireNonNull(frameFactory, "frameFactory cannot be null");

        CountDownLatch closed = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame frame = frameFactory.get();
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        closed.countDown();
                    }
                });
                frame.setVisible(true);
                if (!frame.isDisplayable()) {
                    closed.countDown();
                }
            });
            closed.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GUI launch interrupted", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to launch GUI", e.getCause());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to launch GUI", e);
        }
    }
}

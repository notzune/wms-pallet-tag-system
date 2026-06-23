package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiAsyncTaskRunnerTest {

    private final GuiAsyncTaskRunner runner = new GuiAsyncTaskRunner();

    @Test
    void run_shouldDeliverSuccessOnEventDispatchThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Boolean> onEventDispatchThread = new AtomicReference<>(false);

        runner.run(
                () -> "loaded",
                value -> {
                    result.set(value);
                    onEventDispatchThread.set(SwingUtilities.isEventDispatchThread());
                    latch.countDown();
                },
                ex -> latch.countDown()
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS), "success callback should run");
        assertEquals("loaded", result.get());
        assertTrue(onEventDispatchThread.get(), "success callback should run on EDT");
    }

    @Test
    void run_shouldDeliverFailureOnEventDispatchThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();
        AtomicReference<Boolean> onEventDispatchThread = new AtomicReference<>(false);

        runner.run(
                () -> {
                    throw new IllegalStateException("boom");
                },
                value -> latch.countDown(),
                ex -> {
                    failure.set(ex);
                    onEventDispatchThread.set(SwingUtilities.isEventDispatchThread());
                    latch.countDown();
                }
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS), "failure callback should run");
        assertNotNull(failure.get());
        assertEquals("boom", failure.get().getMessage());
        assertTrue(onEventDispatchThread.get(), "failure callback should run on EDT");
    }
}

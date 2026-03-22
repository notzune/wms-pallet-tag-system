package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainSettingsDialogTest {

    @Test
    void saveShouldInvokeConfiguredCallbacks() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        AtomicReference<Path> savedPath = new AtomicReference<>();
        AtomicInteger savedRetention = new AtomicInteger(-1);
        AtomicBoolean clipboardInstalled = new AtomicBoolean(false);

        MainSettingsDialog dialog = onEdt(() -> new MainSettingsDialog(
                null,
                Path.of("out", "default"),
                14,
                "No update check has completed yet.",
                message -> {
                    throw new AssertionError("Unexpected error: " + message);
                },
                fields -> clipboardInstalled.set(fields.length == 2),
                (path, retentionDays) -> {
                    savedPath.set(path);
                    savedRetention.set(retentionDays);
                },
                ignored -> {
                    throw new AssertionError("Cleanup callback should not run.");
                },
                ignored -> {
                    throw new AssertionError("Update callback should not run.");
                },
                () -> {
                    throw new AssertionError("Uninstall callback should not run.");
                },
                () -> {
                    throw new AssertionError("Advanced settings callback should not run.");
                }
        ));
        try {
            onEdt(() -> {
                List<JTextField> textFields = findComponents(dialog, JTextField.class);
                assertTrue(textFields.size() >= 2);
                textFields.get(0).setText("  .\\out\\custom  ");
                textFields.get(1).setText("21");
                findButton(dialog, "Save").doClick();
            });

            assertTrue(clipboardInstalled.get());
            assertNotNull(savedPath.get());
            assertTrue(savedPath.get().isAbsolute());
            assertTrue(savedPath.get().normalize().toString().endsWith(Path.of("out", "custom").toString()));
            assertEquals(21, savedRetention.get());
            assertFalse(onEdt(dialog::isDisplayable));
        } finally {
            onEdt(dialog::dispose);
        }
    }

    @Test
    void maintenanceButtonsShouldInvokeMatchingCallbacks() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        AtomicInteger cleanupInvocations = new AtomicInteger();
        AtomicInteger updateInvocations = new AtomicInteger();
        AtomicInteger uninstallInvocations = new AtomicInteger();
        AtomicInteger advancedInvocations = new AtomicInteger();
        AtomicReference<String> latestUpdateStatus = new AtomicReference<>();
        AtomicReference<String> latestError = new AtomicReference<>();

        MainSettingsDialog dialog = onEdt(() -> new MainSettingsDialog(
                null,
                Path.of("out", "default"),
                14,
                "Ready",
                latestError::set,
                fields -> {
                },
                (path, retentionDays) -> {
                    throw new AssertionError("Save callback should not run.");
                },
                retentionDays -> cleanupInvocations.incrementAndGet(),
                statusLabel -> {
                    updateInvocations.incrementAndGet();
                    latestUpdateStatus.set(statusLabel.getText());
                },
                uninstallInvocations::incrementAndGet,
                advancedInvocations::incrementAndGet
        ));
        try {
            onEdt(() -> {
                findButton(dialog, "Clean Old Output Now").doClick();
                findButton(dialog, "Check for Updates...").doClick();
                findButton(dialog, "Uninstall / Clean Install Prep...").doClick();
                findButton(dialog, "Advanced Settings...").doClick();
            });

            assertEquals(1, cleanupInvocations.get());
            assertEquals(1, updateInvocations.get());
            assertEquals("Ready", latestUpdateStatus.get());
            assertEquals(1, uninstallInvocations.get());
            assertEquals(1, advancedInvocations.get());
            assertNull(latestError.get());
        } finally {
            onEdt(dialog::dispose);
        }
    }

    private static JButton findButton(Container root, String text) {
        return findComponents(root, JButton.class).stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private static <T extends Component> List<T> findComponents(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collectComponents(root, type, matches);
        return matches;
    }

    private static <T extends Component> void collectComponents(Container root, Class<T> type, List<T> matches) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container child) {
                collectComponents(child, type, matches);
            }
        }
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            throw new RuntimeException(failure.get());
        }
    }

    private static <T> T onEdt(ThrowingSupplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(supplier.get());
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            throw new RuntimeException(failure.get());
        }
        return result.get();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}

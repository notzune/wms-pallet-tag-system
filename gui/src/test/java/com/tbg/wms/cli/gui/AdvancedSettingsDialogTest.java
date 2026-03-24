package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.RuntimePathResolver;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedSettingsDialogTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadEditableFilesFromConfiguredAppHome() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        Path appHome = Files.createDirectories(tempDir.resolve("app-home"));
        Path configDir = Files.createDirectories(appHome.resolve("config"));
        Path siteDir = Files.createDirectories(configDir.resolve("TBG3002"));
        Files.writeString(siteDir.resolve("printers.yaml"), "version: 1\nsiteCode: TBG3002\nprinters: []\n");
        Files.writeString(siteDir.resolve("printer-routing.yaml"), "version: 1\nsiteCode: TBG3002\ndefaultPrinterId: DISPATCH\nrules: []\n");
        Files.writeString(configDir.resolve("walmart-sku-matrix.csv"), "sku,item\n");
        Files.writeString(configDir.resolve("walm_loc_num_matrix.csv"), "sold_to,location\n");
        Files.createDirectories(configDir.resolve("templates"));
        Files.writeString(configDir.resolve("templates").resolve("walmart-canada-label.zpl"), "^XA^XZ");

        Path configFile = tempDir.resolve("wms-tags.env");
        Files.writeString(configFile, String.join("\n",
                "ACTIVE_SITE=TBG3002",
                "ORACLE_USERNAME=test",
                "ORACLE_PASSWORD=test",
                "SITE_TBG3002_NAME=Jersey City",
                "SITE_TBG3002_HOST=127.0.0.1"
        ));

        String previousAppHome = System.getProperty(RuntimePathResolver.APP_HOME_PROP);
        String previousConfigFile = System.getProperty("wms.config.file");
        System.setProperty(RuntimePathResolver.APP_HOME_PROP, appHome.toString());
        System.setProperty("wms.config.file", configFile.toString());
        try {
            AdvancedSettingsDialog dialog = onEdt(() -> new AdvancedSettingsDialog(
                    null,
                    new AppConfig(),
                    () -> {
                    },
                    message -> {
                        throw new AssertionError("Unexpected error: " + message);
                    },
                    fields -> {
                    }
            ));
            try {
                onEdt(() -> {
                    List<Component> comboComponents = findComponents(dialog, JComboBox.class)
                            .stream()
                            .map(component -> (Component) component)
                            .toList();
                    assertEquals(1, comboComponents.size());
                    JComboBox<?> combo = (JComboBox<?>) comboComponents.get(0);
                    assertEquals(5, combo.getItemCount());

                    JLabel pathLabel = findComponents(dialog, JLabel.class).stream()
                            .filter(label -> label.getText() != null && label.getText().contains("printers.yaml"))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("Path label not updated"));
                    assertTrue(pathLabel.getText().startsWith(appHome.toString()));
                });
            } finally {
                onEdt(dialog::dispose);
            }
        } finally {
            if (previousAppHome == null) {
                System.clearProperty(RuntimePathResolver.APP_HOME_PROP);
            } else {
                System.setProperty(RuntimePathResolver.APP_HOME_PROP, previousAppHome);
            }
            if (previousConfigFile == null) {
                System.clearProperty("wms.config.file");
            } else {
                System.setProperty("wms.config.file", previousConfigFile);
            }
        }
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

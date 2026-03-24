package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.RuntimePathResolver;
import com.tbg.wms.core.RuntimeSettings;
import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiSettingsDialogSupportTest {

    @Test
    void defaultPrintToFileOutputDir_shouldFallbackWhenStoredPreferenceIsInvalid() throws Exception {
        Preferences preferences = Preferences.userRoot().node("com/tbg/wms/tests/gui-settings/" + System.nanoTime());
        try {
            preferences.put("printToFile.defaultOutputDir", "C:\\bad|path");
            StubDependencies dependencies = new StubDependencies(preferences);
            GuiSettingsDialogSupport support = new GuiSettingsDialogSupport(
                    dependencies,
                    "printToFile.defaultOutputDir",
                    GuiSettingsDialogSupportTest.class
            );

            Path resolved = support.defaultPrintToFileOutputDir();

            assertEquals(RuntimePathResolver.resolveJarSiblingDir(GuiSettingsDialogSupportTest.class, "out"), resolved);
        } finally {
            preferences.removeNode();
        }
    }

    @Test
    void saveMainSettings_shouldPersistRetentionAndRebuildPrinterModel() throws Exception {
        Preferences preferences = Preferences.userRoot().node("com/tbg/wms/tests/gui-settings/" + System.nanoTime());
        try {
            StubDependencies dependencies = new StubDependencies(preferences);
            int originalRetentionDays = dependencies.runtimeSettings.outRetentionDays(7);
            try {
                dependencies.loadedPrinters = List.of(
                        new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1:9100", List.of("ZPL")),
                        new LabelWorkflowService.PrinterOption("P2", "Printer 2", "10.0.0.2:9100", List.of("ZPL"))
                );
                dependencies.selectedPrinter = dependencies.loadedPrinters.get(1);
                GuiSettingsDialogSupport support = new GuiSettingsDialogSupport(
                        dependencies,
                        "printToFile.defaultOutputDir",
                        GuiSettingsDialogSupportTest.class
                );
                Path configuredPath = Paths.get("C:\\temp\\wms-out");

                support.saveMainSettings(configuredPath, 21);

                assertEquals(configuredPath.toString(), preferences.get("printToFile.defaultOutputDir", ""));
                assertEquals(21, dependencies.runtimeSettings.outRetentionDays(7));
                assertEquals(3, dependencies.lastModel.getSize());
                assertEquals("P2", dependencies.restoredSelection.getId());
                assertTrue(dependencies.applyTopRowSizingCalled);
                assertEquals("Settings saved.", dependencies.readyMessage);
            } finally {
                dependencies.runtimeSettings.setOutRetentionDays(originalRetentionDays);
            }
        } finally {
            preferences.removeNode();
        }
    }

    private static final class StubDependencies implements GuiSettingsDialogSupport.Dependencies {
        private final Preferences preferences;
        private final RuntimeSettings runtimeSettings = new RuntimeSettings();
        private List<LabelWorkflowService.PrinterOption> loadedPrinters = List.of();
        private LabelWorkflowService.PrinterOption selectedPrinter;
        private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> lastModel;
        private LabelWorkflowService.PrinterOption restoredSelection;
        private boolean applyTopRowSizingCalled;
        private String readyMessage;

        private StubDependencies(Preferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public JFrame ownerFrame() {
            return null;
        }

        @Override
        public Preferences preferences() {
            return preferences;
        }

        @Override
        public RuntimeSettings runtimeSettings() {
            return runtimeSettings;
        }

        @Override
        public AppConfig config() {
            return new AppConfig();
        }

        @Override
        public List<LabelWorkflowService.PrinterOption> loadedPrinters() {
            return loadedPrinters;
        }

        @Override
        public LabelWorkflowService.PrinterOption selectedPrinterOption() {
            return selectedPrinter;
        }

        @Override
        public void setPrinterModel(DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model) {
            lastModel = model;
        }

        @Override
        public void applyTopRowSizing() {
            applyTopRowSizingCalled = true;
        }

        @Override
        public void restoreSelection(LabelWorkflowService.PrinterOption previousSelection) {
            restoredSelection = previousSelection;
        }

        @Override
        public void showError(String message) {
        }

        @Override
        public void installClipboardBehavior(JTextComponent... fields) {
        }

        @Override
        public String formatUpdateStatus() {
            return "Up to date";
        }

        @Override
        public void checkForUpdates(JLabel statusOutput) {
        }

        @Override
        public void openUninstallDialog() {
        }

        @Override
        public void setReady(String message) {
            readyMessage = message;
        }

        @Override
        public void clearLabelWorkflowCaches() {
        }

        @Override
        public void clearAdvancedWorkflowCaches() {
        }

        @Override
        public void resetLoadedPrinters() {
            loadedPrinters = List.of();
        }

        @Override
        public void loadPrintersAsync() {
        }
    }
}

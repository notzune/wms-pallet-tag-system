/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.OutDirectoryRetentionService;
import com.tbg.wms.core.RuntimePathResolver;
import com.tbg.wms.core.RuntimeSettings;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Owns settings-dialog behavior, maintenance actions, and runtime config reload flow.
 */
final class GuiSettingsDialogSupport {

    private final Dependencies dependencies;
    private final String printToFilePreferenceKey;
    private final Class<?> runtimeAnchorType;

    GuiSettingsDialogSupport(Dependencies dependencies, String printToFilePreferenceKey, Class<?> runtimeAnchorType) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null");
        this.printToFilePreferenceKey = Objects.requireNonNull(printToFilePreferenceKey, "printToFilePreferenceKey cannot be null");
        this.runtimeAnchorType = Objects.requireNonNull(runtimeAnchorType, "runtimeAnchorType cannot be null");
    }

    Path defaultPrintToFileOutputDir() {
        String configured = dependencies.preferences().get(printToFilePreferenceKey, "");
        if (configured != null && !configured.isBlank()) {
            try {
                return Paths.get(configured.trim());
            } catch (java.nio.file.InvalidPathException ignored) {
                // Fall back to the runtime-derived output directory if the stored path is invalid.
            }
        }
        return RuntimePathResolver.resolveJarSiblingDir(runtimeAnchorType, "out");
    }

    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildMainPrintTargetModel(boolean includeFileOption) {
        return buildPrintTargetModel(
                GuiPrinterTargetSupport.filterLabelScreenPrinters(dependencies.loadedPrinters()),
                includeFileOption
        );
    }

    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(boolean includeFileOption) {
        return buildPrintTargetModel(dependencies.loadedPrinters(), includeFileOption);
    }

    void openSettingsDialog() {
        MainSettingsDialog dialog = new MainSettingsDialog(
                dependencies.ownerFrame(),
                defaultPrintToFileOutputDir(),
                dependencies.runtimeSettings().outRetentionDays(OutDirectoryRetentionService.DEFAULT_RETENTION_DAYS),
                dependencies.formatUpdateStatus(),
                dependencies::showError,
                dependencies::installClipboardBehavior,
                this::saveMainSettings,
                retentionDays -> runOutDirectoryCleanup(dependencies.ownerFrame(), retentionDays),
                () -> dependencies.checkForUpdates(null),
                dependencies::openUninstallDialog,
                () -> openAdvancedSettingsDialog(dependencies.ownerFrame())
        );
        dialog.setVisible(true);
    }

    void saveMainSettings(Path configuredPath, int retentionDays) {
        dependencies.preferences().put(printToFilePreferenceKey, configuredPath.toString());
        dependencies.runtimeSettings().setOutRetentionDays(retentionDays);
        LabelWorkflowService.PrinterOption previousSelection = dependencies.selectedPrinterOption();
        dependencies.setPrinterModel(buildMainPrintTargetModel(true));
        dependencies.applyTopRowSizing();
        dependencies.restoreSelection(previousSelection);
        dependencies.setReady("Settings saved.");
    }

    void runOutDirectoryCleanup(Component owner, int retentionDays) {
        OutDirectoryRetentionService.CleanupResult result =
                new OutDirectoryRetentionService().pruneDefaultOutDirectory(runtimeAnchorType, retentionDays);
        JOptionPane.showMessageDialog(
                owner,
                "Cleanup complete.\nDeleted files: " + result.getDeletedFiles()
                        + "\nDeleted folders: " + result.getDeletedDirectories()
                        + "\nRetention days: " + retentionDays
                        + "\nRoot: " + result.getRootDir(),
                "Out Cleanup",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    void openAdvancedSettingsDialog(Component owner) {
        AdvancedSettingsDialog dialog = new AdvancedSettingsDialog(
                dependencies.ownerFrame(),
                dependencies.config(),
                this::reloadRuntimeConfigArtifacts,
                dependencies::showError,
                dependencies::installClipboardBehavior
        );
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    void reloadRuntimeConfigArtifacts() {
        dependencies.clearLabelWorkflowCaches();
        dependencies.clearAdvancedWorkflowCaches();
        dependencies.resetLoadedPrinters();
        dependencies.loadPrintersAsync();
        dependencies.setReady("Runtime config reloaded.");
    }

    private DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(
            List<LabelWorkflowService.PrinterOption> printerOptions,
            boolean includeFileOption
    ) {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
        for (LabelWorkflowService.PrinterOption option : printerOptions) {
            model.addElement(option);
        }
        if (includeFileOption) {
            model.addElement(GuiPrinterTargetSupport.buildPrintToFileOption(defaultPrintToFileOutputDir()));
        }
        return model;
    }

    interface Dependencies {
        JFrame ownerFrame();
        Preferences preferences();
        RuntimeSettings runtimeSettings();
        AppConfig config();
        List<LabelWorkflowService.PrinterOption> loadedPrinters();
        LabelWorkflowService.PrinterOption selectedPrinterOption();
        void setPrinterModel(DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model);
        void applyTopRowSizing();
        void restoreSelection(LabelWorkflowService.PrinterOption previousSelection);
        void showError(String message);
        void installClipboardBehavior(JTextComponent... fields);
        String formatUpdateStatus();
        void checkForUpdates(JLabel statusOutput);
        void openUninstallDialog();
        void setReady(String message);
        void clearLabelWorkflowCaches();
        void clearAdvancedWorkflowCaches();
        void resetLoadedPrinters();
        void loadPrintersAsync();
    }
}

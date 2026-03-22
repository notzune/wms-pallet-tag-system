package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.RuntimeSettings;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Frame-backed dependency adapter for settings dialog behavior.
 */
final class LabelGuiFrameSettingsDependencies implements GuiSettingsDialogSupport.Dependencies {

    private final JFrame ownerFrame;
    private final Preferences preferences;
    private final RuntimeSettings runtimeSettings;
    private final AppConfig config;
    private final LoadedPrintersSupplier loadedPrintersSupplier;
    private final SelectedPrinterSupplier selectedPrinterSupplier;
    private final PrinterModelSetter printerModelSetter;
    private final Runnable applyTopRowSizing;
    private final RestoreSelection restoreSelection;
    private final MessageSink showError;
    private final ClipboardInstaller clipboardInstaller;
    private final TextSupplier formatUpdateStatus;
    private final CheckForUpdates checkForUpdates;
    private final Runnable openUninstallDialog;
    private final MessageSink setReady;
    private final Runnable clearLabelWorkflowCaches;
    private final Runnable clearAdvancedWorkflowCaches;
    private final Runnable resetLoadedPrinters;
    private final Runnable loadPrintersAsync;

    LabelGuiFrameSettingsDependencies(
            JFrame ownerFrame,
            Preferences preferences,
            RuntimeSettings runtimeSettings,
            AppConfig config,
            LoadedPrintersSupplier loadedPrintersSupplier,
            SelectedPrinterSupplier selectedPrinterSupplier,
            PrinterModelSetter printerModelSetter,
            Runnable applyTopRowSizing,
            RestoreSelection restoreSelection,
            MessageSink showError,
            ClipboardInstaller clipboardInstaller,
            TextSupplier formatUpdateStatus,
            CheckForUpdates checkForUpdates,
            Runnable openUninstallDialog,
            MessageSink setReady,
            Runnable clearLabelWorkflowCaches,
            Runnable clearAdvancedWorkflowCaches,
            Runnable resetLoadedPrinters,
            Runnable loadPrintersAsync
    ) {
        this.ownerFrame = Objects.requireNonNull(ownerFrame, "ownerFrame cannot be null");
        this.preferences = Objects.requireNonNull(preferences, "preferences cannot be null");
        this.runtimeSettings = Objects.requireNonNull(runtimeSettings, "runtimeSettings cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.loadedPrintersSupplier = Objects.requireNonNull(loadedPrintersSupplier, "loadedPrintersSupplier cannot be null");
        this.selectedPrinterSupplier = Objects.requireNonNull(selectedPrinterSupplier, "selectedPrinterSupplier cannot be null");
        this.printerModelSetter = Objects.requireNonNull(printerModelSetter, "printerModelSetter cannot be null");
        this.applyTopRowSizing = Objects.requireNonNull(applyTopRowSizing, "applyTopRowSizing cannot be null");
        this.restoreSelection = Objects.requireNonNull(restoreSelection, "restoreSelection cannot be null");
        this.showError = Objects.requireNonNull(showError, "showError cannot be null");
        this.clipboardInstaller = Objects.requireNonNull(clipboardInstaller, "clipboardInstaller cannot be null");
        this.formatUpdateStatus = Objects.requireNonNull(formatUpdateStatus, "formatUpdateStatus cannot be null");
        this.checkForUpdates = Objects.requireNonNull(checkForUpdates, "checkForUpdates cannot be null");
        this.openUninstallDialog = Objects.requireNonNull(openUninstallDialog, "openUninstallDialog cannot be null");
        this.setReady = Objects.requireNonNull(setReady, "setReady cannot be null");
        this.clearLabelWorkflowCaches = Objects.requireNonNull(clearLabelWorkflowCaches, "clearLabelWorkflowCaches cannot be null");
        this.clearAdvancedWorkflowCaches = Objects.requireNonNull(clearAdvancedWorkflowCaches, "clearAdvancedWorkflowCaches cannot be null");
        this.resetLoadedPrinters = Objects.requireNonNull(resetLoadedPrinters, "resetLoadedPrinters cannot be null");
        this.loadPrintersAsync = Objects.requireNonNull(loadPrintersAsync, "loadPrintersAsync cannot be null");
    }

    @Override
    public JFrame ownerFrame() {
        return ownerFrame;
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
        return config;
    }

    @Override
    public List<LabelWorkflowService.PrinterOption> loadedPrinters() {
        return loadedPrintersSupplier.get();
    }

    @Override
    public LabelWorkflowService.PrinterOption selectedPrinterOption() {
        return selectedPrinterSupplier.get();
    }

    @Override
    public void setPrinterModel(DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model) {
        printerModelSetter.set(model);
    }

    @Override
    public void applyTopRowSizing() {
        applyTopRowSizing.run();
    }

    @Override
    public void restoreSelection(LabelWorkflowService.PrinterOption previousSelection) {
        restoreSelection.apply(previousSelection);
    }

    @Override
    public void showError(String message) {
        showError.accept(message);
    }

    @Override
    public void installClipboardBehavior(JTextComponent... fields) {
        clipboardInstaller.install(fields);
    }

    @Override
    public String formatUpdateStatus() {
        return formatUpdateStatus.get();
    }

    @Override
    public void checkForUpdates(JLabel statusOutput) {
        checkForUpdates.accept(statusOutput);
    }

    @Override
    public void openUninstallDialog() {
        openUninstallDialog.run();
    }

    @Override
    public void setReady(String message) {
        setReady.accept(message);
    }

    @Override
    public void clearLabelWorkflowCaches() {
        clearLabelWorkflowCaches.run();
    }

    @Override
    public void clearAdvancedWorkflowCaches() {
        clearAdvancedWorkflowCaches.run();
    }

    @Override
    public void resetLoadedPrinters() {
        resetLoadedPrinters.run();
    }

    @Override
    public void loadPrintersAsync() {
        loadPrintersAsync.run();
    }

    interface LoadedPrintersSupplier {
        List<LabelWorkflowService.PrinterOption> get();
    }

    interface SelectedPrinterSupplier {
        LabelWorkflowService.PrinterOption get();
    }

    interface PrinterModelSetter {
        void set(DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model);
    }

    interface RestoreSelection {
        void apply(LabelWorkflowService.PrinterOption previousSelection);
    }

    interface MessageSink {
        void accept(String message);
    }

    interface ClipboardInstaller {
        void install(JTextComponent... fields);
    }

    interface TextSupplier {
        String get();
    }

    interface CheckForUpdates {
        void accept(JLabel statusOutput);
    }
}

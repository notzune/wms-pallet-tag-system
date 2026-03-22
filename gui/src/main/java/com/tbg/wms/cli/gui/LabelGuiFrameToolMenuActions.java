package com.tbg.wms.cli.gui;

import java.util.Objects;

/**
 * Frame-backed action adapter for the tools popup menu.
 */
final class LabelGuiFrameToolMenuActions implements LabelGuiFrameToolMenuSupport.MenuActions {

    private final Runnable openRailLabelsDialog;
    private final Runnable openQueueDialog;
    private final Runnable openBarcodeDialog;
    private final Runnable openResumeDialog;
    private final Runnable openSettingsDialog;

    LabelGuiFrameToolMenuActions(
            Runnable openRailLabelsDialog,
            Runnable openQueueDialog,
            Runnable openBarcodeDialog,
            Runnable openResumeDialog,
            Runnable openSettingsDialog
    ) {
        this.openRailLabelsDialog = Objects.requireNonNull(openRailLabelsDialog, "openRailLabelsDialog cannot be null");
        this.openQueueDialog = Objects.requireNonNull(openQueueDialog, "openQueueDialog cannot be null");
        this.openBarcodeDialog = Objects.requireNonNull(openBarcodeDialog, "openBarcodeDialog cannot be null");
        this.openResumeDialog = Objects.requireNonNull(openResumeDialog, "openResumeDialog cannot be null");
        this.openSettingsDialog = Objects.requireNonNull(openSettingsDialog, "openSettingsDialog cannot be null");
    }

    @Override
    public void openRailLabelsDialog() {
        openRailLabelsDialog.run();
    }

    @Override
    public void openQueueDialog() {
        openQueueDialog.run();
    }

    @Override
    public void openBarcodeDialog() {
        openBarcodeDialog.run();
    }

    @Override
    public void openResumeDialog() {
        openResumeDialog.run();
    }

    @Override
    public void openSettingsDialog() {
        openSettingsDialog.run();
    }
}

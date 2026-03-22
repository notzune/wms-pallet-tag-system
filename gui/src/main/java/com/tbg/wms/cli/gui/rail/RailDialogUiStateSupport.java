package com.tbg.wms.cli.gui.rail;

/**
 * Computes button/status transitions for the rail dialog shell.
 */
final class RailDialogUiStateSupport {

    UiState initial() {
        return new UiState("Ready.", true, false, false, true);
    }

    UiState loadingPrinters() {
        return new UiState("Loading rail printers...", false, false, false, false);
    }

    UiState previewLoading() {
        return new UiState("Loading WMS rail rows...", false, false, false, true);
    }

    UiState previewReady(String message) {
        return new UiState(message, true, true, true, true);
    }

    UiState previewFailed(String message) {
        return new UiState(message, true, false, false, true);
    }

    UiState generationBusy(String message) {
        return new UiState(message, false, false, false, true);
    }

    UiState generationComplete(String message, boolean hasPreparedJob) {
        return new UiState(message, true, hasPreparedJob, hasPreparedJob, true);
    }

    UiState printersReady(String message) {
        return new UiState(message, true, false, false, true);
    }

    record UiState(
            String statusMessage,
            boolean loadEnabled,
            boolean generateEnabled,
            boolean printEnabled,
            boolean printerEnabled
    ) {
    }
}

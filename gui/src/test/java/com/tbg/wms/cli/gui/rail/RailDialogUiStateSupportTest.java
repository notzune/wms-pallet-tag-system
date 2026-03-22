package com.tbg.wms.cli.gui.rail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailDialogUiStateSupportTest {

    private final RailDialogUiStateSupport support = new RailDialogUiStateSupport();

    @Test
    void previewLoadingShouldDisableActions() {
        RailDialogUiStateSupport.UiState state = support.previewLoading();

        assertEquals("Loading WMS rail rows...", state.statusMessage());
        assertFalse(state.loadEnabled());
        assertFalse(state.generateEnabled());
        assertFalse(state.printEnabled());
        assertTrue(state.printerEnabled());
    }

    @Test
    void previewReadyShouldEnableGenerateAndPrint() {
        RailDialogUiStateSupport.UiState state = support.previewReady("Preview ready.");

        assertTrue(state.loadEnabled());
        assertTrue(state.generateEnabled());
        assertTrue(state.printEnabled());
    }

    @Test
    void generationCompleteShouldRespectPreparedJobPresence() {
        RailDialogUiStateSupport.UiState withoutJob = support.generationComplete("Generation failed.", false);
        RailDialogUiStateSupport.UiState withJob = support.generationComplete("PDF generated.", true);

        assertFalse(withoutJob.generateEnabled());
        assertFalse(withoutJob.printEnabled());
        assertTrue(withJob.generateEnabled());
        assertTrue(withJob.printEnabled());
    }
}

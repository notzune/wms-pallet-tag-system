package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.UpdatePolicyService;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateUiSupportTest {

    @Test
    void buildState_shouldShowRecommendedSummaryWhenOneStableUpdateBehind() throws BackingStoreException {
        Preferences preferences = Preferences.userRoot().node("/com/tbg/wms/test/" + UUID.randomUUID());
        try {
            UpdatePromptStateStore store = new UpdatePromptStateStore(preferences);
            UpdatePolicyService.UpdateDecision decision = new UpdatePolicyService.UpdateDecision(
                    "1.7.1",
                    "1.7.2",
                    "",
                    1,
                    true,
                    false,
                    UpdatePolicyService.UpdateSeverity.RECOMMENDED
            );

            UpdateUiSupport.UpdateUiState state = new UpdateUiSupport().buildState(decision, store);

            assertEquals("1 update behind, update recommended.", state.summary());
            assertFalse(state.showToolbarWarning());
            assertFalse(state.showStartupPrompt());
        } finally {
            preferences.removeNode();
            preferences.flush();
        }
    }

    @Test
    void buildState_shouldKeepToolbarWarningWhenPromptIgnoredForSpecificTarget() throws BackingStoreException {
        Preferences preferences = Preferences.userRoot().node("/com/tbg/wms/test/" + UUID.randomUUID());
        try {
            UpdatePromptStateStore store = new UpdatePromptStateStore(preferences);
            store.ignorePromptTarget("1.8.0");
            UpdatePolicyService.UpdateDecision decision = new UpdatePolicyService.UpdateDecision(
                    "1.7.1",
                    "1.8.0",
                    "1.8.0-rc1",
                    2,
                    true,
                    true,
                    UpdatePolicyService.UpdateSeverity.REQUIRED
            );

            UpdateUiSupport.UpdateUiState state = new UpdateUiSupport().buildState(decision, store);

            assertEquals("New stable update available: 1.8.0.", state.summary());
            assertTrue(state.showToolbarWarning());
            assertFalse(state.showStartupPrompt());
        } finally {
            preferences.removeNode();
            preferences.flush();
        }
    }
}

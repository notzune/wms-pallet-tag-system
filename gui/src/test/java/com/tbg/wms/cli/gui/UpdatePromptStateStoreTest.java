package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdatePromptStateStoreTest {

    @Test
    void shouldIgnoreOnlyExactPromptedTargetVersion() throws BackingStoreException {
        Preferences preferences = Preferences.userRoot().node("/com/tbg/wms/test/" + UUID.randomUUID());
        try {
            UpdatePromptStateStore store = new UpdatePromptStateStore(preferences);
            store.ignorePromptTarget("1.8.0");

            assertFalse(store.shouldShowPrompt("1.8.0"));
            assertTrue(store.shouldShowPrompt("1.8.1"));
            assertTrue(store.shouldShowPrompt("1.7.2"));
        } finally {
            preferences.removeNode();
            preferences.flush();
        }
    }
}

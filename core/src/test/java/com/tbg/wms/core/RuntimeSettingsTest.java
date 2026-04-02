package com.tbg.wms.core;

import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeSettingsTest {

    @Test
    void developerMode_shouldDefaultToDisabledAndPersistChanges() throws Exception {
        Preferences preferences = Preferences.userRoot().node("com/tbg/wms/tests/runtime-settings/" + System.nanoTime());
        try {
            RuntimeSettings settings = new RuntimeSettings(preferences);

            assertFalse(settings.developerModeEnabled());

            settings.setDeveloperModeEnabled(true);
            assertTrue(settings.developerModeEnabled());

            RuntimeSettings reloaded = new RuntimeSettings(preferences);
            assertTrue(reloaded.developerModeEnabled());
        } finally {
            preferences.removeNode();
        }
    }
}

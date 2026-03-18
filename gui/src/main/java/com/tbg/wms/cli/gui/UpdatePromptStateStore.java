package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.VersionSupport;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Persists updater prompt suppression and experimental update preferences.
 */
public final class UpdatePromptStateStore {
    private static final String KEY_IGNORED_TARGET_VERSION = "updates.ignoredTargetVersion";
    private static final String KEY_EXPERIMENTAL_UPDATES_ENABLED = "updates.experimentalEnabled";

    private final Preferences preferences;

    public UpdatePromptStateStore() {
        this(Preferences.userNodeForPackage(UpdatePromptStateStore.class));
    }

    UpdatePromptStateStore(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences cannot be null");
    }

    public void ignorePromptTarget(String targetVersion) {
        preferences.put(KEY_IGNORED_TARGET_VERSION, VersionSupport.normalize(targetVersion));
    }

    public boolean shouldShowPrompt(String targetVersion) {
        String normalizedTargetVersion = VersionSupport.normalize(targetVersion);
        if (normalizedTargetVersion.isBlank()) {
            return false;
        }
        return !normalizedTargetVersion.equals(preferences.get(KEY_IGNORED_TARGET_VERSION, ""));
    }

    public boolean experimentalUpdatesEnabled() {
        return preferences.getBoolean(KEY_EXPERIMENTAL_UPDATES_ENABLED, false);
    }

    public void setExperimentalUpdatesEnabled(boolean enabled) {
        preferences.putBoolean(KEY_EXPERIMENTAL_UPDATES_ENABLED, enabled);
    }
}

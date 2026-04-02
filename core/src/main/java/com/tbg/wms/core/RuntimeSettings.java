package com.tbg.wms.core;

import java.util.prefs.Preferences;

/**
 * Stores non-secret user/runtime settings shared across CLI and GUI launches.
 */
public final class RuntimeSettings {
    private static final String PREF_NODE = "com/tbg/wms/runtime";
    private static final String PREF_OUT_RETENTION_DAYS = "out.retention.days";
    private static final String PREF_DEVELOPER_MODE = "developer.mode.enabled";

    private final Preferences preferences;

    public RuntimeSettings() {
        this(Preferences.userRoot().node(PREF_NODE));
    }

    public RuntimeSettings(Preferences preferences) {
        this.preferences = preferences;
    }

    public int outRetentionDays(int defaultDays) {
        int stored = preferences.getInt(PREF_OUT_RETENTION_DAYS, defaultDays);
        return stored > 0 ? stored : defaultDays;
    }

    public void setOutRetentionDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }
        preferences.putInt(PREF_OUT_RETENTION_DAYS, days);
    }

    public boolean developerModeEnabled() {
        return preferences.getBoolean(PREF_DEVELOPER_MODE, false);
    }

    public void setDeveloperModeEnabled(boolean enabled) {
        preferences.putBoolean(PREF_DEVELOPER_MODE, enabled);
    }
}

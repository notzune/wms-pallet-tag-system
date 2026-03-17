package com.tbg.wms.core;

import java.util.prefs.Preferences;

/**
 * Stores non-secret user/runtime settings shared across CLI and GUI launches.
 */
public final class RuntimeSettings {
    private static final String PREF_NODE = "com/tbg/wms/runtime";
    private static final String PREF_OUT_RETENTION_DAYS = "out.retention.days";

    private final Preferences preferences = Preferences.userRoot().node(PREF_NODE);

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
}

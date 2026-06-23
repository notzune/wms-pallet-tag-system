package com.tbg.wms.v2.desktop.settings;

import java.nio.file.Path;
import java.util.Objects;

public final class SettingsViewModel {
    private final Store store;
    private SettingsSnapshot snapshot;
    private String status = "Load settings.";

    public SettingsViewModel(Store store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public void load() {
        snapshot = store.load();
        status = "Loaded settings.";
    }

    public void save(Path defaultOutputDir, int retentionDays) {
        store.save(defaultOutputDir, retentionDays);
        snapshot = new SettingsSnapshot(snapshot.siteCode(), snapshot.environment(), defaultOutputDir, retentionDays);
        status = "Saved settings.";
    }

    public String runtimeSummary() {
        if (snapshot == null) {
            return "";
        }
        return snapshot.siteCode() + " / " + snapshot.environment();
    }

    public String status() {
        return status;
    }

    public interface Store {
        SettingsSnapshot load();

        void save(Path defaultOutputDir, int retentionDays);
    }

    public record SettingsSnapshot(String siteCode, String environment, Path defaultOutputDir, int retentionDays) {
    }
}

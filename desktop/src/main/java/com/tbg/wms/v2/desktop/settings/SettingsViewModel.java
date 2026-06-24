package com.tbg.wms.v2.desktop.settings;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Manages desktop settings state and persistence status.
 */
public final class SettingsViewModel {
    private final Store store;
    private SettingsSnapshot snapshot;
    private String status = "Load settings.";

    /**
     * Creates a Settings View Model instance.
     *
     * @param store the store.
     */
    public SettingsViewModel(Store store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /**
     * Loads the current state from the backing store.
     */
    public void load() {
        snapshot = store.load();
        status = "Loaded settings.";
    }

    /**
     * Saves the supplied state to the backing store.
     *
     * @param defaultOutputDir the default output dir.
     * @param retentionDays the retention days.
     */
    public void save(Path defaultOutputDir, int retentionDays) {
        store.save(defaultOutputDir, retentionDays);
        snapshot = new SettingsSnapshot(snapshot.siteCode(), snapshot.environment(), defaultOutputDir, retentionDays);
        status = "Saved settings.";
    }

    /**
     * Returns a compact site and environment summary for display.
     *
     * @return site/environment summary, or an empty string before settings load
     */
    public String runtimeSummary() {
        if (snapshot == null) {
            return "";
        }
        return snapshot.siteCode() + " / " + snapshot.environment();
    }

    /**
     * Returns the current settings workflow status.
     *
     * @return current status message
     */
    public String status() {
        return status;
    }

    /**
     * Defines the contract for store behavior in WMS 2.0 workflows.
     */
    public interface Store {
        /**
         * Loads persisted settings.
         *
         * @return persisted settings snapshot
         */
        SettingsSnapshot load();

        /**
         * Saves the supplied checkpoint.
         *
         * @param defaultOutputDir the default output dir.
         * @param retentionDays the retention days.
         */
        void save(Path defaultOutputDir, int retentionDays);
    }

    /**
     * Carries settings snapshot data across WMS 2.0 module boundaries.
     *
     * @param siteCode the site code.
     * @param environment the environment.
     * @param defaultOutputDir the default output dir.
     * @param retentionDays the retention days.
     */
    public record SettingsSnapshot(String siteCode, String environment, Path defaultOutputDir, int retentionDays) {
    }
}

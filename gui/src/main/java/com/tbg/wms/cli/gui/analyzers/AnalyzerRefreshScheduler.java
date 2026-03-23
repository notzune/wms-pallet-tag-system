package com.tbg.wms.cli.gui.analyzers;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class AnalyzerRefreshScheduler {

    private final Runnable refreshAction;
    private boolean enabled;
    private Duration interval = Duration.ZERO;
    private Instant lastRefreshBaseline;

    public AnalyzerRefreshScheduler(Runnable refreshAction) {
        this.refreshAction = Objects.requireNonNull(refreshAction, "refreshAction cannot be null");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setInterval(Duration interval) {
        this.interval = Objects.requireNonNull(interval, "interval cannot be null");
    }

    public Duration interval() {
        return interval;
    }

    public void markRefreshCompleted(Instant completedAt) {
        lastRefreshBaseline = Objects.requireNonNull(completedAt, "completedAt cannot be null");
    }

    public Instant lastRefreshBaseline() {
        return lastRefreshBaseline;
    }

    public void requestImmediateRefresh() {
        refreshAction.run();
    }
}

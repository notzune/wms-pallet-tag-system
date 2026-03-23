package com.tbg.wms.cli.gui.analyzers;

import javax.swing.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class AnalyzerRefreshScheduler {

    private final Runnable refreshAction;
    private final Timer timer;
    private boolean enabled;
    private Duration interval = Duration.ZERO;
    private Instant lastRefreshBaseline;

    public AnalyzerRefreshScheduler(Runnable refreshAction) {
        this.refreshAction = Objects.requireNonNull(refreshAction, "refreshAction cannot be null");
        this.timer = new Timer(0, e -> refreshAction.run());
        this.timer.setRepeats(true);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        restartTimerIfNeeded();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setInterval(Duration interval) {
        this.interval = Objects.requireNonNull(interval, "interval cannot be null");
        restartTimerIfNeeded();
    }

    public Duration interval() {
        return interval;
    }

    public void markRefreshCompleted(Instant completedAt) {
        lastRefreshBaseline = Objects.requireNonNull(completedAt, "completedAt cannot be null");
        restartTimerIfNeeded();
    }

    public Instant lastRefreshBaseline() {
        return lastRefreshBaseline;
    }

    public void requestImmediateRefresh() {
        refreshAction.run();
        restartTimerIfNeeded();
    }

    private void restartTimerIfNeeded() {
        timer.stop();
        if (!enabled || interval.isZero() || interval.isNegative()) {
            return;
        }
        int delayMs = Math.toIntExact(interval.toMillis());
        timer.setInitialDelay(delayMs);
        timer.setDelay(delayMs);
        timer.start();
    }
}

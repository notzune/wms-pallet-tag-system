package com.tbg.wms.cli.gui.analyzers;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyzerRefreshSchedulerTest {

    @Test
    void manualRefresh_shouldResetTimerBaseline() {
        FakeRefreshTarget target = new FakeRefreshTarget();
        AnalyzerRefreshScheduler scheduler = new AnalyzerRefreshScheduler(target::refreshNow);

        scheduler.setEnabled(true);
        scheduler.setInterval(Duration.ofSeconds(30));
        scheduler.markRefreshCompleted(Instant.parse("2026-03-23T10:00:00Z"));
        scheduler.requestImmediateRefresh();

        assertEquals(1, target.invocations);
        assertEquals(Instant.parse("2026-03-23T10:00:00Z"), scheduler.lastRefreshBaseline());
    }

    private static final class FakeRefreshTarget {
        private int invocations;

        private void refreshNow() {
            invocations++;
        }
    }
}

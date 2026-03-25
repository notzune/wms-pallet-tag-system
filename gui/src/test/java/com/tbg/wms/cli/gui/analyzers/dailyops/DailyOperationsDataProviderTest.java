package com.tbg.wms.cli.gui.analyzers.dailyops;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyOperationsDataProviderTest {

    @Test
    void load_shouldAssembleAllSectionSnapshotsInOrder() throws Exception {
        DailyOperationsDataProvider provider = new DailyOperationsDataProvider(List.of(
                section("Case Pick Summary"),
                section("Case Pick Shift Throughput")
        ));

        AnalyzerResult<AnalyzerDashboardSectionSnapshot> result = provider.load(context());

        assertEquals(List.of("Case Pick Summary", "Case Pick Shift Throughput"),
                result.rows().stream().map(AnalyzerDashboardSectionSnapshot::title).toList());
    }

    @Test
    void load_shouldCaptureSectionFailureAndContinue() throws Exception {
        DailyOperationsDataProvider provider = new DailyOperationsDataProvider(List.of(
                section("Case Pick Summary"),
                failingSection("Unload and Load Activity", "boom")
        ));

        AnalyzerResult<AnalyzerDashboardSectionSnapshot> result = provider.load(context());

        assertFalse(result.rows().get(0).failed());
        assertTrue(result.rows().get(1).failed());
        assertEquals("boom", result.rows().get(1).errorText());
    }

    private static DailyOperationsSectionLoader section(String title) {
        return new DailyOperationsSectionLoader() {
            @Override
            public String title() {
                return title;
            }

            @Override
            public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) {
                return AnalyzerDashboardSectionSnapshot.success(title, new JLabel(title));
            }
        };
    }

    private static DailyOperationsSectionLoader failingSection(String title, String message) {
        return new DailyOperationsSectionLoader() {
            @Override
            public String title() {
                return title;
            }

            @Override
            public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) {
                throw new IllegalStateException(message);
            }
        };
    }

    private static AnalyzerContext context() {
        return new AnalyzerContext(
                new com.tbg.wms.core.AppConfig(),
                Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC)
        );
    }
}

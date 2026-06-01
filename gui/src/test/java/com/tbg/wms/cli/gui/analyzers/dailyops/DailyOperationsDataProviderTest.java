package com.tbg.wms.cli.gui.analyzers.dailyops;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyOperationsDataProviderTest {

    @Test
    void load_shouldAssembleAllSectionSnapshotsInOrder() throws Exception {
        DailyOperationsDataProvider provider = new DailyOperationsDataProvider(List.of(
                section("Case Pick Summary"),
                section("Case Pick Shift Throughput")
        ), () -> fakeDataSource(), Runnable::run);

        AnalyzerResult<AnalyzerDashboardSectionSnapshot> result = provider.load(context());

        assertEquals(List.of("Case Pick Summary", "Case Pick Shift Throughput"),
                result.rows().stream().map(AnalyzerDashboardSectionSnapshot::title).toList());
    }

    @Test
    void load_shouldCaptureSectionFailureAndContinue() throws Exception {
        DailyOperationsDataProvider provider = new DailyOperationsDataProvider(List.of(
                section("Case Pick Summary"),
                failingSection("Unload and Load Activity", "boom")
        ), () -> fakeDataSource(), Runnable::run);

        AnalyzerResult<AnalyzerDashboardSectionSnapshot> result = provider.load(context());

        assertFalse(result.rows().get(0).failed());
        assertTrue(result.rows().get(1).failed());
        assertEquals("boom", result.rows().get(1).errorText());
    }

    @Test
    void load_shouldReuseOneDataSourceAcrossAllSections() throws Exception {
        AtomicInteger supplierCalls = new AtomicInteger();
        DataSource sharedDataSource = fakeDataSource();
        RecordingSectionLoader first = new RecordingSectionLoader("Case Pick Summary");
        RecordingSectionLoader second = new RecordingSectionLoader("Case Pick Shift Throughput");
        DailyOperationsDataProvider provider = new DailyOperationsDataProvider(
                List.of(first, second),
                () -> {
                    supplierCalls.incrementAndGet();
                    return sharedDataSource;
                },
                Runnable::run
        );

        provider.load(context());

        assertEquals(1, supplierCalls.get());
        assertSame(sharedDataSource, first.seenDataSource);
        assertSame(sharedDataSource, second.seenDataSource);
    }

    private static DailyOperationsSectionLoader section(String title) {
        return new DailyOperationsSectionLoader() {
            @Override
            public String title() {
                return title;
            }

            @Override
            public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context, DataSource dataSource) {
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
            public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context, DataSource dataSource) {
                throw new IllegalStateException(message);
            }
        };
    }

    private static DataSource fakeDataSource() {
        return new DataSource() {
            @Override
            public java.sql.Connection getConnection() {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.sql.Connection getConnection(String username, String password) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T unwrap(Class<T> iface) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }

            @Override
            public java.io.PrintWriter getLogWriter() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setLoginTimeout(int seconds) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static final class RecordingSectionLoader implements DailyOperationsSectionLoader {
        private final String title;
        private DataSource seenDataSource;

        private RecordingSectionLoader(String title) {
            this.title = title;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context, DataSource dataSource) {
            seenDataSource = dataSource;
            return AnalyzerDashboardSectionSnapshot.success(title, new JLabel(title));
        }
    }

    private static AnalyzerContext context() {
        return new AnalyzerContext(
                new com.tbg.wms.core.AppConfig(),
                Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC)
        );
    }
}

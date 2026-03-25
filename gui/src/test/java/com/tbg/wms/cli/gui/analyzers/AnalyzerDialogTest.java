package com.tbg.wms.cli.gui.analyzers;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyzerDialogTest {

    @Test
    void renderer_shouldApplyAnalyzerRowColors() {
        AnalyzerRowStyler<String> styler = row -> AnalyzerRowStyle.of(Color.YELLOW, Color.BLACK);
        AnalyzerTableModel<String> model = new AnalyzerTableModel<>(row -> row, List.of("value"));
        model.setRows(List.of("value"));
        JTable table = new JTable(model);
        AnalyzerTableCellRenderer<String> renderer = new AnalyzerTableCellRenderer<>(model, styler);

        Component component = renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);

        assertEquals(Color.YELLOW, component.getBackground());
        assertEquals(Color.BLACK, component.getForeground());
    }

    @Test
    void dialog_shouldSelectDefaultAnalyzerAndLoadOnOpen() {
        ControlledExecutor executor = new ControlledExecutor();
        FakeAnalyzerDefinition analyzer = new FakeAnalyzerDefinition("unpicked-partials", "Unpicked Partials");
        AnalyzerDialog dialog = new AnalyzerDialog(null,
                new AnalyzerRegistry(List.of(analyzer)),
                new AnalyzerContext(new com.tbg.wms.core.AppConfig(),
                        Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC)),
                executor);

        dialog.openForTest();
        executor.runNext();

        assertEquals("Unpicked Partials", dialog.selectedAnalyzerNameForTest());
        assertEquals(1, analyzer.loadCount());
        assertEquals(1, dialog.tableRowCountForTest());
    }

    @Test
    void manualRefresh_shouldReloadSelectedAnalyzer() {
        ControlledExecutor executor = new ControlledExecutor();
        FakeAnalyzerDefinition analyzer = new FakeAnalyzerDefinition("unpicked-partials", "Unpicked Partials");
        AnalyzerDialog dialog = new AnalyzerDialog(null,
                new AnalyzerRegistry(List.of(analyzer)),
                new AnalyzerContext(new com.tbg.wms.core.AppConfig(),
                        Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC)),
                executor);

        dialog.openForTest();
        executor.runNext();
        dialog.triggerManualRefreshForTest();
        executor.runNext();

        assertEquals(2, analyzer.loadCount());
    }

    @Test
    void dialog_shouldRenderDashboardPresentationWhenAnalyzerUsesDashboard() {
        ControlledExecutor executor = new ControlledExecutor();
        FakeDashboardAnalyzerDefinition analyzer = new FakeDashboardAnalyzerDefinition();
        AnalyzerDialog dialog = new AnalyzerDialog(null,
                new AnalyzerRegistry(List.of(analyzer)),
                new AnalyzerContext(new com.tbg.wms.core.AppConfig(),
                        Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC)),
                executor);

        dialog.openForTest();
        executor.runNext();

        assertEquals("dashboard", dialog.activePresentationForTest());
    }

    @Test
    void dialog_shouldEnterLoadingStateBeforeAsyncAnalyzerCompletes() {
        ControlledExecutor executor = new ControlledExecutor();
        FakeAnalyzerDefinition analyzer = new FakeAnalyzerDefinition("unpicked-partials", "Unpicked Partials");
        AnalyzerDialog dialog = new AnalyzerDialog(null,
                new AnalyzerRegistry(List.of(analyzer)),
                new AnalyzerContext(new com.tbg.wms.core.AppConfig(),
                        Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC)),
                executor);

        dialog.openForTest();

        assertEquals("Loading...", dialog.statusTextForTest());
        assertEquals(0, dialog.tableRowCountForTest());

        executor.runNext();

        assertEquals("Loaded Unpicked Partials.", dialog.statusTextForTest());
        assertEquals(1, dialog.tableRowCountForTest());
    }

    @Test
    void dialog_shouldIgnoreStaleAsyncResultsAfterSwitchingAnalyzers() {
        ControlledExecutor executor = new ControlledExecutor();
        FakeAnalyzerDefinition first = new FakeAnalyzerDefinition("first", "First", "first-row");
        FakeAnalyzerDefinition second = new FakeAnalyzerDefinition("second", "Second", "second-row");
        AnalyzerDialog dialog = new AnalyzerDialog(null,
                new AnalyzerRegistry(List.of(first, second)),
                new AnalyzerContext(new com.tbg.wms.core.AppConfig(),
                        Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC)),
                executor);

        dialog.openForTest();
        dialog.selectAnalyzerForTest(1);

        executor.runNext();
        assertEquals("Second", dialog.selectedAnalyzerNameForTest());
        assertEquals(0, dialog.tableRowCountForTest());

        executor.runNext();
        assertEquals("second-row", dialog.firstTableValueForTest());
        assertEquals("Loaded Second.", dialog.statusTextForTest());
    }

    private static final class FakeAnalyzerDefinition implements AnalyzerDefinition<String> {
        private final String id;
        private final String displayName;
        private final Queue<String> rows = new ArrayDeque<>();
        private int loadCount;

        private FakeAnalyzerDefinition(String id, String displayName) {
            this(id, displayName, "row");
        }

        private FakeAnalyzerDefinition(String id, String displayName, String row) {
            this.id = id;
            this.displayName = displayName;
            this.rows.add(row);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public Duration defaultRefreshInterval() {
            return Duration.ofMinutes(1);
        }

        @Override
        public AnalyzerDataProvider<String> createProvider(AnalyzerContext context) {
            return ignored -> {
                loadCount++;
                String row = rows.isEmpty() ? "row" : rows.remove();
                return new AnalyzerResult<>(List.of(row), Instant.now(context.clock()));
            };
        }

        @Override
        public AnalyzerColumnSet<String> columns() {
            return () -> List.of(new AnalyzerColumnSet.Column<>("value", row -> row));
        }

        @Override
        public AnalyzerRowStyler<String> rowStyler() {
            return row -> AnalyzerRowStyle.of(AnalyzerColorPalette.DEFAULT_BACKGROUND, AnalyzerColorPalette.DEFAULT_FOREGROUND);
        }

        @Override
        public AnalyzerPresentation<String> presentation() {
            return TableAnalyzerPresentation.of(columns(), rowStyler());
        }

        private int loadCount() {
            return loadCount;
        }
    }

    private static final class FakeDashboardAnalyzerDefinition implements AnalyzerDefinition<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot> {

        @Override
        public String id() {
            return "daily-operations";
        }

        @Override
        public String displayName() {
            return "Daily Operations";
        }

        @Override
        public Duration defaultRefreshInterval() {
            return Duration.ofMinutes(1);
        }

        @Override
        public AnalyzerDataProvider<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot> createProvider(AnalyzerContext context) {
            return ignored -> new AnalyzerResult<>(
                    List.of(com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot.success("Section", new JLabel("row"))),
                    Instant.now(context.clock())
            );
        }

        @Override
        public AnalyzerColumnSet<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot> columns() {
            return () -> List.of(new AnalyzerColumnSet.Column<>("value", row -> row));
        }

        @Override
        public AnalyzerRowStyler<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot> rowStyler() {
            return row -> AnalyzerRowStyle.of(AnalyzerColorPalette.DEFAULT_BACKGROUND, AnalyzerColorPalette.DEFAULT_FOREGROUND);
        }

        @Override
        public AnalyzerPresentation<com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot> presentation() {
            return DashboardAnalyzerPresentation.of();
        }
    }

    private static final class ControlledExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        private void runNext() {
            Runnable task = tasks.remove();
            task.run();
            try {
                SwingUtilities.invokeAndWait(() -> {
                });
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}

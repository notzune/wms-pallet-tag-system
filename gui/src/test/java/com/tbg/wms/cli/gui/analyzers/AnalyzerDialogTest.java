package com.tbg.wms.cli.gui.analyzers;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

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
        FakeAnalyzerDefinition analyzer = new FakeAnalyzerDefinition("unpicked-partials", "Unpicked Partials");
        AnalyzerDialog dialog = new AnalyzerDialog(null,
                new AnalyzerRegistry(List.of(analyzer)),
                new AnalyzerContext(new com.tbg.wms.core.AppConfig(),
                        Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC)));

        dialog.openForTest();

        assertEquals("Unpicked Partials", dialog.selectedAnalyzerNameForTest());
        assertEquals(1, analyzer.loadCount());
        assertEquals(1, dialog.tableRowCountForTest());
    }

    @Test
    void manualRefresh_shouldReloadSelectedAnalyzer() {
        FakeAnalyzerDefinition analyzer = new FakeAnalyzerDefinition("unpicked-partials", "Unpicked Partials");
        AnalyzerDialog dialog = new AnalyzerDialog(null,
                new AnalyzerRegistry(List.of(analyzer)),
                new AnalyzerContext(new com.tbg.wms.core.AppConfig(),
                        Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC)));

        dialog.openForTest();
        dialog.triggerManualRefreshForTest();

        assertEquals(2, analyzer.loadCount());
    }

    @Test
    void dialog_shouldRenderDashboardPresentationWhenAnalyzerUsesDashboard() {
        FakeDashboardAnalyzerDefinition analyzer = new FakeDashboardAnalyzerDefinition();
        AnalyzerDialog dialog = new AnalyzerDialog(null,
                new AnalyzerRegistry(List.of(analyzer)),
                new AnalyzerContext(new com.tbg.wms.core.AppConfig(),
                        Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC)));

        dialog.openForTest();

        assertEquals("dashboard", dialog.activePresentationForTest());
    }

    private static final class FakeAnalyzerDefinition implements AnalyzerDefinition<String> {
        private final String id;
        private final String displayName;
        private int loadCount;

        private FakeAnalyzerDefinition(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
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
                return new AnalyzerResult<>(List.of("row"), Instant.now(context.clock()));
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

    private static final class FakeDashboardAnalyzerDefinition implements AnalyzerDefinition<String> {

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
        public AnalyzerDataProvider<String> createProvider(AnalyzerContext context) {
            return ignored -> new AnalyzerResult<>(List.of("row"), Instant.now(context.clock()));
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
            return DashboardAnalyzerPresentation.of();
        }
    }
}

package com.tbg.wms.cli.gui.analyzers.dailyops;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.DashboardAnalyzerPresentation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DailyOperationsAnalyzerDefinitionTest {

    @Test
    void definition_shouldExposeDashboardPresentationAndMetadata() {
        DailyOperationsAnalyzerDefinition definition = new DailyOperationsAnalyzerDefinition();

        assertEquals("daily-operations", definition.id());
        assertEquals("Daily Operations", definition.displayName());
        assertInstanceOf(DashboardAnalyzerPresentation.class, definition.presentation());
        assertInstanceOf(DailyOperationsDataProvider.class, definition.createProvider(context()));
    }

    private static AnalyzerContext context() {
        return new AnalyzerContext(
                new com.tbg.wms.core.AppConfig(),
                Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC)
        );
    }
}

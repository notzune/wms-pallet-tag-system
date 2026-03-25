package com.tbg.wms.cli.gui.analyzers.dockdoors;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.TableAnalyzerPresentation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AllDockDoorsAnalyzerDefinitionTest {

    @Test
    void definition_shouldExposeAllDockDoorsAsTableAnalyzer() {
        AllDockDoorsAnalyzerDefinition definition = new AllDockDoorsAnalyzerDefinition();

        assertEquals("all-dock-doors", definition.id());
        assertEquals("All Dock Doors", definition.displayName());
        assertInstanceOf(TableAnalyzerPresentation.class, definition.presentation());
        assertInstanceOf(AllDockDoorsDataProvider.class, definition.createProvider(context()));
    }

    private static AnalyzerContext context() {
        return new AnalyzerContext(
                new com.tbg.wms.core.AppConfig(),
                Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC)
        );
    }
}

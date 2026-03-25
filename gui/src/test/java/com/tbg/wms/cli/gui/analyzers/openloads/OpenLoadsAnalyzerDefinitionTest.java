package com.tbg.wms.cli.gui.analyzers.openloads;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.TableAnalyzerPresentation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OpenLoadsAnalyzerDefinitionTest {

    @Test
    void definition_shouldExposeOpenLoadsAsTableAnalyzer() {
        OpenLoadsAnalyzerDefinition definition = new OpenLoadsAnalyzerDefinition();

        assertEquals("open-loads", definition.id());
        assertEquals("Open Loads", definition.displayName());
        assertInstanceOf(TableAnalyzerPresentation.class, definition.presentation());
        assertInstanceOf(OpenLoadsDataProvider.class, definition.createProvider(context()));
    }

    private static AnalyzerContext context() {
        return new AnalyzerContext(
                new com.tbg.wms.core.AppConfig(),
                Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC)
        );
    }
}

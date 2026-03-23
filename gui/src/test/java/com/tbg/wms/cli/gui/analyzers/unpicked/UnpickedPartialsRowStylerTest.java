package com.tbg.wms.cli.gui.analyzers.unpicked;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColorPalette;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyle;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnpickedPartialsRowStylerTest {

    @Test
    void styleFor_shouldReturnExpectedPaletteForLoblaws() {
        UnpickedPartialsRowStyler styler = new UnpickedPartialsRowStyler();

        AnalyzerRowStyle style = styler.styleFor(new UnpickedPartialsRow(
                "3002",
                LocalDateTime.parse("2026-03-23T10:00:00"),
                "1000057168",
                "LOBLAWS",
                255,
                0,
                0,
                0,
                255,
                "3002-JERSEY CITY",
                LocalDateTime.parse("2026-03-23T10:00:00"),
                "LOBLAWS DC 67",
                "1000 MAIN ST",
                "AJAX",
                "ON",
                UnpickedPartialsRule.LOBLAWS
        ));

        assertEquals(AnalyzerColorPalette.LOBLAWS_YELLOW, style.background());
        assertEquals(AnalyzerColorPalette.DEFAULT_FOREGROUND, style.foreground());
    }
}

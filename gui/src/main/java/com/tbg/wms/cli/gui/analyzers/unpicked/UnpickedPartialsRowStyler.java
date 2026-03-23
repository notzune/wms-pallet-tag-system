package com.tbg.wms.cli.gui.analyzers.unpicked;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColorPalette;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyle;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyler;

public final class UnpickedPartialsRowStyler implements AnalyzerRowStyler<UnpickedPartialsRow> {

    @Override
    public AnalyzerRowStyle styleFor(UnpickedPartialsRow row) {
        return switch (row.rule()) {
            case LOBLAWS -> AnalyzerRowStyle.of(AnalyzerColorPalette.LOBLAWS_YELLOW, AnalyzerColorPalette.DEFAULT_FOREGROUND);
            case CORE_MARK -> AnalyzerRowStyle.of(AnalyzerColorPalette.CORE_MARK_ORANGE, AnalyzerColorPalette.DEFAULT_FOREGROUND);
            case WALMART -> AnalyzerRowStyle.of(AnalyzerColorPalette.WALMART_BLUE, AnalyzerColorPalette.DEFAULT_FOREGROUND);
            case SOBEYS -> AnalyzerRowStyle.of(AnalyzerColorPalette.SOBEYS_LAVENDER, AnalyzerColorPalette.DEFAULT_FOREGROUND);
            case METRO -> AnalyzerRowStyle.of(AnalyzerColorPalette.METRO_GREEN, AnalyzerColorPalette.DEFAULT_FOREGROUND);
            case MR_DAIRY, DEFAULT -> AnalyzerRowStyle.of(AnalyzerColorPalette.DEFAULT_BACKGROUND, AnalyzerColorPalette.DEFAULT_FOREGROUND);
        };
    }
}

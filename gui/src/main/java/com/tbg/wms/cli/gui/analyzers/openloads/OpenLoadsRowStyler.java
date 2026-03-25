package com.tbg.wms.cli.gui.analyzers.openloads;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColorPalette;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyle;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyler;

public final class OpenLoadsRowStyler implements AnalyzerRowStyler<OpenLoadsRow> {

    @Override
    public AnalyzerRowStyle styleFor(OpenLoadsRow row) {
        if ("SHORT".equalsIgnoreCase(row.shortFlag())) {
            return AnalyzerRowStyle.of(AnalyzerColorPalette.CORE_MARK_ORANGE, AnalyzerColorPalette.DEFAULT_FOREGROUND);
        }
        return AnalyzerRowStyle.of(AnalyzerColorPalette.DEFAULT_BACKGROUND, AnalyzerColorPalette.DEFAULT_FOREGROUND);
    }
}

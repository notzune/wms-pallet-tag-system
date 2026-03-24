package com.tbg.wms.cli.gui.analyzers;

import java.awt.Color;
import java.util.Objects;

public record AnalyzerRowStyle(
        Color background,
        Color foreground
) {
    public AnalyzerRowStyle {
        Objects.requireNonNull(background, "background cannot be null");
        Objects.requireNonNull(foreground, "foreground cannot be null");
    }

    public static AnalyzerRowStyle of(Color background, Color foreground) {
        return new AnalyzerRowStyle(background, foreground);
    }
}

package com.tbg.wms.cli.gui.analyzers.dashboard;

import javax.swing.JComponent;
import java.util.Objects;

public record AnalyzerDashboardSectionSnapshot(
        String title,
        boolean failed,
        String errorText,
        JComponent content
) {
    public AnalyzerDashboardSectionSnapshot {
        Objects.requireNonNull(title, "title cannot be null");
    }

    public static AnalyzerDashboardSectionSnapshot success(String title, JComponent content) {
        return new AnalyzerDashboardSectionSnapshot(title, false, "", Objects.requireNonNull(content, "content cannot be null"));
    }

    public static AnalyzerDashboardSectionSnapshot failure(String title, String errorText) {
        return new AnalyzerDashboardSectionSnapshot(title, true, Objects.requireNonNull(errorText, "errorText cannot be null"), null);
    }
}

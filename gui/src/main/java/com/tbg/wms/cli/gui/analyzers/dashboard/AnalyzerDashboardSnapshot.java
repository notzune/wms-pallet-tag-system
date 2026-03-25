package com.tbg.wms.cli.gui.analyzers.dashboard;

import java.util.List;
import java.util.Objects;

public record AnalyzerDashboardSnapshot(
        List<AnalyzerDashboardSectionSnapshot> sections
) {
    public AnalyzerDashboardSnapshot {
        sections = List.copyOf(Objects.requireNonNull(sections, "sections cannot be null"));
    }
}

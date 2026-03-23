package com.tbg.wms.cli.gui.analyzers;

import java.time.Instant;
import java.util.List;

public record AnalyzerDialogState(
        AnalyzerDefinition<?> selectedAnalyzer,
        List<?> rows,
        boolean loading,
        Instant lastUpdated
) {
}

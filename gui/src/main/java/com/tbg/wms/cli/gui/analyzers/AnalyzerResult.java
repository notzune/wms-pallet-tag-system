package com.tbg.wms.cli.gui.analyzers;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AnalyzerResult<R>(
        List<R> rows,
        Instant fetchedAt
) {
    public AnalyzerResult {
        rows = List.copyOf(Objects.requireNonNull(rows, "rows cannot be null"));
        Objects.requireNonNull(fetchedAt, "fetchedAt cannot be null");
    }
}

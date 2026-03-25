package com.tbg.wms.cli.gui.analyzers;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AnalyzerLoadSnapshot<R>(
        List<R> rows,
        Instant fetchedAt
) {
    public AnalyzerLoadSnapshot {
        rows = List.copyOf(Objects.requireNonNull(rows, "rows cannot be null"));
        Objects.requireNonNull(fetchedAt, "fetchedAt cannot be null");
    }

    public static <R> AnalyzerLoadSnapshot<R> fromResult(AnalyzerResult<R> result) {
        return new AnalyzerLoadSnapshot<>(result.rows(), result.fetchedAt());
    }
}

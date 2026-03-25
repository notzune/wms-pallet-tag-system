package com.tbg.wms.cli.gui.analyzers;

import java.util.Objects;

public record TableAnalyzerPresentation<R>(
        AnalyzerColumnSet<R> columns,
        AnalyzerRowStyler<R> rowStyler
) implements AnalyzerPresentation<R> {

    public TableAnalyzerPresentation {
        Objects.requireNonNull(columns, "columns cannot be null");
        Objects.requireNonNull(rowStyler, "rowStyler cannot be null");
    }

    public static <R> TableAnalyzerPresentation<R> of(
            AnalyzerColumnSet<R> columns,
            AnalyzerRowStyler<R> rowStyler
    ) {
        return new TableAnalyzerPresentation<>(columns, rowStyler);
    }

    @Override
    public String id() {
        return "table";
    }
}

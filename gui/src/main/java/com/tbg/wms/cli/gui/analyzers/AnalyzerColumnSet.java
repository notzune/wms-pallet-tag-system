package com.tbg.wms.cli.gui.analyzers;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public interface AnalyzerColumnSet<R> {

    List<Column<R>> columns();

    record Column<R>(
            String name,
            Function<R, Object> valueAccessor
    ) {
        public Column {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(valueAccessor, "valueAccessor cannot be null");
        }

        public Column(String name) {
            this(name, row -> "");
        }
    }
}

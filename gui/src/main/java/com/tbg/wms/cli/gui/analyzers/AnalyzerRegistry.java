package com.tbg.wms.cli.gui.analyzers;

import java.util.List;
import java.util.Objects;

public final class AnalyzerRegistry {

    private final List<AnalyzerDefinition<?>> definitions;

    public AnalyzerRegistry(List<AnalyzerDefinition<?>> definitions) {
        this.definitions = List.copyOf(Objects.requireNonNull(definitions, "definitions cannot be null"));
        if (this.definitions.isEmpty()) {
            throw new IllegalArgumentException("definitions cannot be empty");
        }
    }

    public List<AnalyzerDefinition<?>> definitions() {
        return definitions;
    }

    public AnalyzerDefinition<?> defaultAnalyzer() {
        return definitions.get(0);
    }
}

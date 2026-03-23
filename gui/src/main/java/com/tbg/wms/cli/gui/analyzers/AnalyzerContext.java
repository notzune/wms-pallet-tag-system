package com.tbg.wms.cli.gui.analyzers;

import com.tbg.wms.core.AppConfig;

import java.time.Clock;
import java.util.Objects;

public record AnalyzerContext(
        AppConfig config,
        Clock clock
) {
    public AnalyzerContext {
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(clock, "clock cannot be null");
    }
}

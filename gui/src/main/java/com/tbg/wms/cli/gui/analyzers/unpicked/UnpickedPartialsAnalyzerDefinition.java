package com.tbg.wms.cli.gui.analyzers.unpicked;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColumnSet;
import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDefinition;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class UnpickedPartialsAnalyzerDefinition implements AnalyzerDefinition<Object> {

    @Override
    public String id() {
        return "unpicked-partials";
    }

    @Override
    public String displayName() {
        return "Unpicked Partials";
    }

    @Override
    public Duration defaultRefreshInterval() {
        return Duration.ofMinutes(1);
    }

    @Override
    public AnalyzerDataProvider<Object> createProvider(AnalyzerContext context) {
        return ignored -> new AnalyzerResult<>(List.of(), Instant.now(context.clock()));
    }

    @Override
    public AnalyzerColumnSet<Object> columns() {
        return List::of;
    }
}

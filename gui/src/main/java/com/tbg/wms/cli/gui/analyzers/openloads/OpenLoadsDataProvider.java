package com.tbg.wms.cli.gui.analyzers.openloads;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class OpenLoadsDataProvider implements AnalyzerDataProvider<OpenLoadsRow> {

    private final OpenLoadsQueryService queryService;
    private final Clock clock;

    public OpenLoadsDataProvider(OpenLoadsQueryService queryService, Clock clock) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public AnalyzerResult<OpenLoadsRow> load(AnalyzerContext context) throws Exception {
        return new AnalyzerResult<>(queryService.fetchRows(context.config()), Instant.now(clock));
    }
}

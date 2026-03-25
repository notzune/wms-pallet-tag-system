package com.tbg.wms.cli.gui.analyzers.dockdoors;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class AllDockDoorsDataProvider implements AnalyzerDataProvider<AllDockDoorsRow> {

    private final AllDockDoorsQueryService queryService;
    private final Clock clock;

    public AllDockDoorsDataProvider(AllDockDoorsQueryService queryService, Clock clock) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public AnalyzerResult<AllDockDoorsRow> load(AnalyzerContext context) throws Exception {
        return new AnalyzerResult<>(queryService.fetchRows(context.config()), Instant.now(clock));
    }
}

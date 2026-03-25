package com.tbg.wms.cli.gui.analyzers.openloads;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColumnSet;
import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDefinition;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyler;

import java.time.Duration;

public final class OpenLoadsAnalyzerDefinition implements AnalyzerDefinition<OpenLoadsRow> {

    @Override
    public String id() {
        return "open-loads";
    }

    @Override
    public String displayName() {
        return "Open Loads";
    }

    @Override
    public Duration defaultRefreshInterval() {
        return Duration.ofMinutes(5);
    }

    @Override
    public AnalyzerDataProvider<OpenLoadsRow> createProvider(AnalyzerContext context) {
        return new OpenLoadsDataProvider(new OpenLoadsQueryService(), context.clock());
    }

    @Override
    public AnalyzerColumnSet<OpenLoadsRow> columns() {
        return new OpenLoadsColumns();
    }

    @Override
    public AnalyzerRowStyler<OpenLoadsRow> rowStyler() {
        return new OpenLoadsRowStyler();
    }
}

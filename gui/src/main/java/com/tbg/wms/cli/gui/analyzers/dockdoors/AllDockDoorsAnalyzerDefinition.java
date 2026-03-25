package com.tbg.wms.cli.gui.analyzers.dockdoors;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColumnSet;
import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDefinition;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyler;

import java.time.Duration;

public final class AllDockDoorsAnalyzerDefinition implements AnalyzerDefinition<AllDockDoorsRow> {

    @Override
    public String id() {
        return "all-dock-doors";
    }

    @Override
    public String displayName() {
        return "All Dock Doors";
    }

    @Override
    public Duration defaultRefreshInterval() {
        return Duration.ofMinutes(5);
    }

    @Override
    public AnalyzerDataProvider<AllDockDoorsRow> createProvider(AnalyzerContext context) {
        return new AllDockDoorsDataProvider(new AllDockDoorsQueryService(), context.clock());
    }

    @Override
    public AnalyzerColumnSet<AllDockDoorsRow> columns() {
        return new AllDockDoorsColumns();
    }

    @Override
    public AnalyzerRowStyler<AllDockDoorsRow> rowStyler() {
        return new AllDockDoorsRowStyler();
    }
}

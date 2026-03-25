package com.tbg.wms.cli.gui.analyzers.dailyops;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColumnSet;
import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDefinition;
import com.tbg.wms.cli.gui.analyzers.AnalyzerPresentation;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyle;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyler;
import com.tbg.wms.cli.gui.analyzers.DashboardAnalyzerPresentation;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot;

import java.time.Duration;
import java.util.List;

public final class DailyOperationsAnalyzerDefinition implements AnalyzerDefinition<AnalyzerDashboardSectionSnapshot> {

    @Override
    public String id() {
        return "daily-operations";
    }

    @Override
    public String displayName() {
        return "Daily Operations";
    }

    @Override
    public Duration defaultRefreshInterval() {
        return Duration.ofMinutes(5);
    }

    @Override
    public AnalyzerDataProvider<AnalyzerDashboardSectionSnapshot> createProvider(AnalyzerContext context) {
        return new DailyOperationsDataProvider(List.of());
    }

    @Override
    public AnalyzerColumnSet<AnalyzerDashboardSectionSnapshot> columns() {
        return List::of;
    }

    @Override
    public AnalyzerRowStyler<AnalyzerDashboardSectionSnapshot> rowStyler() {
        return row -> AnalyzerRowStyle.of(null, null);
    }

    @Override
    public AnalyzerPresentation<AnalyzerDashboardSectionSnapshot> presentation() {
        return DashboardAnalyzerPresentation.of();
    }
}

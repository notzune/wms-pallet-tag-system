package com.tbg.wms.cli.gui.analyzers.dailyops;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DailyOperationsDataProvider implements AnalyzerDataProvider<AnalyzerDashboardSectionSnapshot> {

    private final List<DailyOperationsSectionLoader> sectionLoaders;

    public DailyOperationsDataProvider(List<DailyOperationsSectionLoader> sectionLoaders) {
        this.sectionLoaders = List.copyOf(Objects.requireNonNull(sectionLoaders, "sectionLoaders cannot be null"));
    }

    @Override
    public AnalyzerResult<AnalyzerDashboardSectionSnapshot> load(AnalyzerContext context) {
        List<AnalyzerDashboardSectionSnapshot> sections = new ArrayList<>();
        for (DailyOperationsSectionLoader loader : sectionLoaders) {
            try {
                sections.add(loader.loadSection(context));
            } catch (Exception ex) {
                sections.add(AnalyzerDashboardSectionSnapshot.failure(loader.title(), ex.getMessage()));
            }
        }
        return new AnalyzerResult<>(sections, Instant.now(context.clock()));
    }
}

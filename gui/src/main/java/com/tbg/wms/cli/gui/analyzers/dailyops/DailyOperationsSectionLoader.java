package com.tbg.wms.cli.gui.analyzers.dailyops;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot;

public interface DailyOperationsSectionLoader {

    String title();

    AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) throws Exception;
}

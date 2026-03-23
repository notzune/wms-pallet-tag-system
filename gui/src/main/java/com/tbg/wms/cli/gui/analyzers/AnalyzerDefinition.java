package com.tbg.wms.cli.gui.analyzers;

import java.time.Duration;

public interface AnalyzerDefinition<R> {

    String id();

    String displayName();

    Duration defaultRefreshInterval();

    AnalyzerDataProvider<R> createProvider(AnalyzerContext context);

    AnalyzerColumnSet<R> columns();
}

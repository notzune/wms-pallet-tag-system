package com.tbg.wms.cli.gui.analyzers.dailyops;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot;
import com.tbg.wms.core.db.DataSourceFactory;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DailyOperationsDataProvider implements AnalyzerDataProvider<AnalyzerDashboardSectionSnapshot> {

    private final List<DailyOperationsSectionLoader> sectionLoaders;
    private final Function<AnalyzerContext, DataSource> dataSourceFactory;
    private final Executor executor;

    public DailyOperationsDataProvider(List<DailyOperationsSectionLoader> sectionLoaders) {
        this.sectionLoaders = List.copyOf(Objects.requireNonNull(sectionLoaders, "sectionLoaders cannot be null"));
        this.dataSourceFactory = context -> new DataSourceFactory(context.config()).create();
        this.executor = ForkJoinPool.commonPool();
    }

    DailyOperationsDataProvider(
            List<DailyOperationsSectionLoader> sectionLoaders,
            Supplier<DataSource> dataSourceSupplier,
            Executor executor
    ) {
        this.sectionLoaders = List.copyOf(Objects.requireNonNull(sectionLoaders, "sectionLoaders cannot be null"));
        Objects.requireNonNull(dataSourceSupplier, "dataSourceSupplier cannot be null");
        this.dataSourceFactory = context -> dataSourceSupplier.get();
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
    }

    @Override
    public AnalyzerResult<AnalyzerDashboardSectionSnapshot> load(AnalyzerContext context) {
        try {
            Supplier<DataSource> supplier = () -> dataSourceFactory.apply(context);
            List<AnalyzerDashboardSectionSnapshot> sections = new DailyOperationsLoadCoordinator(supplier, executor)
                    .load(context, sectionLoaders);
            return new AnalyzerResult<>(sections, Instant.now(context.clock()));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load daily operations", ex);
        }
    }
}

package com.tbg.wms.cli.gui.analyzers.dailyops;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

final class DailyOperationsLoadCoordinator {

    private final Supplier<DataSource> dataSourceSupplier;
    private final Executor executor;

    DailyOperationsLoadCoordinator(Supplier<DataSource> dataSourceSupplier, Executor executor) {
        this.dataSourceSupplier = Objects.requireNonNull(dataSourceSupplier, "dataSourceSupplier cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
    }

    List<AnalyzerDashboardSectionSnapshot> load(AnalyzerContext context, List<DailyOperationsSectionLoader> sectionLoaders) throws Exception {
        DataSource dataSource = dataSourceSupplier.get();
        try {
            List<CompletableFuture<AnalyzerDashboardSectionSnapshot>> futures = new ArrayList<>();
            for (DailyOperationsSectionLoader loader : sectionLoaders) {
                futures.add(CompletableFuture.supplyAsync(() -> loadSection(loader, context, dataSource), executor));
            }
            List<AnalyzerDashboardSectionSnapshot> sections = new ArrayList<>();
            for (CompletableFuture<AnalyzerDashboardSectionSnapshot> future : futures) {
                sections.add(future.join());
            }
            return sections;
        } finally {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                hikariDataSource.close();
            }
        }
    }

    private AnalyzerDashboardSectionSnapshot loadSection(
            DailyOperationsSectionLoader loader,
            AnalyzerContext context,
            DataSource dataSource
    ) {
        try {
            return loader.loadSection(context, dataSource);
        } catch (Exception ex) {
            return AnalyzerDashboardSectionSnapshot.failure(loader.title(), ex.getMessage());
        }
    }
}

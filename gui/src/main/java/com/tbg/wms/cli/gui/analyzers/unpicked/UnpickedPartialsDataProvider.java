package com.tbg.wms.cli.gui.analyzers.unpicked;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class UnpickedPartialsDataProvider implements AnalyzerDataProvider<UnpickedPartialsRow> {

    private final UnpickedPartialsQueryService queryService;
    private final UnpickedPartialsRuleClassifier classifier;
    private final Clock clock;

    public UnpickedPartialsDataProvider(
            UnpickedPartialsQueryService queryService,
            UnpickedPartialsRuleClassifier classifier,
            Clock clock
    ) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
        this.classifier = Objects.requireNonNull(classifier, "classifier cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public AnalyzerResult<UnpickedPartialsRow> load(AnalyzerContext context) throws Exception {
        List<UnpickedPartialsRow> rows = queryService.fetchRows(context.config()).stream()
                .map(row -> new UnpickedPartialsRow(
                        row.warehouseId(),
                        row.appointment(),
                        row.orderNumber(),
                        row.soldToCustomer(),
                        row.orderedQuantity(),
                        row.allocatedQuantity(),
                        row.unallocatedQuantity(),
                        row.completedQuantity(),
                        row.remainingQuantity(),
                        row.warehouseName(),
                        Instant.now(clock).atZone(clock.getZone()).toLocalDateTime(),
                        row.soldToName(),
                        row.addressLine1(),
                        row.addressCity(),
                        row.addressState(),
                        classifier.classify(row.soldToName(), row.soldToCustomer())
                ))
                .toList();
        return new AnalyzerResult<>(rows, Instant.now(clock));
    }
}

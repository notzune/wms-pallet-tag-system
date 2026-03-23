package com.tbg.wms.cli.gui.analyzers.unpicked;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColumnSet;
import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDataProvider;
import com.tbg.wms.cli.gui.analyzers.AnalyzerDefinition;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;
import com.tbg.wms.cli.gui.analyzers.AnalyzerRowStyler;

import java.time.Duration;
import java.time.Clock;

public final class UnpickedPartialsAnalyzerDefinition implements AnalyzerDefinition<UnpickedPartialsRow> {

    @Override
    public String id() {
        return "unpicked-partials";
    }

    @Override
    public String displayName() {
        return "Unpicked Partials";
    }

    @Override
    public Duration defaultRefreshInterval() {
        return Duration.ofMinutes(1);
    }

    @Override
    public AnalyzerDataProvider<UnpickedPartialsRow> createProvider(AnalyzerContext context) {
        return new UnpickedPartialsDataProvider(
                new UnpickedPartialsQueryService(),
                new UnpickedPartialsRuleClassifier(),
                context.clock()
        );
    }

    @Override
    public AnalyzerColumnSet<UnpickedPartialsRow> columns() {
        return new UnpickedPartialsColumns();
    }

    @Override
    public AnalyzerRowStyler<UnpickedPartialsRow> rowStyler() {
        return new UnpickedPartialsRowStyler();
    }
}

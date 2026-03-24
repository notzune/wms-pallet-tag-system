package com.tbg.wms.cli.gui.analyzers;

@FunctionalInterface
public interface AnalyzerDataProvider<R> {

    AnalyzerResult<R> load(AnalyzerContext context) throws Exception;
}

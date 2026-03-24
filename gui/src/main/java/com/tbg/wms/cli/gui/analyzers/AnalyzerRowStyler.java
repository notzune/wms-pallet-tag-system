package com.tbg.wms.cli.gui.analyzers;

@FunctionalInterface
public interface AnalyzerRowStyler<R> {

    AnalyzerRowStyle styleFor(R row);
}

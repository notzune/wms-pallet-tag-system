package com.tbg.wms.cli.gui.analyzers;

public record DashboardAnalyzerPresentation<R>() implements AnalyzerPresentation<R> {

    public static <R> DashboardAnalyzerPresentation<R> of() {
        return new DashboardAnalyzerPresentation<>();
    }

    @Override
    public String id() {
        return "dashboard";
    }
}

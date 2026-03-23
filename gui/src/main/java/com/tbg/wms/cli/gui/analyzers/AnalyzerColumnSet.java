package com.tbg.wms.cli.gui.analyzers;

import java.util.List;

public interface AnalyzerColumnSet<R> {

    List<Column<R>> columns();

    record Column<R>(
            String name
    ) {
    }
}

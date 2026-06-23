package com.tbg.wms.v2.app.queue;

import com.tbg.wms.v2.app.print.PrintExecutionResult;

import java.util.List;

public record QueueExecutionResult(
        List<PrintExecutionResult> itemResults,
        int artifactsWritten,
        int tasksDispatched
) {
    public QueueExecutionResult {
        if (itemResults == null) {
            throw new IllegalArgumentException("itemResults are required");
        }
        if (artifactsWritten < 0 || tasksDispatched < 0) {
            throw new IllegalArgumentException("counts cannot be negative");
        }
        itemResults = List.copyOf(itemResults);
    }
}

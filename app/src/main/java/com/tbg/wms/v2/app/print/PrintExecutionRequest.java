package com.tbg.wms.v2.app.print;

import com.tbg.wms.v2.domain.print.PrintTask;

import java.util.List;

/**
 * Request to execute a prepared print plan.
 */
public record PrintExecutionRequest(List<PrintTask> tasks, boolean printToFile, String printerId) {
    public PrintExecutionRequest {
        if (tasks == null) {
            throw new IllegalArgumentException("tasks are required");
        }
        tasks = List.copyOf(tasks);
        printerId = printerId == null ? null : printerId.trim();
    }
}

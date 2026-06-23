package com.tbg.wms.v2.app.queue;

public record QueueExecutionRequest(PreparedQueue queue, boolean printToFile, String printerId) {
    public QueueExecutionRequest {
        if (queue == null) {
            throw new IllegalArgumentException("queue is required");
        }
        printerId = printerId == null ? null : printerId.trim();
    }
}

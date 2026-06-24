package com.tbg.wms.v2.app.queue;

/**
 * Carries queue execution request data across WMS 2.0 module boundaries.
 *
 * @param queue the queue.
 * @param printToFile the print to file.
 * @param printerId the printer id.
 */
public record QueueExecutionRequest(PreparedQueue queue, boolean printToFile, String printerId) {
    public QueueExecutionRequest {
        if (queue == null) {
            throw new IllegalArgumentException("queue is required");
        }
        printerId = printerId == null ? null : printerId.trim();
    }
}

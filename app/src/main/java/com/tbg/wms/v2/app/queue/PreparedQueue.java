package com.tbg.wms.v2.app.queue;

import java.util.List;

/**
 * Carries prepared queue data across WMS 2.0 module boundaries.
 *
 * @param items the items.
 */
public record PreparedQueue(List<PreparedQueueItem> items) {
    public PreparedQueue {
        if (items == null) {
            throw new IllegalArgumentException("items are required");
        }
        items = List.copyOf(items);
    }
}

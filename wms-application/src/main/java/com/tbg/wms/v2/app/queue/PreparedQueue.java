package com.tbg.wms.v2.app.queue;

import java.util.List;

public record PreparedQueue(List<PreparedQueueItem> items) {
    public PreparedQueue {
        if (items == null) {
            throw new IllegalArgumentException("items are required");
        }
        items = List.copyOf(items);
    }
}

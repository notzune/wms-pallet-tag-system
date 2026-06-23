package com.tbg.wms.v2.app.queue;

public record QueueRequestItem(QueueItemType type, String id) {
    public QueueRequestItem {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        id = id.trim();
    }
}

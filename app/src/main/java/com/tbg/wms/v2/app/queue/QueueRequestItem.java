package com.tbg.wms.v2.app.queue;

/**
 * Carries queue request item data across WMS 2.0 module boundaries.
 *
 * @param type the type.
 * @param id the id.
 */
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

package com.tbg.wms.v2.app.ports;

import java.util.List;
import java.util.Optional;

/**
 * Persists resumable print-job checkpoints.
 */
public interface CheckpointStore {
    void save(PrintCheckpoint checkpoint);

    Optional<PrintCheckpoint> findById(String checkpointId);

    List<PrintCheckpoint> findIncomplete();

    record PrintCheckpoint(String id, String sourceId, int nextTaskIndex, int totalTasks, String lastError) {
        public PrintCheckpoint {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id is required");
            }
            if (sourceId == null || sourceId.isBlank()) {
                throw new IllegalArgumentException("sourceId is required");
            }
            if (nextTaskIndex < 0) {
                throw new IllegalArgumentException("nextTaskIndex cannot be negative");
            }
            if (totalTasks < 0) {
                throw new IllegalArgumentException("totalTasks cannot be negative");
            }
            id = id.trim();
            sourceId = sourceId.trim();
        }
    }
}

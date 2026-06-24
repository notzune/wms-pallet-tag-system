package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.print.PrintTask;

import java.util.List;
import java.util.Optional;

/**
 * Persists resumable print-job checkpoints.
 */
public interface CheckpointStore {
    /**
     * Saves the supplied checkpoint.
     *
     * @param checkpoint the checkpoint.
     */
    void save(PrintCheckpoint checkpoint);

    /**
     * Finds a checkpoint by identifier.
     *
     * @param checkpointId the checkpoint id.
     * @return the matching value, when present.
     */
    Optional<PrintCheckpoint> findById(String checkpointId);

    /**
     * Lists incomplete checkpoints available for resume.
     *
     * @return the matching values.
     */
    List<PrintCheckpoint> findIncomplete();

    /**
     * Carries print checkpoint data across WMS 2.0 module boundaries.
     *
     * @param id the id.
     * @param sourceId the source id.
     * @param nextTaskIndex the next task index.
     * @param totalTasks the total tasks.
     * @param lastError the last error.
     * @param printToFile the print to file.
     * @param printerId the printer id.
     * @param tasks the tasks.
     */
    record PrintCheckpoint(
            String id,
            String sourceId,
            int nextTaskIndex,
            int totalTasks,
            String lastError,
            boolean printToFile,
            String printerId,
            List<PrintTask> tasks
    ) {
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
            if (!printToFile && (printerId == null || printerId.isBlank())) {
                throw new IllegalArgumentException("printerId is required for live print checkpoints");
            }
            if (tasks == null) {
                throw new IllegalArgumentException("tasks are required");
            }
            id = id.trim();
            sourceId = sourceId.trim();
            printerId = printerId == null ? null : printerId.trim();
            tasks = List.copyOf(tasks);
        }
    }
}

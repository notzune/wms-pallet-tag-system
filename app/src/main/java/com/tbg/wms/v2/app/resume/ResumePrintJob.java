package com.tbg.wms.v2.app.resume;

import com.tbg.wms.v2.app.ports.ArtifactStore;
import com.tbg.wms.v2.app.ports.CheckpointStore;
import com.tbg.wms.v2.app.ports.PrintDispatcher;
import com.tbg.wms.v2.app.ports.PrinterCatalog;
import com.tbg.wms.v2.app.print.ExecutePrintPlan;
import com.tbg.wms.v2.app.print.PrintExecutionRequest;
import com.tbg.wms.v2.app.print.PrintExecutionResult;
import com.tbg.wms.v2.domain.print.PrintTask;

import java.util.List;
import java.util.Objects;

/**
 * Resumes incomplete print jobs using safe mode: reprint the most recent completed task.
 */
public final class ResumePrintJob {
    private final CheckpointStore checkpointStore;
    private final ExecutePrintPlan printExecutor;

    /**
     * Creates a print-job resume service.
     *
     * @param checkpointStore persisted checkpoint store
     * @param artifactStore artifact writer used for file output
     * @param printDispatcher printer dispatch adapter
     * @param printerCatalog printer lookup service
     */
    public ResumePrintJob(
            CheckpointStore checkpointStore,
            ArtifactStore artifactStore,
            PrintDispatcher printDispatcher,
            PrinterCatalog printerCatalog
    ) {
        this.checkpointStore = Objects.requireNonNull(checkpointStore, "checkpointStore");
        this.printExecutor = new ExecutePrintPlan(artifactStore, printDispatcher, printerCatalog);
    }

    /**
     * Resumes an incomplete print job from the selected checkpoint.
     *
     * @param checkpointId checkpoint identifier to resume
     * @return resume result with completed output counts
     */
    public ResumePrintResult resume(String checkpointId) {
        if (checkpointId == null || checkpointId.isBlank()) {
            throw new IllegalArgumentException("Checkpoint ID is required.");
        }
        CheckpointStore.PrintCheckpoint checkpoint = checkpointStore.findById(checkpointId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found: " + checkpointId));

        int startIndex = checkpoint.nextTaskIndex() <= 0 ? 0 : checkpoint.nextTaskIndex() - 1;
        List<PrintTask> remaining = checkpoint.tasks().subList(startIndex, checkpoint.tasks().size());
        PrintExecutionResult result = printExecutor.execute(new PrintExecutionRequest(
                remaining,
                checkpoint.printToFile(),
                checkpoint.printerId()
        ));
        int nextTaskIndex = checkpoint.tasks().size();
        checkpointStore.save(new CheckpointStore.PrintCheckpoint(
                checkpoint.id(),
                checkpoint.sourceId(),
                nextTaskIndex,
                checkpoint.totalTasks(),
                null,
                checkpoint.printToFile(),
                checkpoint.printerId(),
                checkpoint.tasks()
        ));
        return new ResumePrintResult(result.artifactsWritten(), result.tasksDispatched(), nextTaskIndex);
    }
}

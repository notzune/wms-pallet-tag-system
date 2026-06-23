package com.tbg.wms.v2.files.checkpoints;

import com.tbg.wms.v2.app.ports.CheckpointStore;
import com.tbg.wms.v2.domain.print.PrintTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCheckpointStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndFindById_roundTripsCheckpointWithTasks() {
        FileCheckpointStore store = new FileCheckpointStore(tempDir);
        CheckpointStore.PrintCheckpoint checkpoint = checkpoint("job-1", 1, 3, "paper jam");

        store.save(checkpoint);

        CheckpointStore.PrintCheckpoint loaded = store.findById("job-1").orElseThrow();
        assertEquals("source-job-1", loaded.sourceId());
        assertEquals(1, loaded.nextTaskIndex());
        assertEquals("paper jam", loaded.lastError());
        assertEquals(false, loaded.printToFile());
        assertEquals("P1", loaded.printerId());
        assertEquals(List.of("first.zpl", "info.zpl"), loaded.tasks().stream()
                .map(PrintTask::artifactName)
                .toList());
    }

    @Test
    void findIncomplete_returnsOnlyIncompleteCheckpointsNewestFirst() throws Exception {
        FileCheckpointStore store = new FileCheckpointStore(tempDir);
        store.save(checkpoint("old", 1, 3, null));
        Thread.sleep(5);
        store.save(checkpoint("done", 3, 3, null));
        Thread.sleep(5);
        store.save(checkpoint("new", 2, 3, null));

        assertEquals(List.of("new", "old"), store.findIncomplete().stream()
                .map(CheckpointStore.PrintCheckpoint::id)
                .toList());
    }

    @Test
    void findById_returnsEmptyForMissingCheckpoint() {
        FileCheckpointStore store = new FileCheckpointStore(tempDir);

        assertTrue(store.findById("missing").isEmpty());
    }

    private static CheckpointStore.PrintCheckpoint checkpoint(
            String id,
            int nextTaskIndex,
            int totalTasks,
            String lastError
    ) {
        return new CheckpointStore.PrintCheckpoint(
                id,
                "source-" + id,
                nextTaskIndex,
                totalTasks,
                lastError,
                false,
                "P1",
                List.of(
                        new PrintTask(PrintTask.Kind.PALLET_LABEL, "first.zpl", "SHIP:LPN"),
                        new PrintTask(PrintTask.Kind.SHIPMENT_INFO_TAG, "info.zpl", "INFO")
                )
        );
    }
}

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrintCheckpointSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void listIncompleteJobs_shouldReturnNewestIncompleteJobsFirst() throws Exception {
        JobCheckpointStore store = new JobCheckpointStore(tempDir.resolve("checkpoints"));
        PrintCheckpointSupport support = new PrintCheckpointSupport(store, new LabelWorkflowService(new AppConfig()), 10, 100);

        AdvancedPrintWorkflowService.JobCheckpoint older = checkpoint("older", tempDir.resolve("out-older"), false, 1, LocalDateTime.now().minusMinutes(5));
        AdvancedPrintWorkflowService.JobCheckpoint newer = checkpoint("newer", tempDir.resolve("out-newer"), false, 2, LocalDateTime.now());
        AdvancedPrintWorkflowService.JobCheckpoint completed = checkpoint("completed", tempDir.resolve("out-done"), true, 2, LocalDateTime.now().plusMinutes(5));

        store.write(older);
        store.write(newer);
        store.write(completed);

        List<AdvancedPrintWorkflowService.ResumeCandidate> candidates = support.listIncompleteJobs();

        assertEquals(List.of("newer", "older"), candidates.stream()
                .map(AdvancedPrintWorkflowService.ResumeCandidate::checkpointId)
                .toList());
    }

    @Test
    void resumeJob_shouldReplayLastSuccessfulTaskAndCompleteCheckpoint() throws Exception {
        JobCheckpointStore store = new JobCheckpointStore(tempDir.resolve("checkpoints"));
        PrintCheckpointSupport support = new PrintCheckpointSupport(store, new LabelWorkflowService(new AppConfig()), 10, 100);
        Path outDir = tempDir.resolve("out");

        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = checkpoint("resume-me", outDir, false, 1, LocalDateTime.now());
        checkpoint.tasks = List.of(
                new AdvancedPrintWorkflowService.PrintTask(AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL, "label-1.zpl", "^XA^FD1^FS^XZ", "P1"),
                new AdvancedPrintWorkflowService.PrintTask(AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG, "info-1.zpl", "^XA^FD2^FS^XZ", "P2")
        );
        store.write(checkpoint);

        AdvancedPrintWorkflowService.JobCheckpoint resumed = support.resumeJob("resume-me");

        assertTrue(Files.exists(outDir.resolve("label-1.zpl")));
        assertTrue(Files.exists(outDir.resolve("info-1.zpl")));
        assertEquals("^XA^FD1^FS^XZ", Files.readString(outDir.resolve("label-1.zpl")));
        assertEquals("^XA^FD2^FS^XZ", Files.readString(outDir.resolve("info-1.zpl")));
        assertEquals(2, resumed.nextTaskIndex);
        assertTrue(resumed.completed);
        assertNull(resumed.lastError);

        AdvancedPrintWorkflowService.JobCheckpoint persisted = store.read("resume-me");
        assertTrue(persisted.completed);
        assertEquals(2, persisted.nextTaskIndex);
        assertFalse(support.listIncompleteJobs().stream()
                .anyMatch(candidate -> candidate.checkpointId().equals("resume-me")));
    }

    private static AdvancedPrintWorkflowService.JobCheckpoint checkpoint(
            String id,
            Path outputDir,
            boolean completed,
            int nextTaskIndex,
            LocalDateTime updatedAt
    ) {
        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = new AdvancedPrintWorkflowService.JobCheckpoint();
        checkpoint.id = id;
        checkpoint.mode = AdvancedPrintWorkflowService.InputMode.SHIPMENT;
        checkpoint.sourceId = "SRC-" + id;
        checkpoint.outputDirectory = outputDir.toString();
        checkpoint.printToFile = true;
        checkpoint.printerId = "FILE";
        checkpoint.printerEndpoint = "FILE";
        checkpoint.createdAt = updatedAt.minusMinutes(1);
        checkpoint.updatedAt = updatedAt;
        checkpoint.completed = completed;
        checkpoint.nextTaskIndex = nextTaskIndex;
        checkpoint.tasks = List.of(
                new AdvancedPrintWorkflowService.PrintTask(AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL, "label.zpl", "^XA^FS^XZ", "PX")
        );
        return checkpoint;
    }
}

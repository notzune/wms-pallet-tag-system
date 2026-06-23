package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdvancedPrintResumeSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void listIncompleteJobsAndResumeJob_shouldDelegateAndMapResult() throws Exception {
        JobCheckpointStore store = new JobCheckpointStore(tempDir.resolve("checkpoints"));
        PrintCheckpointSupport checkpointSupport =
                new PrintCheckpointSupport(store, new LabelWorkflowService(new AppConfig()), 10, 100);
        AdvancedPrintResumeSupport support =
                new AdvancedPrintResumeSupport(checkpointSupport, new AdvancedPrintResultSupport());
        Path outDir = tempDir.resolve("out");
        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = checkpoint("resume-me", outDir);
        store.write(checkpoint);

        List<AdvancedPrintWorkflowService.ResumeCandidate> candidates = support.listIncompleteJobs();
        AdvancedPrintWorkflowService.PrintResult result = support.resumeJob("resume-me");

        assertEquals(List.of("resume-me"), candidates.stream()
                .map(AdvancedPrintWorkflowService.ResumeCandidate::checkpointId)
                .toList());
        assertEquals(1, result.getLabelsPrinted());
        assertEquals(1, result.getInfoTagsPrinted());
        assertEquals(outDir, result.getOutputDirectory());
        assertEquals("FILE", result.getPrinterId());
    }

    private static AdvancedPrintWorkflowService.JobCheckpoint checkpoint(String id, Path outputDir) {
        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = new AdvancedPrintWorkflowService.JobCheckpoint();
        checkpoint.id = id;
        checkpoint.mode = AdvancedPrintWorkflowService.InputMode.SHIPMENT;
        checkpoint.sourceId = "SHIP1";
        checkpoint.outputDirectory = outputDir.toString();
        checkpoint.printToFile = true;
        checkpoint.printerId = "FILE";
        checkpoint.printerEndpoint = "FILE";
        checkpoint.createdAt = LocalDateTime.now().minusMinutes(1);
        checkpoint.updatedAt = LocalDateTime.now();
        checkpoint.completed = false;
        checkpoint.nextTaskIndex = 1;
        checkpoint.tasks = List.of(
                new AdvancedPrintWorkflowService.PrintTask(
                        AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL,
                        "label.zpl",
                        "^XA^FD1^FS^XZ",
                        "LPN-1"
                ),
                new AdvancedPrintWorkflowService.PrintTask(
                        AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG,
                        "info.zpl",
                        "^XA^FD2^FS^XZ",
                        "SHIP1"
                )
        );
        return checkpoint;
    }
}

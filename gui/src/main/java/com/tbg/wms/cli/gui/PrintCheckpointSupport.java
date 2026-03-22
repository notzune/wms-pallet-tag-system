/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Manages checkpoint persistence and deterministic task execution for GUI print jobs.
 */
final class PrintCheckpointSupport {

    private final JobCheckpointStore checkpointStore;
    private final LabelWorkflowService shipmentService;
    private final int maxTasksPerJob;
    private final int maxCheckpointFilesScanned;

    PrintCheckpointSupport(
            JobCheckpointStore checkpointStore,
            LabelWorkflowService shipmentService,
            int maxTasksPerJob,
            int maxCheckpointFilesScanned
    ) {
        this.checkpointStore = Objects.requireNonNull(checkpointStore, "checkpointStore cannot be null");
        this.shipmentService = Objects.requireNonNull(shipmentService, "shipmentService cannot be null");
        this.maxTasksPerJob = maxTasksPerJob;
        this.maxCheckpointFilesScanned = maxCheckpointFilesScanned;
    }

    AdvancedPrintWorkflowService.JobCheckpoint createCheckpoint(
            String id,
            AdvancedPrintWorkflowService.InputMode mode,
            String sourceId,
            Path outputDir,
            boolean printToFile,
            PrinterConfig printer,
            List<AdvancedPrintWorkflowService.PrintTask> tasks
    ) throws Exception {
        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = new AdvancedPrintWorkflowService.JobCheckpoint();
        checkpoint.id = id;
        checkpoint.mode = mode;
        checkpoint.sourceId = sourceId;
        checkpoint.outputDirectory = outputDir.toAbsolutePath().toString();
        checkpoint.printToFile = printToFile;
        checkpoint.printerId = printToFile ? "FILE" : printer.getId();
        checkpoint.printerEndpoint = printToFile ? "FILE" : printer.getEndpoint();
        checkpoint.createdAt = LocalDateTime.now();
        checkpoint.updatedAt = checkpoint.createdAt;
        checkpoint.nextTaskIndex = 0;
        checkpoint.completed = false;
        checkpoint.tasks = tasks;
        writeCheckpoint(checkpoint);
        return checkpoint;
    }

    List<AdvancedPrintWorkflowService.ResumeCandidate> listIncompleteJobs() throws Exception {
        List<AdvancedPrintWorkflowService.ResumeCandidate> items = new ArrayList<>();
        for (Path file : checkpointStore.listCheckpointFiles(maxCheckpointFilesScanned)) {
            try {
                AdvancedPrintWorkflowService.JobCheckpoint checkpoint =
                        checkpointStore.read(stripJsonExtension(file.getFileName().toString()));
                if (checkpoint != null && !checkpoint.completed) {
                    int total = checkpoint.tasks == null ? 0 : checkpoint.tasks.size();
                    items.add(new AdvancedPrintWorkflowService.ResumeCandidate(
                            checkpoint.id,
                            checkpoint.mode,
                            checkpoint.sourceId,
                            checkpoint.outputDirectory,
                            checkpoint.nextTaskIndex,
                            total,
                            checkpoint.updatedAt,
                            checkpoint.lastError
                    ));
                }
            } catch (Exception ignored) {
                // Skip malformed checkpoint files and continue scanning.
            }
        }
        items.sort(Comparator.comparing(
                AdvancedPrintWorkflowService.ResumeCandidate::updatedAt,
                Comparator.nullsLast(LocalDateTime::compareTo)
        ).reversed());
        return items;
    }

    AdvancedPrintWorkflowService.JobCheckpoint resumeJob(String checkpointId) throws Exception {
        if (checkpointId == null || checkpointId.isBlank()) {
            throw new IllegalArgumentException("Checkpoint ID is required.");
        }
        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = readCheckpoint(checkpointId.trim());
        if (checkpoint == null) {
            throw new IllegalArgumentException("Checkpoint not found: " + checkpointId);
        }
        if (checkpoint.completed) {
            throw new IllegalStateException("Checkpoint is already completed.");
        }
        PrinterConfig printer = checkpoint.printToFile ? null : shipmentService.resolvePrinter(checkpoint.printerId);
        int resumeIndex = checkpoint.nextTaskIndex <= 0 ? 0 : checkpoint.nextTaskIndex - 1;
        executeTasks(checkpoint, printer, resumeIndex);
        return checkpoint;
    }

    void executeTasks(AdvancedPrintWorkflowService.JobCheckpoint checkpoint, PrinterConfig printer, int startIndex) throws Exception {
        Objects.requireNonNull(checkpoint, "checkpoint cannot be null");
        if (checkpoint.tasks == null) {
            throw new IllegalArgumentException("Checkpoint tasks cannot be null.");
        }
        if (checkpoint.tasks.size() > maxTasksPerJob) {
            throw new IllegalArgumentException("Task count exceeds max limit: " + maxTasksPerJob);
        }
        Path outDir = Paths.get(checkpoint.outputDirectory);
        Files.createDirectories(outDir);
        NetworkPrintService printService = new NetworkPrintService();
        int start = Math.max(0, Math.min(startIndex, checkpoint.tasks.size()));
        for (int i = start; i < checkpoint.tasks.size(); i++) {
            AdvancedPrintWorkflowService.PrintTask task = checkpoint.tasks.get(i);
            Path outFile = outDir.resolve(task.fileName);
            try {
                Files.writeString(outFile, task.zpl);
                if (!checkpoint.printToFile) {
                    if (printer == null) {
                        throw new IllegalStateException("Printer is required.");
                    }
                    printService.print(printer, task.zpl, task.payloadId);
                }
                checkpoint.nextTaskIndex = i + 1;
                checkpoint.updatedAt = LocalDateTime.now();
                checkpoint.lastError = null;
                writeCheckpoint(checkpoint);
            } catch (Exception ex) {
                checkpoint.completed = false;
                checkpoint.updatedAt = LocalDateTime.now();
                checkpoint.lastError = ex.getMessage();
                writeCheckpoint(checkpoint);
                throw ex;
            }
        }
        checkpoint.completed = true;
        checkpoint.updatedAt = LocalDateTime.now();
        checkpoint.lastError = null;
        writeCheckpoint(checkpoint);
    }

    AdvancedPrintWorkflowService.JobCheckpoint readCheckpoint(String id) throws Exception {
        return checkpointStore.read(id);
    }

    void writeCheckpoint(AdvancedPrintWorkflowService.JobCheckpoint checkpoint) throws Exception {
        checkpointStore.write(checkpoint);
    }

    private String stripJsonExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        return fileName.toLowerCase(Locale.ROOT).endsWith(".json")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;
    }
}

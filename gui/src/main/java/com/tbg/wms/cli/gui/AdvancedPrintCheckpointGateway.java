/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.print.PrinterConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class AdvancedPrintCheckpointGateway implements AdvancedPrintExecutionSupport.CheckpointGateway {
    private final PrintCheckpointSupport checkpointSupport;

    AdvancedPrintCheckpointGateway(PrintCheckpointSupport checkpointSupport) {
        this.checkpointSupport = Objects.requireNonNull(checkpointSupport, "checkpointSupport cannot be null");
    }

    @Override
    public AdvancedPrintWorkflowService.JobCheckpoint createCheckpoint(
            String id,
            AdvancedPrintWorkflowService.InputMode mode,
            String sourceId,
            Path outputDir,
            boolean printToFile,
            PrinterConfig printer,
            List<AdvancedPrintWorkflowService.PrintTask> tasks
    ) throws Exception {
        return checkpointSupport.createCheckpoint(id, mode, sourceId, outputDir, printToFile, printer, tasks);
    }

    @Override
    public void executeTasks(
            AdvancedPrintWorkflowService.JobCheckpoint checkpoint,
            PrinterConfig printer,
            int startIndex
    ) throws Exception {
        checkpointSupport.executeTasks(checkpoint, printer, startIndex);
    }
}

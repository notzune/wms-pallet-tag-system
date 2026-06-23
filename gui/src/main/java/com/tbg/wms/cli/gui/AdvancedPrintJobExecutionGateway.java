/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.print.PrinterRoutingService;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class AdvancedPrintJobExecutionGateway implements AdvancedPrintJobPrintSupport.ExecutionGateway {
    private final AdvancedPrintExecutionSupport executionSupport;

    AdvancedPrintJobExecutionGateway(AdvancedPrintExecutionSupport executionSupport) {
        this.executionSupport = Objects.requireNonNull(executionSupport, "executionSupport cannot be null");
    }

    @Override
    public AdvancedPrintWorkflowService.PrintResult executeShipmentJob(
            LabelWorkflowService.PreparedJob job,
            String printerId,
            Path outputDir,
            boolean printToFile,
            List<AdvancedPrintWorkflowService.PrintTask> tasks
    ) throws Exception {
        return executionSupport.executeShipmentJob(job, printerId, outputDir, printToFile, tasks);
    }

    @Override
    public AdvancedPrintWorkflowService.PrintResult executeCarrierMoveJob(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            PrinterRoutingService routing,
            String printerId,
            Path outputDir,
            boolean printToFile,
            List<AdvancedPrintWorkflowService.PrintTask> tasks
    ) throws Exception {
        return executionSupport.executeCarrierMoveJob(job, routing, printerId, outputDir, printToFile, tasks);
    }
}

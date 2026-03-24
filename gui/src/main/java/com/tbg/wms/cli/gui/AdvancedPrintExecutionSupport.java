/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Shared checkpoint execution helpers for advanced print jobs.
 */
final class AdvancedPrintExecutionSupport {

    private final CheckpointGateway checkpointGateway;
    private final AdvancedPrintResultSupport resultSupport;
    private final DateTimeFormatter timestampFormatter;

    AdvancedPrintExecutionSupport(CheckpointGateway checkpointGateway, AdvancedPrintResultSupport resultSupport) {
        this(checkpointGateway, resultSupport, DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    AdvancedPrintExecutionSupport(
            CheckpointGateway checkpointGateway,
            AdvancedPrintResultSupport resultSupport,
            DateTimeFormatter timestampFormatter
    ) {
        this.checkpointGateway = Objects.requireNonNull(checkpointGateway, "checkpointGateway cannot be null");
        this.resultSupport = Objects.requireNonNull(resultSupport, "resultSupport cannot be null");
        this.timestampFormatter = Objects.requireNonNull(timestampFormatter, "timestampFormatter cannot be null");
    }

    AdvancedPrintWorkflowService.PrintResult executeShipmentJob(
            LabelWorkflowService.PreparedJob job,
            String printerId,
            Path outputDir,
            boolean printToFile,
            List<AdvancedPrintWorkflowService.PrintTask> tasks
    ) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        return execute(
                "shipment",
                job.getShipmentId(),
                AdvancedPrintWorkflowService.InputMode.SHIPMENT,
                job.getRouting(),
                printerId,
                outputDir == null ? defaultOutputDir("gui-" + job.getShipmentId()) : outputDir,
                printToFile,
                tasks
        );
    }

    AdvancedPrintWorkflowService.PrintResult executeCarrierMoveJob(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            PrinterRoutingService routing,
            String printerId,
            Path outputDir,
            boolean printToFile,
            List<AdvancedPrintWorkflowService.PrintTask> tasks
    ) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        return execute(
                "carrier",
                job.getCarrierMoveId(),
                AdvancedPrintWorkflowService.InputMode.CARRIER_MOVE,
                routing,
                printerId,
                outputDir == null ? defaultOutputDir("gui-cmid-" + job.getCarrierMoveId()) : outputDir,
                printToFile,
                tasks
        );
    }

    private AdvancedPrintWorkflowService.PrintResult execute(
            String checkpointPrefix,
            String sourceId,
            AdvancedPrintWorkflowService.InputMode inputMode,
            PrinterRoutingService routing,
            String printerId,
            Path outputDir,
            boolean printToFile,
            List<AdvancedPrintWorkflowService.PrintTask> tasks
    ) throws Exception {
        PrinterConfig printer = resultSupport.resolvePrinterForPrint(routing, printerId, printToFile);
        String timestamp = timestampFormatter.format(LocalDateTime.now());
        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = checkpointGateway.createCheckpoint(
                checkpointPrefix + "-" + sourceId + "-" + timestamp,
                inputMode,
                sourceId,
                outputDir,
                printToFile,
                printer,
                tasks
        );
        checkpointGateway.executeTasks(checkpoint, printer, 0);
        return resultSupport.toResult(checkpoint);
    }

    private Path defaultOutputDir(String prefix) {
        return Paths.get("out", prefix + "-" + timestampFormatter.format(LocalDateTime.now()));
    }

    interface CheckpointGateway {
        AdvancedPrintWorkflowService.JobCheckpoint createCheckpoint(
                String id,
                AdvancedPrintWorkflowService.InputMode mode,
                String sourceId,
                Path outputDir,
                boolean printToFile,
                PrinterConfig printer,
                List<AdvancedPrintWorkflowService.PrintTask> tasks
        ) throws Exception;

        void executeTasks(
                AdvancedPrintWorkflowService.JobCheckpoint checkpoint,
                PrinterConfig printer,
                int startIndex
        ) throws Exception;
    }
}

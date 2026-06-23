package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.PrinterRoutingService;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Builds print tasks for prepared jobs and dispatches them to checkpoint execution.
 */
final class AdvancedPrintJobPrintSupport {
    private final ExecutionGateway executionGateway;

    AdvancedPrintJobPrintSupport(ExecutionGateway executionGateway) {
        this.executionGateway = Objects.requireNonNull(executionGateway, "executionGateway cannot be null");
    }

    AdvancedPrintWorkflowService.PrintResult printShipmentJob(
            LabelWorkflowService.PreparedJob job,
            List<Lpn> selectedLpns,
            String printerId,
            Path outputDir,
            boolean printToFile,
            boolean includeInfoTags
    ) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        List<Lpn> lpnsToPrint = PrintTaskPlanner.filterLpnsForPrint(job.getLpnsForLabels(), selectedLpns);
        PrintTaskPlanner.ShipmentPrintBatch shipmentBatch =
                PrintTaskPlanner.ShipmentPrintBatch.forShipment(job, lpnsToPrint, includeInfoTags);
        List<AdvancedPrintWorkflowService.PrintTask> tasks = PrintTaskPlanner.buildShipmentTasks(shipmentBatch);
        return executionGateway.executeShipmentJob(job, printerId, outputDir, printToFile, tasks);
    }

    AdvancedPrintWorkflowService.PrintResult printCarrierMoveJob(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            List<LabelSelectionRef> selectedLabels,
            String printerId,
            Path outputDir,
            boolean printToFile,
            boolean includeInfoTags
    ) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        LabelWorkflowService.PreparedJob firstShipment = job.firstShipmentJob();
        List<AdvancedPrintWorkflowService.PrintTask> tasks =
                PrintTaskPlanner.buildCarrierMoveTasks(job, selectedLabels, includeInfoTags);
        return executionGateway.executeCarrierMoveJob(
                job,
                firstShipment.getRouting(),
                printerId,
                outputDir,
                printToFile,
                tasks
        );
    }

    interface ExecutionGateway {
        AdvancedPrintWorkflowService.PrintResult executeShipmentJob(
                LabelWorkflowService.PreparedJob job,
                String printerId,
                Path outputDir,
                boolean printToFile,
                List<AdvancedPrintWorkflowService.PrintTask> tasks
        ) throws Exception;

        AdvancedPrintWorkflowService.PrintResult executeCarrierMoveJob(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
                PrinterRoutingService routing,
                String printerId,
                Path outputDir,
                boolean printToFile,
                List<AdvancedPrintWorkflowService.PrintTask> tasks
        ) throws Exception;
    }
}

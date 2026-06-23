package com.tbg.wms.v2.app.queue;

import com.tbg.wms.v2.app.labels.BuildCarrierMovePrintPlan;
import com.tbg.wms.v2.app.labels.BuildShipmentPrintPlan;
import com.tbg.wms.v2.app.ports.ArtifactStore;
import com.tbg.wms.v2.app.ports.PrintDispatcher;
import com.tbg.wms.v2.app.ports.PrinterCatalog;
import com.tbg.wms.v2.app.print.ExecutePrintPlan;
import com.tbg.wms.v2.app.print.PrintExecutionRequest;
import com.tbg.wms.v2.app.print.PrintExecutionResult;
import com.tbg.wms.v2.domain.print.PrintTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Executes a prepared queue one item at a time and aggregates execution totals.
 */
public final class ExecuteQueue {
    private final BuildShipmentPrintPlan shipmentPlanner;
    private final BuildCarrierMovePrintPlan carrierMovePlanner;
    private final ExecutePrintPlan printExecutor;

    public ExecuteQueue(
            BuildShipmentPrintPlan shipmentPlanner,
            BuildCarrierMovePrintPlan carrierMovePlanner,
            ArtifactStore artifactStore,
            PrintDispatcher printDispatcher,
            PrinterCatalog printerCatalog
    ) {
        this.shipmentPlanner = Objects.requireNonNull(shipmentPlanner, "shipmentPlanner");
        this.carrierMovePlanner = Objects.requireNonNull(carrierMovePlanner, "carrierMovePlanner");
        this.printExecutor = new ExecutePrintPlan(artifactStore, printDispatcher, printerCatalog);
    }

    public QueueExecutionResult execute(QueueExecutionRequest request) {
        Objects.requireNonNull(request, "request");
        List<PrintExecutionResult> results = new ArrayList<>();
        int artifacts = 0;
        int dispatched = 0;

        for (PreparedQueueItem item : request.queue().items()) {
            List<PrintTask> tasks = item.type() == QueueItemType.SHIPMENT
                    ? shipmentPlanner.build(item.shipment(), List.of()).tasks()
                    : carrierMovePlanner.build(item.carrierMove()).tasks();
            PrintExecutionResult result = printExecutor.execute(new PrintExecutionRequest(
                    tasks,
                    request.printToFile(),
                    request.printerId()
            ));
            results.add(result);
            artifacts += result.artifactsWritten();
            dispatched += result.tasksDispatched();
        }

        return new QueueExecutionResult(results, artifacts, dispatched);
    }
}

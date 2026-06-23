package com.tbg.wms.v2.app.queue;

import com.tbg.wms.v2.app.labels.BuildCarrierMovePrintPlan;
import com.tbg.wms.v2.app.labels.BuildShipmentPrintPlan;
import com.tbg.wms.v2.app.ports.ArtifactStore;
import com.tbg.wms.v2.app.ports.PrintDispatcher;
import com.tbg.wms.v2.app.ports.PrinterCatalog;
import com.tbg.wms.v2.app.ports.PrinterRef;
import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.carriermove.PreparedStopGroup;
import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import com.tbg.wms.v2.domain.print.PrintTask;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecuteQueueTest {
    @Test
    void executeQueue_runsPreparedItemsAndAggregatesResults() {
        List<String> events = new ArrayList<>();
        ExecuteQueue executeQueue = new ExecuteQueue(
                new BuildShipmentPrintPlan(),
                new BuildCarrierMovePrintPlan(),
                artifactStore(events),
                dispatcher(events),
                catalog()
        );

        QueueExecutionResult result = executeQueue.execute(new QueueExecutionRequest(
                queue(),
                false,
                "P1"
        ));

        assertEquals(6, result.artifactsWritten());
        assertEquals(6, result.tasksDispatched());
        assertEquals(2, result.itemResults().size());
        assertEquals(List.of(
                "write:8001_LPN-1_1_of_1.zpl",
                "write:info-shipment-8001.zpl",
                "dispatch:8001_LPN-1_1_of_1.zpl:P1",
                "dispatch:info-shipment-8001.zpl:P1",
                "write:8002_LPN-2_1_of_1.zpl",
                "write:info-shipment-8002.zpl",
                "write:info-stop-01-of-01.zpl",
                "write:info-final-cmid-77.zpl",
                "dispatch:8002_LPN-2_1_of_1.zpl:P1",
                "dispatch:info-shipment-8002.zpl:P1",
                "dispatch:info-stop-01-of-01.zpl:P1",
                "dispatch:info-final-cmid-77.zpl:P1"
        ), events);
    }

    private static PreparedQueue queue() {
        PreparedShipmentLabels shipment = new PreparedShipmentLabels(
                "8001",
                List.of(new LabelSelectionRef("LPN-1", 1)),
                true
        );
        CarrierMoveLabels carrierMove = new CarrierMoveLabels(
                "77",
                List.of(PreparedStopGroup.of(1, 10, List.of(new PreparedShipmentLabels(
                        "8002",
                        List.of(new LabelSelectionRef("LPN-2", 1)),
                        true
                )))),
                true
        );
        return new PreparedQueue(List.of(
                PreparedQueueItem.forShipment("8001", shipment),
                PreparedQueueItem.forCarrierMove("77", carrierMove)
        ));
    }

    private static ArtifactStore artifactStore(List<String> events) {
        return task -> {
            events.add("write:" + task.artifactName());
            return Path.of("out", task.artifactName());
        };
    }

    private static PrintDispatcher dispatcher(List<String> events) {
        return (printer, task) -> events.add("dispatch:" + task.artifactName() + ":" + printer.id());
    }

    private static PrinterCatalog catalog() {
        PrinterRef printer = new PrinterRef("P1", "Printer 1", List.of("zpl"));
        return new PrinterCatalog() {
            @Override
            public List<PrinterRef> listEnabled() {
                return List.of(printer);
            }

            @Override
            public Optional<PrinterRef> findEnabledById(String printerId) {
                return printer.id().equals(printerId) ? Optional.of(printer) : Optional.empty();
            }
        };
    }
}

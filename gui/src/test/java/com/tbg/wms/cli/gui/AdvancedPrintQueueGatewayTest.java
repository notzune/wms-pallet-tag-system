package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AdvancedPrintQueueGatewayTest {

    @Test
    void prepareAndPrintMethods_shouldDelegateToConfiguredOperations() throws Exception {
        LabelWorkflowService.PreparedJob shipmentJob = PreviewSelectionTestData.shipmentJob(
                "SHIP1",
                List.of(new Lpn("LPN-1", "SHIP1", null, 0, 0, 0.0, null, null, null, null, null, List.of()))
        );
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob =
                PreviewSelectionTestData.carrierMoveJob(
                        "CM1",
                        List.of(PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentJob)))
                );
        AdvancedPrintWorkflowService.PrintResult shipmentResult =
                new AdvancedPrintWorkflowService.PrintResult(1, 1, Path.of("shipment-out"), "P1", "10.0.0.1", false);
        AdvancedPrintWorkflowService.PrintResult carrierResult =
                new AdvancedPrintWorkflowService.PrintResult(2, 2, Path.of("carrier-out"), "P2", "10.0.0.2", true);
        RecordingOperations operations = new RecordingOperations(shipmentJob, carrierMoveJob, shipmentResult, carrierResult);
        AdvancedPrintQueueGateway gateway = new AdvancedPrintQueueGateway(
                operations::prepareShipment,
                operations::prepareCarrierMove,
                operations::printShipment,
                operations::printCarrierMove
        );

        assertSame(shipmentJob, gateway.prepareShipment("SHIP1"));
        assertSame(carrierMoveJob, gateway.prepareCarrierMove("CM1"));
        assertSame(shipmentResult, gateway.printShipment(shipmentJob, "P1", Path.of("out"), false));
        assertSame(carrierResult, gateway.printCarrierMove(carrierMoveJob, "P2", null, true));

        assertEquals(List.of("SHIP1"), operations.preparedShipments);
        assertEquals(List.of("CM1"), operations.preparedCarrierMoves);
        assertEquals(List.of("SHIP1:P1:false"), operations.printedShipments);
        assertEquals(List.of("CM1:P2:true"), operations.printedCarrierMoves);
    }

    private static final class RecordingOperations {
        private final LabelWorkflowService.PreparedJob shipmentJob;
        private final AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob;
        private final AdvancedPrintWorkflowService.PrintResult shipmentResult;
        private final AdvancedPrintWorkflowService.PrintResult carrierResult;
        private final java.util.ArrayList<String> preparedShipments = new java.util.ArrayList<>();
        private final java.util.ArrayList<String> preparedCarrierMoves = new java.util.ArrayList<>();
        private final java.util.ArrayList<String> printedShipments = new java.util.ArrayList<>();
        private final java.util.ArrayList<String> printedCarrierMoves = new java.util.ArrayList<>();

        private RecordingOperations(
                LabelWorkflowService.PreparedJob shipmentJob,
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob,
                AdvancedPrintWorkflowService.PrintResult shipmentResult,
                AdvancedPrintWorkflowService.PrintResult carrierResult
        ) {
            this.shipmentJob = shipmentJob;
            this.carrierMoveJob = carrierMoveJob;
            this.shipmentResult = shipmentResult;
            this.carrierResult = carrierResult;
        }

        private LabelWorkflowService.PreparedJob prepareShipment(String shipmentId) {
            preparedShipments.add(shipmentId);
            return shipmentJob;
        }

        private AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMove(String carrierMoveId) {
            preparedCarrierMoves.add(carrierMoveId);
            return carrierMoveJob;
        }

        private AdvancedPrintWorkflowService.PrintResult printShipment(
                LabelWorkflowService.PreparedJob shipmentJob,
                String printerId,
                Path outputDir,
                boolean printToFile
        ) {
            printedShipments.add(shipmentJob.getShipmentId() + ":" + printerId + ":" + printToFile);
            return shipmentResult;
        }

        private AdvancedPrintWorkflowService.PrintResult printCarrierMove(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob,
                String printerId,
                Path outputDir,
                boolean printToFile
        ) {
            printedCarrierMoves.add(carrierMoveJob.getCarrierMoveId() + ":" + printerId + ":" + printToFile);
            return carrierResult;
        }
    }
}

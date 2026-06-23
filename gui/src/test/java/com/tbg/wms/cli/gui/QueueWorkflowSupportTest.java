/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueueWorkflowSupportTest {
    private final RecordingPreparationGateway preparationGateway = new RecordingPreparationGateway();
    private final RecordingPrintGateway printGateway = new RecordingPrintGateway();
    private final QueueWorkflowSupport support = new QueueWorkflowSupport(preparationGateway, printGateway);

    @Test
    void normalizeRequests_shouldDropBlankEntriesAndRejectEmptyQueues() {
        List<AdvancedPrintWorkflowService.QueueRequestItem> normalized = support.normalizeRequests(
                Arrays.asList(
                        null,
                        new AdvancedPrintWorkflowService.QueueRequestItem(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, " "),
                        new AdvancedPrintWorkflowService.QueueRequestItem(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, "SHIP123")
                ),
                5
        );

        assertEquals(1, normalized.size());
        assertEquals("SHIP123", normalized.get(0).getId());
        assertThrows(IllegalArgumentException.class, () -> support.normalizeRequests(List.of(), 5));
    }

    @Test
    void summarizeResults_shouldAggregateLabelsAndInfoTags() {
        AdvancedPrintWorkflowService.QueuePrintResult result = support.summarizeResults(List.of(
                new AdvancedPrintWorkflowService.PrintResult(2, 1, Path.of("out", "a"), "P1", "10.0.0.1", false),
                new AdvancedPrintWorkflowService.PrintResult(3, 2, Path.of("out", "b"), "P1", "10.0.0.1", false)
        ));

        assertEquals(5, result.getTotalLabelsPrinted());
        assertEquals(3, result.getTotalInfoTagsPrinted());
        assertEquals(2, result.getItemResults().size());
    }

    @Test
    void prepareQueue_shouldResolveShipmentAndCarrierItemsInOrder() throws Exception {
        AdvancedPrintWorkflowService.PreparedQueueJob queue = support.prepareQueue(
                List.of(
                        new AdvancedPrintWorkflowService.QueueRequestItem(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, "SHIP1"),
                        new AdvancedPrintWorkflowService.QueueRequestItem(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, "CM1")
                ),
                5
        );

        assertEquals(List.of("SHIP1"), preparationGateway.shipmentIds);
        assertEquals(List.of("CM1"), preparationGateway.carrierMoveIds);
        assertEquals(2, queue.getItems().size());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, queue.getItems().get(0).getType());
        assertSame(preparationGateway.shipmentJob, queue.getItems().get(0).getShipmentJob());
        assertEquals(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, queue.getItems().get(1).getType());
        assertSame(preparationGateway.carrierMoveJob, queue.getItems().get(1).getCarrierMoveJob());
    }

    @Test
    void printQueue_shouldDispatchItemsAndAggregateResults() throws Exception {
        AdvancedPrintWorkflowService.PreparedQueueJob queue = new AdvancedPrintWorkflowService.PreparedQueueJob(List.of(
                AdvancedPrintWorkflowService.PreparedQueueItem.forShipment("SHIP1", printGateway.shipmentJob),
                AdvancedPrintWorkflowService.PreparedQueueItem.forCarrier("CM1", printGateway.carrierMoveJob)
        ));

        AdvancedPrintWorkflowService.QueuePrintResult result = support.printQueue(queue, "P1", true);

        assertEquals(List.of("SHIP1"), printGateway.shipmentIds);
        assertEquals(List.of("CM1"), printGateway.carrierMoveIds);
        assertEquals(2, result.getItemResults().size());
        assertEquals(5, result.getTotalLabelsPrinted());
        assertEquals(3, result.getTotalInfoTagsPrinted());
    }

    private static final class RecordingPreparationGateway implements QueueWorkflowSupport.PreparationGateway {
        private final LabelWorkflowService.PreparedJob shipmentJob = PreviewSelectionTestData.shipmentJob(
                "SHIP1",
                List.of(new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of()))
        );
        private final AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob =
                PreviewSelectionTestData.carrierMoveJob(
                        "CM1",
                        List.of(PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentJob)))
                );
        private final java.util.ArrayList<String> shipmentIds = new java.util.ArrayList<>();
        private final java.util.ArrayList<String> carrierMoveIds = new java.util.ArrayList<>();

        @Override
        public LabelWorkflowService.PreparedJob prepareShipment(String shipmentId) {
            shipmentIds.add(shipmentId);
            return shipmentJob;
        }

        @Override
        public AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMove(String carrierMoveId) {
            carrierMoveIds.add(carrierMoveId);
            return carrierMoveJob;
        }
    }

    private static final class RecordingPrintGateway implements QueueWorkflowSupport.PrintGateway {
        private final LabelWorkflowService.PreparedJob shipmentJob = PreviewSelectionTestData.shipmentJob(
                "SHIP1",
                List.of(new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of()))
        );
        private final AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob =
                PreviewSelectionTestData.carrierMoveJob(
                        "CM1",
                        List.of(PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentJob)))
                );
        private final java.util.ArrayList<String> shipmentIds = new java.util.ArrayList<>();
        private final java.util.ArrayList<String> carrierMoveIds = new java.util.ArrayList<>();

        @Override
        public AdvancedPrintWorkflowService.PrintResult printShipment(
                LabelWorkflowService.PreparedJob shipmentJob,
                String printerId,
                Path outputDir,
                boolean printToFile
        ) {
            shipmentIds.add(shipmentJob.getShipmentId());
            return new AdvancedPrintWorkflowService.PrintResult(2, 1, Path.of("out", "shipment"), printerId, null, printToFile);
        }

        @Override
        public AdvancedPrintWorkflowService.PrintResult printCarrierMove(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob,
                String printerId,
                Path outputDir,
                boolean printToFile
        ) {
            carrierMoveIds.add(carrierMoveJob.getCarrierMoveId());
            return new AdvancedPrintWorkflowService.PrintResult(3, 2, Path.of("out", "carrier"), printerId, null, printToFile);
        }
    }
}

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CarrierMovePreviewSupportTest {

    private final CarrierMovePreviewSupport support = new CarrierMovePreviewSupport();

    @Test
    void buildCarrierMoveMathText_shouldAggregateVisibleStopsAndShipments() {
        Lpn first = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn second = new Lpn("LPN-2", "S2", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipmentOne = PreviewSelectionTestData.shipmentJob("S1", List.of(first));
        LabelWorkflowService.PreparedJob shipmentTwo = PreviewSelectionTestData.shipmentJob("S2", List.of(second));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentOne)),
                        PreviewSelectionTestData.stopGroup(2, 2, List.of(shipmentTwo))
                ));

        String text = support.buildCarrierMoveMathText(carrierJob, 1, 10, 2, 2);

        assertTrue(text.contains("S1"));
        assertTrue(text.contains("Selected Labels: 2"));
        assertTrue(text.contains("Info Tags: 2"));
    }

    @Test
    void buildQueuePreview_shouldSummarizeShipmentAndCarrierItems() throws Exception {
        Lpn first = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipment = PreviewSelectionTestData.shipmentJob("S1", List.of(first));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipment))
                ));
        Constructor<AdvancedPrintWorkflowService.PreparedQueueItem> itemCtor =
                AdvancedPrintWorkflowService.PreparedQueueItem.class.getDeclaredConstructor(
                        AdvancedPrintWorkflowService.QueueItemType.class,
                        String.class,
                        LabelWorkflowService.PreparedJob.class,
                        AdvancedPrintWorkflowService.PreparedCarrierMoveJob.class
                );
        itemCtor.setAccessible(true);
        Constructor<AdvancedPrintWorkflowService.PreparedQueueJob> queueCtor =
                AdvancedPrintWorkflowService.PreparedQueueJob.class.getDeclaredConstructor(List.class);
        queueCtor.setAccessible(true);

        AdvancedPrintWorkflowService.PreparedQueueJob queueJob = queueCtor.newInstance(List.of(
                itemCtor.newInstance(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, "S1", shipment, null),
                itemCtor.newInstance(AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE, "CM1", null, carrierJob)
        ));

        String preview = support.buildQueuePreview(queueJob);

        assertTrue(preview.contains("Queue Items: 2"));
        assertTrue(preview.contains("Shipments: 1"));
        assertTrue(preview.contains("Carrier Moves: 1"));
        assertTrue(preview.contains("Carrier Stops: 1"));
        assertTrue(preview.contains("Carrier-Move Shipments: 1"));
        assertTrue(preview.contains("Total Shipments Covered: 2"));
        assertTrue(preview.contains(" - S:S1 | labels=1"));
        assertTrue(preview.contains(" - C:CM1 | stops=1 | shipments=1"));
        assertTrue(preview.contains("Total documents: 5"));
    }
}

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InfoTagPrintTaskSupportTest {

    private final InfoTagPrintTaskSupport support = new InfoTagPrintTaskSupport();

    @Test
    void buildShipmentInfoTask_shouldCreateShipmentInfoTaskMetadata() {
        LabelWorkflowService.PreparedJob job = shipmentJob("SHIP/1");

        AdvancedPrintWorkflowService.PrintTask task = support.buildShipmentInfoTask(job);

        assertEquals(AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG, task.kind);
        assertEquals("info-shipment-ship-1.zpl", task.fileName);
        assertEquals("INFO-SHIPMENT SHIP/1", task.payloadId);
        assertFalse(task.zpl.isBlank());
    }

    @Test
    void buildStopInfoTask_shouldCreateStopInfoTaskMetadata() {
        LabelWorkflowService.PreparedJob job = shipmentJob("S100");

        AdvancedPrintWorkflowService.PrintTask task = support.buildStopInfoTask(
                "CM1",
                2,
                5,
                42,
                List.of(job)
        );

        assertEquals(AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG, task.kind);
        assertEquals("info-stop-02-of-05.zpl", task.fileName);
        assertEquals("INFO-STOP 2", task.payloadId);
        assertFalse(task.zpl.isBlank());
    }

    @Test
    void buildFinalInfoTask_shouldCreateFinalCarrierMoveInfoTaskMetadata() {
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob job =
                PreviewSelectionTestData.carrierMoveJob("CM/1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentJob("S100")))
                ));

        AdvancedPrintWorkflowService.PrintTask task = support.buildFinalInfoTask(job);

        assertEquals(AdvancedPrintWorkflowService.TaskKind.FINAL_INFO_TAG, task.kind);
        assertEquals("info-final-cmid-cm-1.zpl", task.fileName);
        assertEquals("INFO-FINAL CM/1", task.payloadId);
        assertFalse(task.zpl.isBlank());
    }

    private static LabelWorkflowService.PreparedJob shipmentJob(String shipmentId) {
        Lpn lpn = new Lpn("LPN-" + shipmentId, shipmentId, null, 0, 0, 0.0, null, null, null, null, null, List.of());
        return PreviewSelectionTestData.shipmentJob(shipmentId, List.of(lpn));
    }
}

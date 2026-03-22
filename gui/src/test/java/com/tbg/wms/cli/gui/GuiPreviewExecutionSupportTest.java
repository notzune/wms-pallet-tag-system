package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiPreviewExecutionSupportTest {

    private final GuiPreviewExecutionSupport support = new GuiPreviewExecutionSupport();

    @Test
    void prepareRequest_shouldRequireModeSpecificInput() {
        IllegalArgumentException shipment = assertThrows(IllegalArgumentException.class,
                () -> support.prepareRequest(" ", false));
        IllegalArgumentException carrier = assertThrows(IllegalArgumentException.class,
                () -> support.prepareRequest(" ", true));

        assertEquals("Enter a Shipment ID.", shipment.getMessage());
        assertEquals("Enter a Carrier Move ID.", carrier.getMessage());
        assertEquals("SHIP1", support.prepareRequest(" SHIP1 ", false).inputId());
    }

    @Test
    void execute_shouldDispatchShipmentPreview() throws Exception {
        LabelWorkflowService.PreparedJob preparedJob =
                PreviewSelectionTestData.shipmentJob("SHIP1", List.of(new Lpn("L1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of())));
        RecordingPreviewLoader loader = new RecordingPreviewLoader(preparedJob, null);

        GuiPreviewExecutionSupport.PreparedPreview preview =
                support.execute(new GuiPreviewExecutionSupport.PreviewRequest("SHIP1", false), loader);

        assertFalse(preview.isCarrierMove());
        assertSame(preparedJob, preview.shipmentJob());
        assertEquals("SHIP1", loader.shipmentId);
    }

    @Test
    void execute_shouldDispatchCarrierMovePreview() throws Exception {
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of());
        RecordingPreviewLoader loader = new RecordingPreviewLoader(null, carrierMoveJob);

        GuiPreviewExecutionSupport.PreparedPreview preview =
                support.execute(new GuiPreviewExecutionSupport.PreviewRequest("CM1", true), loader);

        assertTrue(preview.isCarrierMove());
        assertSame(carrierMoveJob, preview.carrierMoveJob());
        assertEquals("CM1", loader.carrierMoveId);
    }

    @Test
    void buildOutcomes_shouldProvideStandardMessages() {
        GuiPreviewExecutionSupport.SuccessOutcome success = support.buildSuccessOutcome();
        GuiPreviewExecutionSupport.FailureOutcome failure =
                support.buildFailureOutcome(new IllegalStateException("top", new IllegalArgumentException("root")));

        assertEquals("Preview ready. Verify details, then click Confirm Print.", success.statusMessage());
        assertEquals("Preview failed.", failure.statusMessage());
        assertEquals("root", failure.errorMessage());
    }

    private static final class RecordingPreviewLoader implements GuiPreviewExecutionSupport.PreviewLoader {
        private final LabelWorkflowService.PreparedJob preparedJob;
        private final AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob;
        private String shipmentId;
        private String carrierMoveId;

        private RecordingPreviewLoader(
                LabelWorkflowService.PreparedJob preparedJob,
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob
        ) {
            this.preparedJob = preparedJob;
            this.carrierMoveJob = carrierMoveJob;
        }

        @Override
        public LabelWorkflowService.PreparedJob prepareShipmentJob(String shipmentId) {
            this.shipmentId = shipmentId;
            return preparedJob;
        }

        @Override
        public AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMoveJob(String carrierMoveId) {
            this.carrierMoveId = carrierMoveId;
            return carrierMoveJob;
        }
    }
}

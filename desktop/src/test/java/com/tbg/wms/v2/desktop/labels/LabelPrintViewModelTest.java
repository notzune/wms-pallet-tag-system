package com.tbg.wms.v2.desktop.labels;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LabelPrintViewModelTest {
    @Test
    void previewShipmentLoadsTaskSummaryAndEnablesPrint() {
        FakeWorkflow workflow = new FakeWorkflow();
        LabelPrintViewModel viewModel = new LabelPrintViewModel(workflow);

        viewModel.previewShipment(" 800123 ");

        assertEquals(LabelPrintViewModel.Mode.SHIPMENT, viewModel.mode());
        assertEquals("800123", viewModel.sourceId());
        assertEquals(3, viewModel.preview().taskCount());
        assertEquals(List.of("800123_LPN-1.zpl", "800123_LPN-2.zpl", "info-shipment-800123.zpl"),
                viewModel.preview().artifactNames());
        assertTrue(viewModel.canPrint());
        assertEquals("Ready to print 3 shipment artifacts.", viewModel.status());
    }

    @Test
    void previewCarrierMoveLoadsCarrierMoveSummary() {
        FakeWorkflow workflow = new FakeWorkflow();
        LabelPrintViewModel viewModel = new LabelPrintViewModel(workflow);

        viewModel.previewCarrierMove("456");

        assertEquals(LabelPrintViewModel.Mode.CARRIER_MOVE, viewModel.mode());
        assertEquals("456", viewModel.sourceId());
        assertEquals(7, viewModel.preview().taskCount());
        assertEquals("Ready to print 7 carrier move artifacts.", viewModel.status());
    }

    @Test
    void printToFileUsesCurrentPreviewAndReportsArtifacts() {
        FakeWorkflow workflow = new FakeWorkflow();
        LabelPrintViewModel viewModel = new LabelPrintViewModel(workflow);
        viewModel.previewShipment("800123");

        viewModel.printToFile(Path.of("out", "labels"));

        assertEquals(Path.of("out", "labels"), workflow.lastOutputDir);
        assertEquals("Printed 3 artifacts to out\\labels.", viewModel.status().replace('/', '\\'));
        assertEquals(3, viewModel.lastPrintResult().artifactsWritten());
    }

    private static final class FakeWorkflow implements LabelPrintWorkflow {
        private Path lastOutputDir;

        @Override
        public LabelPreview previewShipment(String shipmentId) {
            return new LabelPreview(
                    "shipment",
                    shipmentId,
                    3,
                    List.of("800123_LPN-1.zpl", "800123_LPN-2.zpl", "info-shipment-800123.zpl")
            );
        }

        @Override
        public LabelPreview previewCarrierMove(String carrierMoveId) {
            return new LabelPreview(
                    "carrier move",
                    carrierMoveId,
                    7,
                    List.of("stop-1.zpl", "stop-2.zpl")
            );
        }

        @Override
        public LabelPrintResult printToFile(LabelPreview preview, Path outputDir) {
            lastOutputDir = outputDir;
            return new LabelPrintResult(preview.taskCount(), 0, outputDir);
        }
    }
}

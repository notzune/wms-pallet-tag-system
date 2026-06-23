package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiAdvancedPrintRunnerTest {

    @Test
    void printShipment_shouldDelegateToGateway() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        GuiAdvancedPrintRunner runner = new GuiAdvancedPrintRunner(gateway);
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob job = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(lpn));

        AdvancedPrintWorkflowService.PrintResult result = runner.printShipment(
                job,
                List.of(lpn),
                "P1",
                Path.of("out"),
                false,
                true
        );

        assertSame(gateway.shipmentResult, result);
        assertSame(job, gateway.shipmentJob);
        assertEquals(List.of(lpn), gateway.selectedLpns);
        assertEquals("P1", gateway.printerId);
        assertEquals(Path.of("out"), gateway.outputDir);
        assertTrue(gateway.includeInfoTags);
    }

    @Test
    void printCarrierMove_shouldDelegateToGateway() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        GuiAdvancedPrintRunner runner = new GuiAdvancedPrintRunner(gateway);
        LabelWorkflowService.PreparedJob shipment = PreviewSelectionTestData.shipmentJob("SHIP1", List.of());
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob job = PreviewSelectionTestData.carrierMoveJob(
                "CM1",
                List.of(PreviewSelectionTestData.stopGroup(1, 1, List.of(shipment)))
        );
        LabelSelectionRef selection = LabelSelectionRef.forCarrierMove(1, "SHIP1", "LPN-1", 1);

        AdvancedPrintWorkflowService.PrintResult result = runner.printCarrierMove(
                job,
                List.of(selection),
                null,
                Path.of("out"),
                true,
                false
        );

        assertSame(gateway.carrierResult, result);
        assertSame(job, gateway.carrierMoveJob);
        assertEquals(List.of(selection), gateway.selectedCarrierLabels);
        assertEquals(Path.of("out"), gateway.outputDir);
        assertTrue(gateway.printToFile);
    }

    private static final class RecordingGateway implements GuiAdvancedPrintRunner.Gateway {
        private final AdvancedPrintWorkflowService.PrintResult shipmentResult =
                printResult(1, 1, Path.of("out"), "P1", "10.0.0.1:9100", false);
        private final AdvancedPrintWorkflowService.PrintResult carrierResult =
                printResult(1, 0, Path.of("out"), null, null, true);
        private LabelWorkflowService.PreparedJob shipmentJob;
        private List<Lpn> selectedLpns;
        private AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob;
        private List<LabelSelectionRef> selectedCarrierLabels;
        private String printerId;
        private Path outputDir;
        private boolean printToFile;
        private boolean includeInfoTags;

        @Override
        public AdvancedPrintWorkflowService.PrintResult printShipmentJob(
                LabelWorkflowService.PreparedJob preparedJob,
                List<Lpn> selectedLpns,
                String printerId,
                Path outputDir,
                boolean printToFile,
                boolean includeInfoTags
        ) {
            this.shipmentJob = preparedJob;
            this.selectedLpns = selectedLpns;
            this.printerId = printerId;
            this.outputDir = outputDir;
            this.printToFile = printToFile;
            this.includeInfoTags = includeInfoTags;
            return shipmentResult;
        }

        @Override
        public AdvancedPrintWorkflowService.PrintResult printCarrierMoveJob(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                List<LabelSelectionRef> selectedLabels,
                String printerId,
                Path outputDir,
                boolean printToFile,
                boolean includeInfoTags
        ) {
            this.carrierMoveJob = preparedCarrierJob;
            this.selectedCarrierLabels = selectedLabels;
            this.printerId = printerId;
            this.outputDir = outputDir;
            this.printToFile = printToFile;
            this.includeInfoTags = includeInfoTags;
            return carrierResult;
        }
    }

    private static AdvancedPrintWorkflowService.PrintResult printResult(
            int labels,
            int infoTags,
            Path outputDir,
            String printerId,
            String printerEndpoint,
            boolean printToFile
    ) {
        try {
            var ctor = AdvancedPrintWorkflowService.PrintResult.class.getDeclaredConstructor(
                    int.class, int.class, Path.class, String.class, String.class, boolean.class
            );
            ctor.setAccessible(true);
            return ctor.newInstance(labels, infoTags, outputDir, printerId, printerEndpoint, printToFile);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to construct PrintResult fixture.", ex);
        }
    }
}

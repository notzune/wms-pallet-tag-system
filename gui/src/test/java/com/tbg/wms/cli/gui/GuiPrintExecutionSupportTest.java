package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiPrintExecutionSupportTest {

    private final GuiPrintExecutionSupport support = new GuiPrintExecutionSupport(new GuiPrintFlowSupport());

    @Test
    void prepareExecution_shouldRequireConfirmationForNetworkPrint() {
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob preparedJob = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(lpn));
        PreviewSelectionSupport.SelectionSnapshot selection = new PreviewSelectionSupport.SelectionSnapshot(
                List.of(new PreviewSelectionSupport.LabelOption("01", lpn, null)),
                List.of(lpn),
                List.of(),
                1
        );

        GuiPrintExecutionSupport.PreparedExecution execution = support.prepareExecution(new GuiPrintExecutionSupport.PrintRequest(
                false,
                preparedJob,
                null,
                new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1:9100"),
                false,
                selection,
                true,
                Path.of("out"),
                "20260322-000000"
        ));

        assertTrue(execution.requiresConfirmation());
        assertTrue(execution.confirmationMessage().contains("Print 1 labels"));
        assertEquals("P1", execution.plan().printerId());
    }

    @Test
    void prepareExecution_shouldSkipConfirmationForPrintToFile() {
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob preparedJob = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(lpn));
        PreviewSelectionSupport.SelectionSnapshot selection = new PreviewSelectionSupport.SelectionSnapshot(
                List.of(new PreviewSelectionSupport.LabelOption("01", lpn, null)),
                List.of(lpn),
                List.of(),
                0
        );

        GuiPrintExecutionSupport.PreparedExecution execution = support.prepareExecution(new GuiPrintExecutionSupport.PrintRequest(
                false,
                preparedJob,
                null,
                new LabelWorkflowService.PrinterOption("FILE", "Print to file", "out"),
                true,
                selection,
                false,
                Path.of("out"),
                "20260322-000000"
        ));

        assertFalse(execution.requiresConfirmation());
        assertNull(execution.confirmationMessage());
    }

    @Test
    void execute_shouldDispatchShipmentPrint() throws Exception {
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob preparedJob = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(lpn));
        GuiPrintFlowSupport.PrintPlan plan = new GuiPrintFlowSupport.PrintPlan(
                false,
                false,
                "P1",
                Path.of("out"),
                List.of(lpn),
                List.of(),
                1,
                1,
                true
        );
        GuiPrintExecutionSupport.PreparedExecution execution =
                new GuiPrintExecutionSupport.PreparedExecution(plan, preparedJob, null, "confirm");
        RecordingPrintRunner service = new RecordingPrintRunner();

        AdvancedPrintWorkflowService.PrintResult result = support.execute(execution, service);

        assertSame(service.shipmentResult, result);
        assertEquals("SHIP1", service.shipmentJobId);
        assertEquals(List.of(lpn), service.shipmentLpns);
        assertEquals("P1", service.printerId);
    }

    @Test
    void execute_shouldDispatchCarrierMovePrint() throws Exception {
        LabelWorkflowService.PreparedJob shipment = PreviewSelectionTestData.shipmentJob("SHIP1", List.of());
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob = PreviewSelectionTestData.carrierMoveJob(
                "CM1",
                List.of(PreviewSelectionTestData.stopGroup(1, 1, List.of(shipment)))
        );
        LabelSelectionRef selectionRef = LabelSelectionRef.forCarrierMove(1, "SHIP1", "LPN-1", 1);
        GuiPrintFlowSupport.PrintPlan plan = new GuiPrintFlowSupport.PrintPlan(
                true,
                true,
                null,
                Path.of("out"),
                List.of(),
                List.of(selectionRef),
                1,
                0,
                false
        );
        GuiPrintExecutionSupport.PreparedExecution execution =
                new GuiPrintExecutionSupport.PreparedExecution(plan, null, carrierJob, null);
        RecordingPrintRunner service = new RecordingPrintRunner();

        AdvancedPrintWorkflowService.PrintResult result = support.execute(execution, service);

        assertSame(service.carrierResult, result);
        assertEquals("CM1", service.carrierMoveId);
        assertEquals(List.of(selectionRef), service.selectedCarrierLabels);
        assertTrue(service.printToFile);
    }

    @Test
    void buildOutcomes_shouldUsePrintAndExceptionSupport() {
        AdvancedPrintWorkflowService.PrintResult result = printResult(2, 1, Path.of("out"), "P1", "10.0.0.1:9100", false);

        GuiPrintExecutionSupport.CompletionOutcome completion = support.buildCompletionOutcome(result);
        GuiPrintExecutionSupport.FailureOutcome failure =
                support.buildFailureOutcome(new IllegalStateException("top", new IllegalArgumentException("root cause")));

        assertEquals("Printed 2 labels and 1 info tags to P1 (10.0.0.1:9100)", completion.statusMessage());
        assertTrue(completion.dialogMessage().contains("Printed 2 labels and 1 info tags"));
        assertEquals("Print failed.", failure.statusMessage());
        assertEquals("root cause", failure.errorMessage());
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

    private static final class RecordingPrintRunner implements GuiPrintExecutionSupport.PrintRunner {
        private final AdvancedPrintWorkflowService.PrintResult shipmentResult =
                printResult(1, 1, Path.of("out"), "P1", "10.0.0.1:9100", false);
        private final AdvancedPrintWorkflowService.PrintResult carrierResult =
                printResult(1, 0, Path.of("out"), null, null, true);
        private String shipmentJobId;
        private List<Lpn> shipmentLpns;
        private String printerId;
        private String carrierMoveId;
        private List<LabelSelectionRef> selectedCarrierLabels;
        private boolean printToFile;

        @Override
        public AdvancedPrintWorkflowService.PrintResult printShipment(
                LabelWorkflowService.PreparedJob preparedJob,
                List<Lpn> selectedLpns,
                String printerId,
                Path outputDir,
                boolean printToFile,
                boolean includeInfoTags
        ) {
            this.shipmentJobId = preparedJob.getShipmentId();
            this.shipmentLpns = selectedLpns;
            this.printerId = printerId;
            return shipmentResult;
        }

        @Override
        public AdvancedPrintWorkflowService.PrintResult printCarrierMove(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                List<LabelSelectionRef> selectedLabels,
                String printerId,
                Path outputDir,
                boolean printToFile,
                boolean includeInfoTags
        ) {
            this.carrierMoveId = preparedCarrierJob.getCarrierMoveId();
            this.selectedCarrierLabels = selectedLabels;
            this.printToFile = printToFile;
            return carrierResult;
        }
    }
}

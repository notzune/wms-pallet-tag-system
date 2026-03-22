package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiPrintFlowSupportTest {

    private final GuiPrintFlowSupport support = new GuiPrintFlowSupport();

    @Test
    void planPrint_shouldRejectMissingPreview() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> support.planPrint(
                false,
                null,
                null,
                new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1:9100"),
                false,
                new PreviewSelectionSupport.SelectionSnapshot(List.of(), List.of(), List.of(), 0),
                true,
                Path.of("out"),
                "20260322-000000"
        ));

        assertEquals("Run Preview first.", ex.getMessage());
    }

    @Test
    void planPrint_shouldBuildShipmentPlan() {
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob preparedJob = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(lpn));
        PreviewSelectionSupport.SelectionSnapshot selection =
                new PreviewSelectionSupport.SelectionSnapshot(
                        List.of(new PreviewSelectionSupport.LabelOption("01. LPN-1", lpn, null)),
                        List.of(lpn),
                        List.of(),
                        1
                );

        GuiPrintFlowSupport.PrintPlan plan = support.planPrint(
                false,
                preparedJob,
                null,
                new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1:9100"),
                false,
                selection,
                true,
                Path.of("out"),
                "20260322-000000"
        );

        assertEquals("P1", plan.printerId());
        assertEquals(Path.of("out", "gui-SHIP1-20260322-000000"), plan.outputDir());
        assertEquals(1, plan.plannedLabels());
        assertEquals(1, plan.plannedInfoTags());
    }

    @Test
    void planPrint_shouldBuildCarrierMovePlan() {
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipment = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(lpn));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipment))
                ));
        LabelSelectionRef selectionRef = LabelSelectionRef.forCarrierMove(1, "SHIP1", "LPN-1", 1);
        PreviewSelectionSupport.SelectionSnapshot selection =
                new PreviewSelectionSupport.SelectionSnapshot(
                        List.of(new PreviewSelectionSupport.LabelOption("01", lpn, selectionRef)),
                        List.of(lpn),
                        List.of(selectionRef),
                        2
                );

        GuiPrintFlowSupport.PrintPlan plan = support.planPrint(
                true,
                null,
                carrierJob,
                new LabelWorkflowService.PrinterOption("FILE", "Print to file", "out"),
                true,
                selection,
                true,
                Path.of("out"),
                "20260322-000000"
        );

        assertNull(plan.printerId());
        assertEquals(Path.of("out", "gui-cmid-CM1-20260322-000000"), plan.outputDir());
        assertEquals(1, plan.plannedLabels());
        assertEquals(2, plan.plannedInfoTags());
    }

    @Test
    void buildCompletionMessages_shouldDifferentiatePrinterAndFile() {
        AdvancedPrintWorkflowService.PrintResult printToFile =
                printResult(2, 1, Path.of("out"), "FILE", "FILE", true);
        AdvancedPrintWorkflowService.PrintResult network =
                printResult(2, 1, Path.of("out"), "P1", "10.0.0.1:9100", false);

        assertEquals("Saved 2 labels and 1 info tags to out", support.buildCompletionStatus(printToFile));
        assertEquals("Printed 2 labels and 1 info tags to P1 (10.0.0.1:9100)", support.buildCompletionStatus(network));
        assertEquals("Saved 2 labels and 1 info tags.\nOutput: out", support.buildCompletionDialogMessage(printToFile));
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

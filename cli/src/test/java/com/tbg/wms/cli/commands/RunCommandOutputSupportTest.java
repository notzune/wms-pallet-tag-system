package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.AdvancedPrintWorkflowService;
import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.cli.gui.WorkflowPrintPlanSupport;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RunCommandOutputSupportTest {

    private final RunCommandOutputSupport support = new RunCommandOutputSupport();

    @Test
    void buildShipmentLabelPreview_shouldHandleNullLpnsWithoutThrowing() throws Exception {
        LabelWorkflowService.PreparedJob preparedJob = preparedJobWithLpns(Arrays.asList(
                new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of()),
                null
        ));

        String output = support.buildShipmentLabelPreview(preparedJob, List.of(), 100);

        assertTrue(output.contains("1. LPN-1"));
        assertTrue(output.contains("2. UNKNOWN"));
    }

    @Test
    void buildCompletionMessage_shouldDifferentiatePrintModes() throws Exception {
        AdvancedPrintWorkflowService.PrintResult fileResult = printResult(2, 1, Path.of("out"), "FILE", "FILE", true);
        AdvancedPrintWorkflowService.PrintResult printerResult = printResult(2, 1, Path.of("out"), "P1", "10.0.0.1:9100", false);

        assertTrue(support.buildCompletionMessage(fileResult).contains("Print-to-file mode"));
        assertTrue(support.buildCompletionMessage(printerResult).contains("Printed to: P1"));
    }

    @Test
    void buildCarrierMovePlanSummary_shouldIncludeKeyTotals() throws Exception {
        WorkflowPrintPlanSupport.CarrierMovePlanSummary plan = carrierPlanSummary();

        String output = support.buildCarrierMovePlanSummary(plan);

        assertTrue(output.contains("Carrier Move: CM1"));
        assertTrue(output.contains("Labels: 8"));
        assertTrue(output.contains("Info Tags: 3"));
    }

    @SuppressWarnings("java:S3011")
    private static LabelWorkflowService.PreparedJob preparedJobWithLpns(List<Lpn> lpns) throws Exception {
        Constructor<LabelWorkflowService.PreparedJob> constructor = LabelWorkflowService.PreparedJob.class
                .getDeclaredConstructor(
                        String.class,
                        com.tbg.wms.core.model.Shipment.class,
                        com.tbg.wms.core.print.PrinterRoutingService.class,
                        com.tbg.wms.core.label.SiteConfig.class,
                        com.tbg.wms.core.sku.SkuMappingService.class,
                        com.tbg.wms.core.template.LabelTemplate.class,
                        java.util.Map.class,
                        com.tbg.wms.core.model.PalletPlanningService.PlanResult.class,
                        List.class,
                        List.class,
                        boolean.class,
                        String.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance("S1", null, null, null, null, null, null, null, lpns, List.of(), false, null);
    }

    private static AdvancedPrintWorkflowService.PrintResult printResult(
            int labels,
            int infoTags,
            Path outputDir,
            String printerId,
            String printerEndpoint,
            boolean printToFile
    ) throws Exception {
        Constructor<AdvancedPrintWorkflowService.PrintResult> constructor =
                AdvancedPrintWorkflowService.PrintResult.class.getDeclaredConstructor(
                        int.class, int.class, Path.class, String.class, String.class, boolean.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance(labels, infoTags, outputDir, printerId, printerEndpoint, printToFile);
    }

    private static WorkflowPrintPlanSupport.CarrierMovePlanSummary carrierPlanSummary() throws Exception {
        Constructor<WorkflowPrintPlanSupport.CarrierMovePlanSummary> constructor =
                WorkflowPrintPlanSupport.CarrierMovePlanSummary.class.getDeclaredConstructor(
                        String.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance("CM1", 4, 100, 6, 2, 4, 8, 8, 3);
    }
}

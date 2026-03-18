package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunCommandTest {

    @Test
    void printShipmentLabelPreview_shouldHandleNullLpnsWithoutThrowing() throws Exception {
        RunCommand command = new RunCommand();
        LabelWorkflowService.PreparedJob preparedJob = preparedJobWithLpns(Arrays.asList(
                new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of()),
                null
        ));
        Method method = RunCommand.class.getDeclaredMethod(
                "printShipmentLabelPreview",
                LabelWorkflowService.PreparedJob.class,
                List.class
        );
        method.setAccessible(true);

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try (PrintStream capture = new PrintStream(outputBuffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);

            assertDoesNotThrow(() -> method.invoke(command, preparedJob, List.of()));
        } finally {
            System.setOut(originalOut);
        }

        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("1. LPN-1"));
        assertTrue(output.contains("2. UNKNOWN"));
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
}

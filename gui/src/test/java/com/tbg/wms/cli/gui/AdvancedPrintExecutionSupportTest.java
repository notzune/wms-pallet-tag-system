package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedPrintExecutionSupportTest {

    @Test
    void executeShipmentJob_shouldCreateCheckpointAndReturnMappedResult() throws Exception {
        RecordingCheckpointGateway gateway = new RecordingCheckpointGateway();
        AdvancedPrintExecutionSupport support = new AdvancedPrintExecutionSupport(
                gateway,
                new AdvancedPrintResultSupport(),
                DateTimeFormatter.ofPattern("'TS'")
        );
        PrinterRoutingService routing = new PrinterRoutingService(
                Map.of("P1", new PrinterConfig("P1", "Printer 1", "10.0.0.1", 9100, List.of(), List.of(), "Dock", true)),
                List.of(),
                "P1",
                "TBG3002"
        );
        LabelWorkflowService.PreparedJob job = preparedJob("SHIP1", routing, List.of(
                new Lpn("LPN-1", "SHIP1", null, 0, 0, 0.0, null, null, null, null, null, List.of())
        ));
        List<AdvancedPrintWorkflowService.PrintTask> tasks = List.of(
                task(AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL),
                task(AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG)
        );

        AdvancedPrintWorkflowService.PrintResult result =
                support.executeShipmentJob(job, "P1", Path.of("out"), false, tasks);

        assertEquals("shipment-SHIP1-TS", gateway.checkpoint.id);
        assertEquals(AdvancedPrintWorkflowService.InputMode.SHIPMENT, gateway.checkpoint.mode);
        assertEquals("SHIP1", gateway.checkpoint.sourceId);
        assertEquals(2, result.getLabelsPrinted() + result.getInfoTagsPrinted());
        assertSame(gateway.checkpoint, gateway.executedCheckpoint);
        assertEquals("P1", gateway.executedPrinter.getId());
    }

    @Test
    void executeCarrierMoveJob_shouldBuildDefaultOutputDirWhenMissing() throws Exception {
        RecordingCheckpointGateway gateway = new RecordingCheckpointGateway();
        AdvancedPrintExecutionSupport support = new AdvancedPrintExecutionSupport(
                gateway,
                new AdvancedPrintResultSupport(),
                DateTimeFormatter.ofPattern("'TS'")
        );
        PrinterRoutingService routing = new PrinterRoutingService(
                Map.of("P1", new PrinterConfig("P1", "Printer 1", "10.0.0.1", 9100, List.of(), List.of(), "Dock", true)),
                List.of(),
                "P1",
                "TBG3002"
        );
        LabelWorkflowService.PreparedJob shipment = preparedJob("SHIP1", routing, List.of());
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob job =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipment))
                ));
        List<AdvancedPrintWorkflowService.PrintTask> tasks = List.of(
                task(AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL),
                task(AdvancedPrintWorkflowService.TaskKind.FINAL_INFO_TAG)
        );

        AdvancedPrintWorkflowService.PrintResult result =
                support.executeCarrierMoveJob(job, routing, "P1", null, false, tasks);

        assertTrue(gateway.checkpoint.outputDirectory.endsWith("out\\gui-cmid-CM1-TS") ||
                gateway.checkpoint.outputDirectory.endsWith("out/gui-cmid-CM1-TS"));
        assertEquals("P1", result.getPrinterId());
    }

    private static LabelWorkflowService.PreparedJob preparedJob(
            String shipmentId,
            PrinterRoutingService routing,
            List<Lpn> lpns
    ) {
        try {
            Constructor<LabelWorkflowService.PreparedJob> ctor = LabelWorkflowService.PreparedJob.class.getDeclaredConstructor(
                    String.class,
                    com.tbg.wms.core.model.Shipment.class,
                    PrinterRoutingService.class,
                    com.tbg.wms.core.label.SiteConfig.class,
                    com.tbg.wms.core.sku.SkuMappingService.class,
                    com.tbg.wms.core.template.LabelTemplate.class,
                    Map.class,
                    com.tbg.wms.core.model.PalletPlanningService.PlanResult.class,
                    List.class,
                    List.class,
                    boolean.class,
                    String.class
            );
            ctor.setAccessible(true);
            return ctor.newInstance(
                    shipmentId,
                    PreviewSelectionTestData.shipmentJob(shipmentId, lpns).getShipment(),
                    routing,
                    null,
                    null,
                    null,
                    Map.of(),
                    PreviewSelectionTestData.shipmentJob(shipmentId, lpns).getPlanResult(),
                    lpns,
                    List.of(),
                    false,
                    "STAGE"
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create PreparedJob test fixture.", ex);
        }
    }

    private static AdvancedPrintWorkflowService.PrintTask task(AdvancedPrintWorkflowService.TaskKind kind) {
        AdvancedPrintWorkflowService.PrintTask task = new AdvancedPrintWorkflowService.PrintTask();
        task.kind = kind;
        task.fileName = "out.zpl";
        task.zpl = "^XA^XZ";
        task.payloadId = LabelSelectionRef.forShipment(1, "SHIP1", "LPN-1").toString();
        return task;
    }

    private static final class RecordingCheckpointGateway implements AdvancedPrintExecutionSupport.CheckpointGateway {
        private AdvancedPrintWorkflowService.JobCheckpoint checkpoint;
        private AdvancedPrintWorkflowService.JobCheckpoint executedCheckpoint;
        private PrinterConfig executedPrinter;

        @Override
        public AdvancedPrintWorkflowService.JobCheckpoint createCheckpoint(
                String id,
                AdvancedPrintWorkflowService.InputMode mode,
                String sourceId,
                Path outputDir,
                boolean printToFile,
                PrinterConfig printer,
                List<AdvancedPrintWorkflowService.PrintTask> tasks
        ) {
            checkpoint = new AdvancedPrintWorkflowService.JobCheckpoint();
            checkpoint.id = id;
            checkpoint.mode = mode;
            checkpoint.sourceId = sourceId;
            checkpoint.outputDirectory = outputDir.toString();
            checkpoint.printToFile = printToFile;
            checkpoint.printerId = printToFile ? "FILE" : printer.getId();
            checkpoint.printerEndpoint = printToFile ? "FILE" : printer.getEndpoint();
            checkpoint.tasks = tasks;
            return checkpoint;
        }

        @Override
        public void executeTasks(
                AdvancedPrintWorkflowService.JobCheckpoint checkpoint,
                PrinterConfig printer,
                int startIndex
        ) {
            this.executedCheckpoint = checkpoint;
            this.executedPrinter = printer;
        }
    }
}

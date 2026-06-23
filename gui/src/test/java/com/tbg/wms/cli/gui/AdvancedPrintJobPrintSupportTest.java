package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedPrintJobPrintSupportTest {

    @TempDir
    private Path tempDir;

    @Test
    void printShipmentJob_shouldFilterSelectedLpnsAndExecuteBuiltTasks() throws Exception {
        Lpn first = lpn("LPN-1", "000000000000000001");
        Lpn second = lpn("LPN-2", "000000000000000002");
        LabelWorkflowService.PreparedJob job = preparedJob("SHIP1", List.of(first, second));
        RecordingExecutionGateway gateway = new RecordingExecutionGateway();
        AdvancedPrintJobPrintSupport support = new AdvancedPrintJobPrintSupport(gateway);

        AdvancedPrintWorkflowService.PrintResult result = support.printShipmentJob(
                job,
                List.of(second),
                "P1",
                Path.of("out"),
                false,
                true
        );

        assertSame(gateway.shipmentResult, result);
        assertSame(job, gateway.shipmentJob);
        assertEquals("P1", gateway.printerId);
        assertEquals(Path.of("out"), gateway.outputDir);
        assertEquals(2, gateway.tasks.size());
        assertEquals(1, gateway.tasks.stream()
                .filter(task -> task.kind == AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL)
                .count());
        assertEquals(1, gateway.tasks.stream()
                .filter(task -> task.kind == AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG)
                .count());
        assertTrue(gateway.tasks.stream().anyMatch(task -> task.payloadId.contains("LPN-2")));
    }

    @Test
    void printCarrierMoveJob_shouldBuildCarrierTasksAndExecuteWithFirstShipmentRouting() throws Exception {
        Lpn lpn = lpn("LPN-1", "000000000000000001");
        LabelWorkflowService.PreparedJob shipment = preparedJob("SHIP1", List.of(lpn));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob job = PreviewSelectionTestData.carrierMoveJob(
                "CM1",
                List.of(PreviewSelectionTestData.stopGroup(10, 1, List.of(shipment)))
        );
        LabelSelectionRef selected = LabelSelectionRef.forCarrierMove(1, "SHIP1", "LPN-1", 1);
        RecordingExecutionGateway gateway = new RecordingExecutionGateway();
        AdvancedPrintJobPrintSupport support = new AdvancedPrintJobPrintSupport(gateway);

        AdvancedPrintWorkflowService.PrintResult result = support.printCarrierMoveJob(
                job,
                List.of(selected),
                "P1",
                Path.of("out"),
                false,
                true
        );

        assertSame(gateway.carrierResult, result);
        assertSame(job, gateway.carrierMoveJob);
        assertSame(shipment.getRouting(), gateway.routing);
        assertEquals(3, gateway.tasks.size());
        assertEquals(1, gateway.tasks.stream()
                .filter(task -> task.kind == AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL)
                .count());
        assertEquals(1, gateway.tasks.stream()
                .filter(task -> task.kind == AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG)
                .count());
        assertEquals(1, gateway.tasks.stream()
                .filter(task -> task.kind == AdvancedPrintWorkflowService.TaskKind.FINAL_INFO_TAG)
                .count());
    }

    private LabelWorkflowService.PreparedJob preparedJob(String shipmentId, List<Lpn> lpns) throws Exception {
        Files.writeString(tempDir.resolve("sku.csv"), "TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU\n");
        Constructor<LabelWorkflowService.PreparedJob> ctor = LabelWorkflowService.PreparedJob.class.getDeclaredConstructor(
                String.class,
                Shipment.class,
                PrinterRoutingService.class,
                SiteConfig.class,
                SkuMappingService.class,
                LabelTemplate.class,
                Map.class,
                PalletPlanningService.PlanResult.class,
                List.class,
                List.class,
                boolean.class,
                String.class
        );
        ctor.setAccessible(true);
        return ctor.newInstance(
                shipmentId,
                shipment(shipmentId, lpns),
                null,
                new SiteConfig("Ship From", "1 Main", "City, ST 12345"),
                new SkuMappingService(tempDir.resolve("sku.csv")),
                new LabelTemplate("TEST", "^XA^FD{lpnId}^FS^FD{palletSeq}/{palletTotal}^FS^FD{stopSequence}^FS^XZ"),
                Map.<String, ShipmentSkuFootprint>of(),
                planResult(lpns.size()),
                lpns,
                List.of(),
                false,
                "STAGE"
        );
    }

    private static Shipment shipment(String shipmentId, List<Lpn> lpns) {
        return new Shipment(
                shipmentId,
                shipmentId + "-EXT",
                shipmentId + "-ORDER",
                "3002",
                "Ship To",
                "123 Any St",
                null,
                null,
                "City",
                "ST",
                "12345",
                "USA",
                null,
                "CARRIER",
                "TL",
                null,
                null,
                null,
                null,
                "6080",
                null,
                null,
                1,
                "CMID",
                null,
                null,
                "R",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                lpns
        );
    }

    private static PalletPlanningService.PlanResult planResult(int labelCount) throws Exception {
        Constructor<PalletPlanningService.PlanResult> ctor = PalletPlanningService.PlanResult.class.getDeclaredConstructor(
                int.class,
                int.class,
                int.class,
                int.class,
                List.class
        );
        ctor.setAccessible(true);
        return ctor.newInstance(labelCount, labelCount, 0, labelCount, List.of());
    }

    private static Lpn lpn(String id, String sscc) {
        return new Lpn(id, "SHIP1", sscc, 0, 0, 0.0, null, null, null, null, null, List.of());
    }

    private static final class RecordingExecutionGateway implements AdvancedPrintJobPrintSupport.ExecutionGateway {
        private final AdvancedPrintWorkflowService.PrintResult shipmentResult =
                new AdvancedPrintWorkflowService.PrintResult(1, 1, Path.of("out"), "P1", "10.0.0.1", false);
        private final AdvancedPrintWorkflowService.PrintResult carrierResult =
                new AdvancedPrintWorkflowService.PrintResult(1, 2, Path.of("out"), "P1", "10.0.0.1", false);
        private LabelWorkflowService.PreparedJob shipmentJob;
        private AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob;
        private PrinterRoutingService routing;
        private String printerId;
        private Path outputDir;
        private List<AdvancedPrintWorkflowService.PrintTask> tasks = List.of();

        @Override
        public AdvancedPrintWorkflowService.PrintResult executeShipmentJob(
                LabelWorkflowService.PreparedJob job,
                String printerId,
                Path outputDir,
                boolean printToFile,
                List<AdvancedPrintWorkflowService.PrintTask> tasks
        ) {
            this.shipmentJob = job;
            this.printerId = printerId;
            this.outputDir = outputDir;
            this.tasks = tasks;
            return shipmentResult;
        }

        @Override
        public AdvancedPrintWorkflowService.PrintResult executeCarrierMoveJob(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
                PrinterRoutingService routing,
                String printerId,
                Path outputDir,
                boolean printToFile,
                List<AdvancedPrintWorkflowService.PrintTask> tasks
        ) {
            this.carrierMoveJob = job;
            this.routing = routing;
            this.printerId = printerId;
            this.outputDir = outputDir;
            this.tasks = tasks;
            return carrierResult;
        }
    }
}

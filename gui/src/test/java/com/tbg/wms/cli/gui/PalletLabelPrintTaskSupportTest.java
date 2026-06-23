package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PalletLabelPrintTaskSupportTest {

    @TempDir
    private Path tempDir;

    private final PalletLabelPrintTaskSupport support = new PalletLabelPrintTaskSupport();

    @Test
    void buildPalletLabelTasks_shouldRenderPalletLabelTasksWithStableMetadata() throws Exception {
        Lpn first = lpn("LPN/1", "000000000000000001");
        Lpn second = lpn("LPN/2", "000000000000000002");
        LabelWorkflowService.PreparedJob job = preparedJob("SHIP/1", List.of(first, second), false);
        PrintTaskPlanner.ShipmentPrintBatch batch =
                PrintTaskPlanner.ShipmentPrintBatch.forShipment(job, List.of(first, second), false);

        List<AdvancedPrintWorkflowService.PrintTask> tasks = support.buildPalletLabelTasks(batch);

        assertEquals(2, tasks.size());
        assertEquals(AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL, tasks.get(0).kind);
        assertEquals("ship-1_lpn-1_1_of_2.zpl", tasks.get(0).fileName);
        assertEquals("SHIP/1:LPN/1", tasks.get(0).payloadId);
        assertTrue(tasks.get(0).zpl.contains("^FDLPN/1^FS"));
        assertTrue(tasks.get(0).zpl.contains("^FD1/2^FS"));
        assertEquals("ship-1_lpn-2_2_of_2.zpl", tasks.get(1).fileName);
        assertEquals("SHIP/1:LPN/2", tasks.get(1).payloadId);
    }

    @Test
    void buildPalletLabelTasks_shouldIncludeCarrierStopContext() throws Exception {
        Lpn lpn = lpn("LPN1", "000000000000000001");
        LabelWorkflowService.PreparedJob job = preparedJob("SHIP1", List.of(lpn), false);
        PrintTaskPlanner.ShipmentPrintBatch batch =
                PrintTaskPlanner.ShipmentPrintBatch.forCarrierStop(job, List.of(lpn), 7, 3);

        AdvancedPrintWorkflowService.PrintTask task = support.buildPalletLabelTasks(batch).get(0);

        assertEquals("SHIP1:LPN1 stop 3", task.payloadId);
        assertTrue(task.zpl.contains("^FD7^FS"));
    }

    @Test
    void buildPalletLabelTasks_shouldRejectTooManyLabels() throws Exception {
        Lpn lpn = lpn("LPN1", "000000000000000001");
        LabelWorkflowService.PreparedJob job = preparedJob("SHIP1", List.of(lpn), false);
        List<Lpn> selected = new ArrayList<>();
        for (int i = 0; i < 10_001; i++) {
            selected.add(lpn("LPN" + i, String.format("%018d", i + 1)));
        }
        PrintTaskPlanner.ShipmentPrintBatch batch =
                PrintTaskPlanner.ShipmentPrintBatch.forShipment(job, selected, false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> support.buildPalletLabelTasks(batch)
        );

        assertEquals("Label count exceeds max limit: 10000", ex.getMessage());
    }

    private LabelWorkflowService.PreparedJob preparedJob(String shipmentId, List<Lpn> lpns, boolean virtualLabels) throws Exception {
        Files.writeString(tempDir.resolve("sku.csv"), "TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU\n");
        Constructor<LabelWorkflowService.PreparedJob> ctor = LabelWorkflowService.PreparedJob.class.getDeclaredConstructor(
                String.class,
                Shipment.class,
                com.tbg.wms.core.print.PrinterRoutingService.class,
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
                virtualLabels,
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

    private static Lpn lpn(String lpnId, String sscc) {
        return new Lpn(lpnId, "SHIP1", sscc, 0, 0, 0.0, null, null, null, null, null, List.of());
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
}

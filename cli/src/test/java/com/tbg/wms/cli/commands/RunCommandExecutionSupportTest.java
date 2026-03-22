package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunCommandExecutionSupportTest {

    private final RunCommandExecutionSupport support = new RunCommandExecutionSupport();

    @Test
    void resolveInputId_shouldRequireExactlyOneMode() {
        assertThrows(IllegalArgumentException.class, () -> support.resolveInputId(null, null));
        assertThrows(IllegalArgumentException.class, () -> support.resolveInputId("SHIP1", "CM1"));
        assertEquals("SHIP1", support.resolveInputId(" SHIP1 ", null));
        assertEquals("CM1", support.resolveInputId(null, " CM1 "));
    }

    @Test
    void resolveSelectedShipmentLpns_shouldHonorSelectionExpression() {
        Lpn one = new Lpn("LPN-1", "SHIP1", "SSCC-1", 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn two = new Lpn("LPN-2", "SHIP1", "SSCC-2", 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob prepared = shipmentJob("SHIP1", List.of(one, two));

        List<Lpn> selected = support.resolveSelectedShipmentLpns(prepared, "2");

        assertEquals(List.of(two), selected);
        assertSame(prepared.getLpnsForLabels(), support.resolveSelectedShipmentLpns(prepared, null));
    }

    @Test
    void resolvePrinterId_shouldPreferPrintToFileThenOverrideThenRouting() throws Exception {
        PrinterRoutingService routing = new PrinterRoutingService(
                Map.of("AUTO", new PrinterConfig("AUTO", "Auto", "10.0.0.1", 9100, List.of(), List.of(), "Dock", true)),
                List.of(),
                "AUTO",
                "TBG3002"
        );

        assertNull(support.resolvePrinterId(true, "", routing, "ROSSI"));
        assertEquals("MANUAL", support.resolvePrinterId(false, " MANUAL ", routing, "ROSSI"));
        assertEquals("AUTO", support.resolvePrinterId(false, "", routing, "ROSSI"));
    }

    @Test
    void prepareOutputDirectory_shouldCreateDirectory(@TempDir Path tempDir) throws Exception {
        Path output = support.prepareOutputDirectory(tempDir.resolve("nested").toString());

        assertNotNull(output);
        assertEquals(true, Files.isDirectory(output));
    }

    @Test
    void enforceMaxLabels_shouldRejectUnsafeCounts() {
        support.enforceMaxLabels(10_000);
        assertThrows(IllegalArgumentException.class, () -> support.enforceMaxLabels(10_001));
    }

    private static LabelWorkflowService.PreparedJob shipmentJob(String shipmentId, List<Lpn> lpns) {
        try {
            Constructor<LabelWorkflowService.PreparedJob> ctor = LabelWorkflowService.PreparedJob.class.getDeclaredConstructor(
                    String.class,
                    Shipment.class,
                    PrinterRoutingService.class,
                    com.tbg.wms.core.label.SiteConfig.class,
                    com.tbg.wms.core.sku.SkuMappingService.class,
                    com.tbg.wms.core.template.LabelTemplate.class,
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
                    null,
                    null,
                    null,
                    Map.<String, ShipmentSkuFootprint>of(),
                    planResult(lpns.size()),
                    lpns,
                    List.of(),
                    false,
                    "STAGE"
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create PreparedJob test fixture.", ex);
        }
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

    private static PalletPlanningService.PlanResult planResult(int labelCount) {
        try {
            Constructor<PalletPlanningService.PlanResult> ctor = PalletPlanningService.PlanResult.class.getDeclaredConstructor(
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    List.class
            );
            ctor.setAccessible(true);
            return ctor.newInstance(labelCount, labelCount, 0, labelCount, List.of());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create PlanResult test fixture.", ex);
        }
    }
}

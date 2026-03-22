package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelWorkflowPrintSupportTest {

    @Test
    void print_shouldWriteZplArtifactsForPrintToFile(@TempDir Path tempDir) throws Exception {
        RecordingPrintDispatcher dispatcher = new RecordingPrintDispatcher();
        LabelWorkflowPrintSupport support = new LabelWorkflowPrintSupport(dispatcher);
        LabelWorkflowService.PreparedJob job = printableJob(tempDir, "SHIP1", List.of(
                lpn("LPN-1", "SSCC-1"),
                lpn("LPN-2", "SSCC-2")
        ), false);

        LabelWorkflowService.PrintResult result = support.print(job, null, tempDir.resolve("out"), true);

        assertTrue(result.isPrintToFile());
        assertEquals(2, result.getLabelsPrinted());
        assertEquals(0, dispatcher.invocationCount);
        assertTrue(Files.exists(tempDir.resolve("out").resolve("SHIP1_LPN-1_1_of_2.zpl")));
        assertTrue(Files.exists(tempDir.resolve("out").resolve("SHIP1_LPN-2_2_of_2.zpl")));
        String zpl = Files.readString(tempDir.resolve("out").resolve("SHIP1_LPN-1_1_of_2.zpl"));
        assertTrue(zpl.contains("^FDLPN-1^FS"));
    }

    @Test
    void print_shouldDispatchToPrinterWhenNotPrintToFile(@TempDir Path tempDir) throws Exception {
        RecordingPrintDispatcher dispatcher = new RecordingPrintDispatcher();
        LabelWorkflowPrintSupport support = new LabelWorkflowPrintSupport(dispatcher);
        LabelWorkflowService.PreparedJob job = printableJob(tempDir, "SHIP2", List.of(lpn("LPN-9", "SSCC-9")), false);
        PrinterConfig printer = new PrinterConfig("P1", "Printer 1", "10.0.0.1", 9100, List.of(), List.of(), "Office", true);

        LabelWorkflowService.PrintResult result = support.print(job, printer, tempDir.resolve("out"), false);

        assertEquals(1, result.getLabelsPrinted());
        assertEquals("P1", result.getPrinterId());
        assertEquals(1, dispatcher.invocationCount);
        assertEquals("LPN-9", dispatcher.lastLabelId);
        assertTrue(dispatcher.lastZpl.contains("^FDSSCC-9^FS"));
    }

    @Test
    void print_shouldOverrideVirtualLabelSequenceTotals(@TempDir Path tempDir) throws Exception {
        RecordingPrintDispatcher dispatcher = new RecordingPrintDispatcher();
        LabelWorkflowPrintSupport support = new LabelWorkflowPrintSupport(dispatcher);
        LabelWorkflowService.PreparedJob job = printableJob(tempDir, "SHIP3", List.of(
                lpn("VLPN-1", "VSSCC-1"),
                lpn("VLPN-2", "VSSCC-2")
        ), true);

        support.print(job, null, tempDir.resolve("out"), true);

        String first = Files.readString(tempDir.resolve("out").resolve("SHIP3_VLPN-1_1_of_2.zpl"));
        String second = Files.readString(tempDir.resolve("out").resolve("SHIP3_VLPN-2_2_of_2.zpl"));
        assertTrue(first.contains("^FD1/2^FS"));
        assertTrue(second.contains("^FD2/2^FS"));
    }

    private static LabelWorkflowService.PreparedJob printableJob(
            Path tempDir,
            String shipmentId,
            List<Lpn> lpns,
            boolean usingVirtualLabels
    ) throws Exception {
        Path csv = tempDir.resolve("sku.csv");
        Files.writeString(csv, "TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU\n", StandardCharsets.UTF_8);
        SkuMappingService skuMapping = new SkuMappingService(csv);
        SiteConfig siteConfig = new SiteConfig("Ship From", "1 Main", "City, ST 12345");
        LabelTemplate template = new LabelTemplate("TEST", "^XA^FD{lpnId}^FS^FD{ssccBarcode}^FS^FD{palletSeq}/{palletTotal}^FS^XZ");

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
                shipment(shipmentId, usingVirtualLabels ? List.of(lpn("PHYSICAL", "PHYSICAL-SSCC")) : lpns),
                null,
                siteConfig,
                skuMapping,
                template,
                Map.<String, ShipmentSkuFootprint>of(),
                planResult(lpns.size()),
                lpns,
                List.of(),
                usingVirtualLabels,
                "STAGE"
        );
    }

    private static Lpn lpn(String lpnId, String sscc) {
        return new Lpn(lpnId, "SHIP", sscc, 100, 10, 50.0, "STAGE", "LOT1", "CLOT1", null, null, List.of());
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

    private static final class RecordingPrintDispatcher implements LabelWorkflowPrintSupport.PrintDispatcher {
        private int invocationCount;
        private String lastLabelId;
        private String lastZpl;

        @Override
        public void print(PrinterConfig printer, String zpl, String labelId) {
            invocationCount++;
            lastLabelId = labelId;
            lastZpl = zpl;
        }
    }
}

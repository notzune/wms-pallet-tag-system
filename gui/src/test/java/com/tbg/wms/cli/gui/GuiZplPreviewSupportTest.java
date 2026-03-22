package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiZplPreviewSupportTest {

    private final GuiZplPreviewSupport support = new GuiZplPreviewSupport();

    @Test
    void buildBarcodeDocumentsReturnsSingleRenderableDocument() {
        List<GuiZplPreviewSupport.PreviewDocument> documents = support.buildBarcodeDocuments(
                new com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest(
                        "TEST-123",
                        com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology.CODE128,
                        com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation.PORTRAIT,
                        812,
                        1218,
                        60,
                        60,
                        3,
                        3,
                        220,
                        true,
                        1
                )
        );

        assertEquals(1, documents.size());
        assertEquals("barcode-TEST-123.zpl", documents.get(0).name());
        assertTrue(documents.get(0).zpl().contains("^XA"));
    }

    @Test
    void buildShipmentDocumentsIncludesSelectedLabelsAndInfoTag() throws Exception {
        LabelWorkflowService.PreparedJob job = preparedJob("8000000001", "LPN-001");

        List<GuiZplPreviewSupport.PreviewDocument> documents = support.buildShipmentDocuments(
                job,
                job.getLpnsForLabels(),
                true
        );

        assertEquals(2, documents.size());
        assertTrue(documents.get(0).name().contains("8000000001"));
        assertTrue(documents.get(0).zpl().contains("^XA"));
        assertEquals("info-shipment-8000000001.zpl", documents.get(1).name());
    }

    @Test
    void buildCarrierMoveDocumentsBuildsStopAndFinalInfoTags() throws Exception {
        LabelWorkflowService.PreparedJob shipmentJob = preparedJob("8000000001", "LPN-001");
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob = PreviewSelectionTestData.carrierMoveJob(
                "241127",
                List.of(PreviewSelectionTestData.stopGroup(10, 1, List.of(shipmentJob)))
        );

        List<GuiZplPreviewSupport.PreviewDocument> documents = support.buildCarrierMoveDocuments(
                carrierMoveJob,
                PrintTaskPlanner.collectAllCarrierMoveLabelSelections(carrierMoveJob),
                true
        );

        assertEquals(3, documents.size());
        assertTrue(documents.get(0).name().contains("8000000001"));
        assertEquals("info-stop-01-of-01.zpl", documents.get(1).name());
        assertEquals("info-final-cmid-241127.zpl", documents.get(2).name());
    }

    private static LabelWorkflowService.PreparedJob preparedJob(String shipmentId, String lpnId) throws Exception {
        Path repoRoot = Path.of("..").toAbsolutePath().normalize();
        Path skuMatrix = repoRoot.resolve("config").resolve("walmart-sku-matrix.csv");
        Path templatePath = repoRoot.resolve("config").resolve("templates").resolve("walmart-canada-label.zpl");
        SkuMappingService skuMapping = new SkuMappingService(skuMatrix);
        LabelTemplate template = new LabelTemplate("WALMART_CANADA", Files.readString(templatePath));
        SiteConfig siteConfig = new SiteConfig("Ship From", "123 Any St", "City, ST 12345");
        Lpn lpn = new Lpn(
                lpnId,
                shipmentId,
                "000000000000000001",
                10,
                120,
                100.0,
                "STAGE",
                null,
                null,
                LocalDate.now(),
                LocalDate.now(),
                List.of(new LineItem(
                        "1",
                        "0",
                        "82592631596000",
                        "NJ SM MANGO PL 0.45L 1Px8C CDN",
                        "NJ SM MANGO PL 0.45L 1Px8C CDN",
                        shipmentId + "-ORDER",
                        null,
                        null,
                        120,
                        8,
                        "EA",
                        0.0,
                        null,
                        null,
                        null
                ))
        );
        Shipment shipment = new Shipment(
                shipmentId,
                shipmentId + "-EXT",
                shipmentId + "-ORDER",
                "3002",
                "WAL-MART CANADA 6098R-REG",
                "123 Store Rd",
                null,
                null,
                "Toronto",
                "ON",
                "A1A1A1",
                "CA",
                null,
                "WALM",
                "TL",
                null,
                null,
                "SPUR08",
                null,
                "6098",
                null,
                null,
                1,
                "241127",
                null,
                null,
                "R",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(lpn)
        );

        Constructor<PalletPlanningService.PlanResult> planCtor =
                PalletPlanningService.PlanResult.class.getDeclaredConstructor(int.class, int.class, int.class, int.class, List.class);
        planCtor.setAccessible(true);
        PalletPlanningService.PlanResult plan = planCtor.newInstance(1, 1, 0, 1, List.of());

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
                shipment,
                null,
                siteConfig,
                skuMapping,
                template,
                Map.<String, ShipmentSkuFootprint>of(),
                plan,
                List.of(lpn),
                List.of(),
                false,
                "STAGE"
        );
    }
}

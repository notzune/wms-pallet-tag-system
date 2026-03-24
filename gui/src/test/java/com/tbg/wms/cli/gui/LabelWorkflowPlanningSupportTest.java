package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.sku.SkuMappingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LabelWorkflowPlanningSupportTest {

    private final LabelWorkflowPlanningSupport support = new LabelWorkflowPlanningSupport();

    @TempDir
    Path tempDir;

    @Test
    void buildSkuMathRows_shouldUseSkuMappingDescriptionFallback() throws Exception {
        Path csv = tempDir.resolve("sku-matrix.csv");
        Files.writeString(csv, String.join("\n",
                "TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU",
                "205641,30081705,1.36L PL 1/6 NJ STRW BAN,1.36L PL 1/6 NJ STRW BAN",
                ""
        ), StandardCharsets.UTF_8);
        SkuMappingService skuMapping = new SkuMappingService(csv);
        List<ShipmentSkuFootprint> rows = List.of(
                new ShipmentSkuFootprint("10048500205641000", "?", 120, 12, 12, 48.0, 40.0, 60.0)
        );

        List<LabelWorkflowService.SkuMathRow> mathRows = support.buildSkuMathRows(rows, skuMapping);

        assertEquals(1, mathRows.size());
        assertEquals("1.36L PL 1/6 NJ STRW BAN", mathRows.get(0).getDescription());
        assertEquals(10, mathRows.get(0).getFullPallets());
    }

    @Test
    void resolveLpnsForLabeling_shouldFallbackToVirtualRowsWhenShipmentHasNoLpns() {
        Shipment shipment = shipment("SHIP1", List.of());
        List<ShipmentSkuFootprint> rows = List.of(
                new ShipmentSkuFootprint("SKU-A", "Desc", 120, 12, 12, 48.0, 40.0, 60.0),
                new ShipmentSkuFootprint("SKU-B", "Desc", 61, 6, 6, 48.0, 40.0, 60.0)
        );

        List<Lpn> resolved = support.resolveLpnsForLabeling(shipment, rows);

        assertFalse(resolved.isEmpty());
        assertEquals(21, resolved.size());
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
}

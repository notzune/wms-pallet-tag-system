package com.tbg.wms.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PalletPlanningServiceTest {

    @Test
    void testPlanComputesEstimatedPalletsFromUnitsPerPallet() {
        PalletPlanningService service = new PalletPlanningService();

        List<ShipmentSkuFootprint> rows = List.of(
                new ShipmentSkuFootprint("SKU-A", 120, 12, 60, 48.0, 40.0, 60.0),
                new ShipmentSkuFootprint("SKU-B", 61, 6, 30, 48.0, 40.0, 60.0)
        );

        PalletPlanningService.PlanResult result = service.plan(rows);

        assertEquals(181, result.getTotalUnits());
        assertEquals(5, result.getEstimatedPallets()); // 120/60=2, 61/30=3
        assertTrue(result.getSkusMissingFootprint().isEmpty());
    }

    @Test
    void testPlanReportsMissingFootprintForSkuWithUnits() {
        PalletPlanningService service = new PalletPlanningService();

        List<ShipmentSkuFootprint> rows = List.of(
                new ShipmentSkuFootprint("SKU-A", 100, 10, null, null, null, null)
        );

        PalletPlanningService.PlanResult result = service.plan(rows);

        assertEquals(100, result.getTotalUnits());
        assertEquals(0, result.getEstimatedPallets());
        assertEquals(List.of("SKU-A"), result.getSkusMissingFootprint());
    }
}

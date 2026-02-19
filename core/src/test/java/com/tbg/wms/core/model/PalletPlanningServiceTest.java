package com.tbg.wms.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PalletPlanningServiceTest {

    @Test
    void testPlanComputesFullAndPartialPallets() {
        List<ShipmentSkuFootprint> rows = List.of(
                new ShipmentSkuFootprint("SKU-A", 120, 12, 60, 48.0, 40.0, 60.0),
                new ShipmentSkuFootprint("SKU-B", 61, 6, 30, 48.0, 40.0, 60.0)
        );

        PalletPlanningService.PlanResult result = new PalletPlanningService().plan(rows);

        assertEquals(181, result.getTotalUnits());
        assertEquals(4, result.getFullPallets());
        assertEquals(1, result.getPartialPallets());
        assertEquals(5, result.getEstimatedPallets());
        assertTrue(result.getSkusMissingFootprint().isEmpty());
    }

    @Test
    void testPlanReportsMissingFootprintForSkuWithUnits() {
        List<ShipmentSkuFootprint> rows = List.of(
                new ShipmentSkuFootprint("SKU-A", 100, 10, null, null, null, null)
        );

        PalletPlanningService.PlanResult result = new PalletPlanningService().plan(rows);

        assertEquals(100, result.getTotalUnits());
        assertEquals(0, result.getFullPallets());
        assertEquals(1, result.getPartialPallets());
        assertEquals(1, result.getEstimatedPallets());
        assertEquals(List.of("SKU-A"), result.getSkusMissingFootprint());
    }
}

/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests pallet planning calculations for full/partial footprint scenarios.
 */
class PalletPlanningServiceTest {

    /**
     * Ensures mixed SKU footprint input yields deterministic full/partial totals.
     */
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

    /**
     * Ensures SKUs with units and no footprint are tracked as missing.
     */
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

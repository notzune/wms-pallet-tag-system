/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests deterministic family-share planning for rail helper labels.
 */
final class RailLabelPlannerTest {

    /**
     * Validates top-family ordering and rounded percentages.
     */
    @Test
    void planComputesDeterministicTopFamilies() {
        RailStopRecord record = new RailStopRecord(
                "03-02-26",
                "142",
                "0124",
                "BR-8000531313",
                "D-2",
                "TPIX3004",
                List.of(
                        new RailStopRecord.ItemQuantity("01832", 2300),
                        new RailStopRecord.ItemQuantity("00818", 1650)
                )
        );
        Map<String, RailFamilyFootprint> footprints = Map.of(
                "01832", new RailFamilyFootprint("01832", "DOM", 100),
                "00818", new RailFamilyFootprint("00818", "CAN", 75)
        );

        List<RailLabelPlanner.PlannedRailLabel> planned = new RailLabelPlanner().plan(List.of(record), footprints);
        RailLabelPlanner.PlannedRailLabel first = planned.get(0);

        assertEquals(1, planned.size());
        assertEquals("DOM:51", first.toMergeFields().get("Item_1"));
        assertEquals("CAN:49", first.toMergeFields().get("Item_2"));
        assertEquals("", first.toMergeFields().get("Item_3"));
        assertTrue(first.getMissingFootprintItems().isEmpty());
    }

    /**
     * Validates planner behavior when footprint lookups are unavailable.
     */
    @Test
    void planTracksMissingFootprintsWithoutCrashing() {
        RailStopRecord record = new RailStopRecord(
                "03-02-26",
                "143",
                "0124",
                "BR-8000531318",
                "D-3",
                "TPIX3010",
                List.of(new RailStopRecord.ItemQuantity("75003", 2860))
        );

        List<RailLabelPlanner.PlannedRailLabel> planned = new RailLabelPlanner().plan(List.of(record), Map.of());
        RailLabelPlanner.PlannedRailLabel first = planned.get(0);

        assertEquals(1, first.getMissingFootprintItems().size());
        assertEquals("75003", first.getMissingFootprintItems().get(0));
        assertEquals("", first.toMergeFields().get("Item_1"));
        assertEquals("", first.toMergeFields().get("Item_2"));
        assertEquals("", first.toMergeFields().get("Item_3"));
    }

    /**
     * Validates that items beyond configured merge slots are surfaced in overflow diagnostics.
     */
    @Test
    void planTracksOverflowBeyondConfiguredItemSlots() {
        List<RailStopRecord.ItemQuantity> items = new ArrayList<>();
        for (int i = 1; i <= 14; i++) {
            String code = String.format("9%04d", i);
            items.add(new RailStopRecord.ItemQuantity(code, 100));
        }

        RailStopRecord record = new RailStopRecord(
                "03-02-26",
                "205",
                "0303",
                "TPIX3032",
                "BR",
                "8000558490",
                items
        );

        Map<String, RailFamilyFootprint> footprints = java.util.stream.IntStream.rangeClosed(1, 14)
                .boxed()
                .collect(java.util.stream.Collectors.toMap(
                        i -> String.format("9%04d", i),
                        i -> new RailFamilyFootprint(String.format("9%04d", i), "DOM", 100)
                ));

        RailLabelPlanner.PlannedRailLabel planned = new RailLabelPlanner(13)
                .plan(List.of(record), footprints)
                .get(0);

        assertEquals(1, planned.getOverflowItems().size());
        assertEquals("90014", planned.getOverflowItems().get(0).getItemNumber());
        assertEquals("90013", planned.toMergeFields().get("ITEM_NBR_13"));
    }
}

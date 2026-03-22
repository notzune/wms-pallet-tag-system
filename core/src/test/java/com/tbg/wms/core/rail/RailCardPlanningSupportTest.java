package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailCardPlanningSupportTest {

    private final RailCardPlanningSupport support = new RailCardPlanningSupport();

    @Test
    void planShouldComputeBucketsFamiliesAndMissingItemsInOnePass() {
        RailCarAggregate aggregate = new RailCarAggregate(
                "03-22-26",
                "142",
                "TRAIN1",
                "CAR1",
                "BR",
                new LinkedHashSet<>(List.of("L1")),
                new LinkedHashMap<>(Map.of(
                        "DOM1", 2300,
                        "CAN1", 1650,
                        "MISS1", 20
                ))
        );
        Map<String, RailFamilyFootprint> footprints = Map.of(
                "DOM1", new RailFamilyFootprint("DOM1", "DOM", 100),
                "CAN1", new RailFamilyFootprint("CAN1", "CAN", 75)
        );

        RailCardPlanningSupport.RailCardPlan plan = support.plan(aggregate, footprints);

        assertEquals(23, plan.domPallets());
        assertEquals(22, plan.canPallets());
        assertEquals(0, plan.kevPallets());
        assertEquals(List.of("DOM:51", "CAN:49"), plan.topFamilies());
        assertEquals(List.of("MISS1"), plan.missingItems());
    }
}

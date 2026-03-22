/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailFamilyShareSupportTest {

    private final RailFamilyShareSupport support = new RailFamilyShareSupport();

    @Test
    void buildSortedSharesOrdersByEquivalentDescendingThenFamilyCode() {
        Map<String, Double> equivalentByFamily = Map.of(
                "CAN", 10.0d,
                "DOM", 10.0d,
                "KEV", 5.0d
        );

        List<RailLabelPlanner.FamilyShare> shares = support.buildSortedShares(equivalentByFamily, 25.0d);

        assertEquals(
                List.of("CAN:40", "DOM:40", "KEV:20"),
                shares.stream()
                        .map(share -> share.getFamilyCode() + ":" + share.getPercent())
                        .collect(Collectors.toList())
        );
    }

    @Test
    void allocatePercentagesUsesLargestRemainderAndSumsToHundred() {
        List<Map.Entry<String, Double>> entries = List.of(
                Map.entry("AAA", 1.0d),
                Map.entry("BBB", 1.0d),
                Map.entry("CCC", 1.0d)
        );

        Map<String, Integer> percents = support.allocatePercentages(entries, 3.0d);

        assertEquals(Map.of("AAA", 34, "BBB", 33, "CCC", 33), percents);
        assertEquals(100, percents.values().stream().reduce(0, Integer::sum));
    }
}

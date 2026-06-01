package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailConsistMarkerSupportTest {

    @Test
    void buildMarkerCountsDistinctDisplayedItemsByFamily() {
        RailConsistMarkerSupport support = new RailConsistMarkerSupport();
        List<RailStopRecord.ItemQuantity> items = List.of(
                new RailStopRecord.ItemQuantity("D1", 10),
                new RailStopRecord.ItemQuantity("D2", 20),
                new RailStopRecord.ItemQuantity("D3", 30),
                new RailStopRecord.ItemQuantity("D4", 40),
                new RailStopRecord.ItemQuantity("C1", 50),
                new RailStopRecord.ItemQuantity("C2", 60),
                new RailStopRecord.ItemQuantity("C3", 70)
        );

        String marker = support.buildMarker(items, Map.of(
                "D1", new RailFamilyFootprint("D1", "DOM", 56),
                "D2", new RailFamilyFootprint("D2", "DOMESTIC", 56),
                "D3", new RailFamilyFootprint("D3", "DOM", 56),
                "D4", new RailFamilyFootprint("D4", "DOM", 56),
                "C1", new RailFamilyFootprint("C1", "CAN", 56),
                "C2", new RailFamilyFootprint("C2", "CANADA", 56),
                "C3", new RailFamilyFootprint("C3", "CAN", 56)
        ));

        assertEquals("D-4 C-3", marker);
    }

    @Test
    void buildMarkerIncludesKevitaAndSkipsUnresolvedItems() {
        RailConsistMarkerSupport support = new RailConsistMarkerSupport();
        List<RailStopRecord.ItemQuantity> items = List.of(
                new RailStopRecord.ItemQuantity("K1", 10),
                new RailStopRecord.ItemQuantity("K2", 20),
                new RailStopRecord.ItemQuantity("MISSING", 30)
        );

        String marker = support.buildMarker(items, Map.of(
                "K1", new RailFamilyFootprint("K1", "KEV", 56),
                "K2", new RailFamilyFootprint("K2", "KEVITA", 56)
        ));

        assertEquals("K-2", marker);
    }
}

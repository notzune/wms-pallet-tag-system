package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailFootprintResolverTest {

    @Test
    void resolveKeepsOnlyCanSplitConsistentShortCodes() {
        Map<String, List<RailFootprintCandidate>> candidates = Map.of(
                "01830", List.of(
                        new RailFootprintCandidate("01830", "ITEMA", "DOM", 56),
                        new RailFootprintCandidate("01830", "ITEMB", "NJ", 60)
                ),
                "01831", List.of(
                        new RailFootprintCandidate("01831", "ITEMC", "CAN", 56),
                        new RailFootprintCandidate("01831", "ITEMD", "DOM", 56)
                ),
                "01832", List.of(
                        new RailFootprintCandidate("01832", "ITEME", "KEV", 48),
                        new RailFootprintCandidate("01832", "ITEMF", "KEV", 52)
                ),
                "01833", List.of(
                        new RailFootprintCandidate("01833", "ITEMG", "DOM", 56),
                        new RailFootprintCandidate("01833", "ITEMH", "NJ", 56)
                )
        );

        Map<String, RailFamilyFootprint> resolved = new RailFootprintResolver().resolve(candidates);

        assertEquals(1, resolved.size());
        assertTrue(resolved.containsKey("01833"));
        assertEquals("DOM", resolved.get("01833").getFamilyCode());
        assertEquals(56, resolved.get("01833").getCasesPerPallet());
    }
}

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
                )
        );

        Map<String, RailFamilyFootprint> resolved = new RailFootprintResolver().resolve(candidates);

        assertEquals(2, resolved.size());
        assertTrue(resolved.containsKey("01830"));
        assertTrue(resolved.containsKey("01832"));
        assertEquals("DOM", resolved.get("01830").getFamilyCode());
        assertEquals(60, resolved.get("01830").getCasesPerPallet());
        assertEquals("KEV", resolved.get("01832").getFamilyCode());
        assertEquals(52, resolved.get("01832").getCasesPerPallet());
    }
}

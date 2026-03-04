package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailFootprintResolverTest {

    @Test
    void resolveKeepsOnlyConsistentShortCodes() {
        Map<String, List<RailFootprintCandidate>> candidates = Map.of(
                "01830", List.of(
                        new RailFootprintCandidate("01830", "ITEMA", "DOM", 56),
                        new RailFootprintCandidate("01830", "ITEMB", "DOM", 56)
                ),
                "01831", List.of(
                        new RailFootprintCandidate("01831", "ITEMC", "CAN", 56),
                        new RailFootprintCandidate("01831", "ITEMD", "DOM", 56)
                )
        );

        Map<String, RailFamilyFootprint> resolved = new RailFootprintResolver().resolve(candidates);

        assertEquals(1, resolved.size());
        assertTrue(resolved.containsKey("01830"));
        assertEquals("DOM", resolved.get("01830").getFamilyCode());
    }
}

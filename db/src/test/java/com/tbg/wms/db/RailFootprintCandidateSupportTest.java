/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.db;

import com.tbg.wms.core.rail.RailFootprintCandidate;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class RailFootprintCandidateSupportTest {

    private final RailFootprintCandidateSupport support = new RailFootprintCandidateSupport();

    @Test
    void normalizeFamilyCodeMapsKnownFamiliesAndFallbacks() {
        assertEquals("CAN", RailFootprintCandidateSupport.normalizeFamilyCode("kev", 1));
        assertEquals("CAN", RailFootprintCandidateSupport.normalizeFamilyCode("cans", 0));
        assertEquals("KEV", RailFootprintCandidateSupport.normalizeFamilyCode("kevita", 0));
        assertEquals("DOM", RailFootprintCandidateSupport.normalizeFamilyCode("domestic", 0));
        assertEquals("DOM", RailFootprintCandidateSupport.normalizeFamilyCode("", null));
        assertEquals("ABC", RailFootprintCandidateSupport.normalizeFamilyCode("abcdef", 0));
    }

    @Test
    void collectCandidatesSkipsInvalidRowsAndSortsByItemNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.wasNull()).thenReturn(false);
        when(rs.getString("SHORT_CODE")).thenReturn("01830", "01830", "");
        when(rs.getString("ITEM_NBR")).thenReturn("ITEMB", "ITEMA", "ITEMC");
        when(rs.getString("PRTFAM")).thenReturn("Domestic", "Domestic", "Domestic");
        when(rs.getInt("UC_PARS_FLG")).thenReturn(0, 0, 0);
        when(rs.getInt("UNITS_PER_PALLET")).thenReturn(70, 56, 42);

        Map<String, List<RailFootprintCandidate>> byShortCode = new LinkedHashMap<>();
        support.collectCandidates(rs, byShortCode);
        support.sortCandidates(byShortCode);

        List<RailFootprintCandidate> candidates = byShortCode.get("01830");
        assertEquals(2, candidates.size());
        assertEquals(List.of("ITEMA", "ITEMB"), itemNumbers(candidates));
    }

    private static List<String> itemNumbers(List<RailFootprintCandidate> candidates) {
        List<String> itemNumbers = new ArrayList<>();
        for (RailFootprintCandidate candidate : candidates) {
            itemNumbers.add(candidate.getItemNumber());
        }
        return itemNumbers;
    }
}

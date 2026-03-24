/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.db;

import com.tbg.wms.core.model.NormalizationService;
import com.tbg.wms.core.rail.RailFootprintCandidate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds and normalizes rail footprint candidates from Oracle result rows.
 */
final class RailFootprintCandidateSupport {

    RailFootprintCandidate toCandidate(ResultSet rs) throws SQLException {
        String shortCode = NormalizationService.normalizeString(rs.getString("SHORT_CODE"));
        String itemNumber = NormalizationService.normalizeSku(rs.getString("ITEM_NBR"));
        String familyCode = normalizeFamilyCode(
                rs.getString("PRTFAM"),
                nullableInt(rs, "UC_PARS_FLG")
        );
        Integer unitsPerPallet = nullableInt(rs, "UNITS_PER_PALLET");
        int casesPerPallet = unitsPerPallet == null ? 0 : unitsPerPallet;
        return new RailFootprintCandidate(shortCode, itemNumber, familyCode, casesPerPallet);
    }

    void collectCandidates(ResultSet rs, Map<String, List<RailFootprintCandidate>> byShortCode) throws SQLException {
        while (rs.next()) {
            RailFootprintCandidate candidate = toCandidate(rs);
            if (!candidate.isValid()) {
                continue;
            }
            byShortCode.computeIfAbsent(candidate.getShortCode(), ignored -> new ArrayList<>()).add(candidate);
        }
    }

    void sortCandidates(Map<String, List<RailFootprintCandidate>> byShortCode) {
        for (List<RailFootprintCandidate> candidates : byShortCode.values()) {
            candidates.sort(Comparator.comparing(RailFootprintCandidate::getItemNumber));
        }
    }

    static String normalizeFamilyCode(String prtfam, Integer parsFlag) {
        String family = NormalizationService.normalizeToUppercase(prtfam);
        if (parsFlag != null && parsFlag == 1) {
            return "CAN";
        }
        if (family.contains("CAN")) {
            return "CAN";
        }
        if (family.contains("KEV")) {
            return "KEV";
        }
        if (family.contains("DOM")) {
            return "DOM";
        }
        if (family.isBlank()) {
            return "DOM";
        }
        if (family.length() > 3) {
            return family.substring(0, 3);
        }
        return family;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}

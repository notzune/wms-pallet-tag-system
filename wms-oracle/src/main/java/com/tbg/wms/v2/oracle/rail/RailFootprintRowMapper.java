package com.tbg.wms.v2.oracle.rail;

import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public final class RailFootprintRowMapper {
    public RailFamilyFootprint map(ResultSet row) throws SQLException {
        return new RailFamilyFootprint(
                row.getString("ITEM_NBR"),
                familyCode(row.getString("PRTFAM"), row.getInt("UC_PARS_FLG")),
                row.getInt("UNITS_PER_PALLET")
        );
    }

    private static String familyCode(String rawFamily, int parsFlag) {
        if (parsFlag == 1) {
            return "CAN";
        }
        String family = rawFamily == null ? "" : rawFamily.trim().toUpperCase(Locale.ROOT);
        if (family.contains("CAN")) {
            return "CAN";
        }
        if (family.contains("KEV")) {
            return "KEV";
        }
        if (family.contains("DOM") || family.isBlank()) {
            return "DOM";
        }
        return family.length() > 3 ? family.substring(0, 3) : family;
    }
}

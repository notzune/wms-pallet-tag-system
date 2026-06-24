package com.tbg.wms.v2.oracle.shipment;

import com.tbg.wms.v2.domain.label.LabelSelectionRef;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Maps shipment label query rows into adapter records.
 */
public final class ShipmentLabelRowMapper {
    /**
     * Maps the current result-set row.
     *
     * @param row result set positioned at the row to map
     * @return mapped shipment label row
     * @throws SQLException if a column cannot be read
     */
    public ShipmentLabelRow map(ResultSet row) throws SQLException {
        return new ShipmentLabelRow(
                upper(row.getString("SHIP_ID")),
                new LabelSelectionRef(upper(row.getString("LABEL_ID")), row.getInt("SOURCE_SEQUENCE"))
        );
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

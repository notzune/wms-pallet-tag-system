package com.tbg.wms.v2.oracle.rail;

import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import com.tbg.wms.v2.domain.rail.RailStopRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class RailStopRowMapper {
    public RailStopRecord map(ResultSet row) throws SQLException {
        return new RailStopRecord(
                row.getString("RUN_DATE"),
                sequence(row),
                row.getString("TRAIN_NBR"),
                row.getString("VEHICLE_ID"),
                row.getString("DCS_WHSE"),
                row.getString("LOAD_NBR"),
                List.of(new RailItemQuantity(row.getString("SHORT_CODE"), row.getInt("TOTAL_CASES")))
        );
    }

    private static String sequence(ResultSet row) throws SQLException {
        int sequence = row.getInt("SEQ");
        return row.wasNull() ? "" : Integer.toString(sequence);
    }
}

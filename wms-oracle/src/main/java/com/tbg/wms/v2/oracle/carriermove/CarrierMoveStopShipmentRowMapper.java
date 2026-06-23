package com.tbg.wms.v2.oracle.carriermove;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class CarrierMoveStopShipmentRowMapper {
    public CarrierMoveStopShipmentRow map(ResultSet row) throws SQLException {
        return new CarrierMoveStopShipmentRow(
                row.getString("CAR_MOVE_ID"),
                row.getString("STOP_ID"),
                row.getInt("STOP_SEQUENCE"),
                row.getString("SHIP_ID")
        );
    }
}

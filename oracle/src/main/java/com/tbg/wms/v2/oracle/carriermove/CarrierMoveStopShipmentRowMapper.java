package com.tbg.wms.v2.oracle.carriermove;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps carrier-move stop shipment query rows into adapter records.
 */
public final class CarrierMoveStopShipmentRowMapper {
    /**
     * Maps the current result-set row.
     *
     * @param row result set positioned at the row to map
     * @return mapped carrier-move stop shipment row
     * @throws SQLException if a column cannot be read
     */
    public CarrierMoveStopShipmentRow map(ResultSet row) throws SQLException {
        return new CarrierMoveStopShipmentRow(
                row.getString("CAR_MOVE_ID"),
                row.getString("STOP_ID"),
                row.getInt("STOP_SEQUENCE"),
                row.getString("SHIP_ID")
        );
    }
}

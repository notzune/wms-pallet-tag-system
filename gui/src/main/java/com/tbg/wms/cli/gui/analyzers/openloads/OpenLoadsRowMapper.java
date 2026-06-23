package com.tbg.wms.cli.gui.analyzers.openloads;

import java.sql.ResultSet;
import java.time.LocalDateTime;

final class OpenLoadsRowMapper {

    OpenLoadsRow read(ResultSet resultSet) throws Exception {
        return map(new OpenLoadsQueryRow(
                resultSet.getString("wh_id"),
                resultSet.getString("car_move_id"),
                resultSet.getString("ordnum"),
                resultSet.getString("ship_id"),
                resultSet.getString("d_l"),
                resultSet.getString("carcod"),
                resultSet.getString("trlr_num"),
                resultSet.getString("yard_loc"),
                resultSet.getString("shpsts"),
                toLocalDateTime(resultSet, "appt"),
                resultSet.getString("dstloc"),
                resultSet.getString("customer"),
                resultSet.getString("carnam"),
                integerValue(resultSet, "casepicks"),
                integerValue(resultSet, "case_picks_comp"),
                integerValue(resultSet, "picks_rem"),
                resultSet.getString("platform"),
                resultSet.getString("shp_dck_flg"),
                resultSet.getString("trlr_cod"),
                resultSet.getString("nottxt"),
                integerValue(resultSet, "staged"),
                integerValue(resultSet, "stop_seq"),
                resultSet.getString("short")
        ));
    }

    OpenLoadsRow map(OpenLoadsQueryRow row) {
        return new OpenLoadsRow(
                row.warehouseId(),
                row.carrierMoveId(),
                row.orderNumber(),
                row.shipmentId(),
                row.dropLive(),
                row.carrierCode(),
                row.trailerNumber(),
                row.yardLocation(),
                row.shipmentStatus(),
                row.appointment(),
                row.destinationLocation(),
                row.customer(),
                row.carrierName(),
                zero(row.casePicks()),
                zero(row.completedCasePicks()),
                zero(row.picksRemaining()),
                row.platform(),
                row.shippingDockFlag(),
                row.trailerCode(),
                row.noteText(),
                zero(row.staged()),
                zero(row.stopSequence()),
                row.shortFlag()
        );
    }

    private static int zero(Integer value) {
        return value == null ? 0 : value;
    }

    private static Integer integerValue(ResultSet resultSet, String column) throws Exception {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime toLocalDateTime(ResultSet resultSet, String column) throws Exception {
        java.sql.Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

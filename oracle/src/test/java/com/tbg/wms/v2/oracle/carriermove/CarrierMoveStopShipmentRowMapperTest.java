package com.tbg.wms.v2.oracle.carriermove;

import org.junit.jupiter.api.Test;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarrierMoveStopShipmentRowMapperTest {
    @Test
    void map_normalizesCarrierMoveStopAndShipmentFields() throws Exception {
        CachedRowSet row = rowSet(" cm-1 ", " stop-9 ", 3, " ship123 ");
        row.next();

        CarrierMoveStopShipmentRow mapped = new CarrierMoveStopShipmentRowMapper().map(row);

        assertEquals("CM-1", mapped.carrierMoveId());
        assertEquals("STOP-9", mapped.stopId());
        assertEquals(3, mapped.stopSequence());
        assertEquals("SHIP123", mapped.shipmentId());
    }

    private static CachedRowSet rowSet(String carrierMoveId, String stopId, int stopSequence, String shipmentId)
            throws Exception {
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount(4);
        metaData.setColumnName(1, "CAR_MOVE_ID");
        metaData.setColumnType(1, Types.VARCHAR);
        metaData.setColumnName(2, "STOP_ID");
        metaData.setColumnType(2, Types.VARCHAR);
        metaData.setColumnName(3, "STOP_SEQUENCE");
        metaData.setColumnType(3, Types.INTEGER);
        metaData.setColumnName(4, "SHIP_ID");
        metaData.setColumnType(4, Types.VARCHAR);
        rowSet.setMetaData(metaData);
        rowSet.moveToInsertRow();
        rowSet.updateString(1, carrierMoveId);
        rowSet.updateString(2, stopId);
        rowSet.updateInt(3, stopSequence);
        rowSet.updateString(4, shipmentId);
        rowSet.insertRow();
        rowSet.moveToCurrentRow();
        rowSet.beforeFirst();
        return rowSet;
    }
}

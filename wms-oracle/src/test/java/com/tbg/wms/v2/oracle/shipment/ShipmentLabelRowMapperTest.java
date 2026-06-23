package com.tbg.wms.v2.oracle.shipment;

import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import org.junit.jupiter.api.Test;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipmentLabelRowMapperTest {
    @Test
    void map_normalizesShipmentAndLabelIdAndKeepsSourceSequence() throws Exception {
        CachedRowSet row = rowSet(
                new Object[]{" ship123 ", " lpn-2 ", 7}
        );
        row.next();

        ShipmentLabelRow mapped = new ShipmentLabelRowMapper().map(row);

        assertEquals("SHIP123", mapped.shipmentId());
        assertEquals(new LabelSelectionRef("LPN-2", 7), mapped.label());
    }

    private static CachedRowSet rowSet(Object[] values) throws Exception {
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount(3);
        metaData.setColumnName(1, "SHIP_ID");
        metaData.setColumnType(1, Types.VARCHAR);
        metaData.setColumnName(2, "LABEL_ID");
        metaData.setColumnType(2, Types.VARCHAR);
        metaData.setColumnName(3, "SOURCE_SEQUENCE");
        metaData.setColumnType(3, Types.INTEGER);
        rowSet.setMetaData(metaData);
        rowSet.moveToInsertRow();
        rowSet.updateString(1, (String) values[0]);
        rowSet.updateString(2, (String) values[1]);
        rowSet.updateInt(3, (Integer) values[2]);
        rowSet.insertRow();
        rowSet.moveToCurrentRow();
        rowSet.beforeFirst();
        return rowSet;
    }
}

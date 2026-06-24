package com.tbg.wms.v2.oracle.shipment;

import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import com.tbg.wms.v2.oracle.JdbcProxySupport;
import org.junit.jupiter.api.Test;

import static com.tbg.wms.v2.oracle.RowSetTestSupport.integer;
import static com.tbg.wms.v2.oracle.RowSetTestSupport.rowSet;
import static com.tbg.wms.v2.oracle.RowSetTestSupport.varchar;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleShipmentRepositoryTest {
    @Test
    void findByShipmentId_loadsOrderedPreparedShipmentLabels() throws Exception {
        JdbcProxySupport.RecordingDatabase database = JdbcProxySupport.database(rowSet(
                new String[]{"SHIP_ID", "LABEL_ID", "SOURCE_SEQUENCE"},
                new int[]{varchar(), varchar(), integer()},
                new Object[]{"SHIP123", "LPN1", 1},
                new Object[]{"SHIP123", "LPN2", 2}
        ));

        PreparedShipmentLabels labels = new OracleShipmentRepository(database.dataSource())
                .findByShipmentId(" ship123 ");

        assertEquals("SHIP123", labels.shipmentId());
        assertEquals(true, labels.includeShipmentInfoTag());
        assertEquals(2, labels.palletLabels().size());
        assertEquals("LPN1", labels.palletLabels().get(0).labelId());
        assertEquals("LPN2", labels.palletLabels().get(1).labelId());
        assertEquals("SHIP123", database.parameters().get(0));
    }
}

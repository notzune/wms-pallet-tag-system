package com.tbg.wms.v2.oracle.carriermove;

import com.tbg.wms.v2.app.ports.ShipmentRepository;
import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import com.tbg.wms.v2.oracle.JdbcProxySupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tbg.wms.v2.oracle.RowSetTestSupport.integer;
import static com.tbg.wms.v2.oracle.RowSetTestSupport.rowSet;
import static com.tbg.wms.v2.oracle.RowSetTestSupport.varchar;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleCarrierMoveRepositoryTest {
    @Test
    void findByCarrierMoveId_groupsShipmentsByStopSequence() throws Exception {
        JdbcProxySupport.RecordingDatabase database = JdbcProxySupport.database(rowSet(
                new String[]{"CAR_MOVE_ID", "STOP_ID", "STOP_SEQUENCE", "SHIP_ID"},
                new int[]{varchar(), varchar(), integer(), varchar()},
                new Object[]{"CM1", "STOP1", 1, "SHIP1"},
                new Object[]{"CM1", "STOP1", 1, "SHIP2"},
                new Object[]{"CM1", "STOP2", 2, "SHIP3"}
        ));
        ShipmentRepository shipments = shipmentId -> PreparedShipmentLabels.of(
                shipmentId,
                List.of(LabelSelectionRef.palletLabel(shipmentId + "-LPN", 1)),
                true
        );

        CarrierMoveLabels labels = new OracleCarrierMoveRepository(database.dataSource(), shipments)
                .findByCarrierMoveId(" cm1 ");

        assertEquals("CM1", labels.carrierMoveId());
        assertEquals(true, labels.includeInfoTags());
        assertEquals(2, labels.stops().size());
        assertEquals(1, labels.stops().get(0).stopPosition());
        assertEquals(1, labels.stops().get(0).stopSequence());
        assertEquals(List.of("SHIP1", "SHIP2"), labels.stops().get(0).shipments().stream()
                .map(PreparedShipmentLabels::shipmentId)
                .toList());
        assertEquals(2, labels.stops().get(1).stopPosition());
        assertEquals("SHIP3", labels.stops().get(1).shipments().get(0).shipmentId());
        assertEquals("CM1", database.parameters().get(0));
    }
}

package com.tbg.wms.v2.oracle.rail;

import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import com.tbg.wms.v2.domain.rail.RailStopRecord;
import com.tbg.wms.v2.oracle.JdbcProxySupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.tbg.wms.v2.oracle.RowSetTestSupport.integer;
import static com.tbg.wms.v2.oracle.RowSetTestSupport.rowSet;
import static com.tbg.wms.v2.oracle.RowSetTestSupport.varchar;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleRailRepositoryTest {
    @Test
    void findStopsByTrainId_groupsItemsByRailStopAndNormalizesTrainParameter() throws Exception {
        JdbcProxySupport.RecordingDatabase database = JdbcProxySupport.database(rowSet(
                new String[]{"RUN_DATE", "SEQ", "TRAIN_NBR", "VEHICLE_ID", "DCS_WHSE", "LOAD_NBR", "SHORT_CODE", "TOTAL_CASES"},
                new int[]{varchar(), integer(), varchar(), varchar(), varchar(), varchar(), varchar(), integer()},
                new Object[]{"03-22-26", 142, "train1", "car1", "br", "load1", "01830", 120},
                new Object[]{"03-22-26", 142, "train1", "car1", "br", "load1", "01831", 25}
        ));

        List<RailStopRecord> rows = new OracleRailRepository(database.dataSource())
                .findStopsByTrainId(" jc04152026 ");

        assertEquals("JC04152026", database.parameters().get(0));
        assertEquals(1, rows.size());
        assertEquals("142", rows.get(0).sequence());
        assertEquals("CAR1", rows.get(0).vehicleId());
        assertEquals(List.of("01830", "01831"), rows.get(0).items().stream()
                .map(RailItemQuantity::itemNumber)
                .toList());
    }

    @Test
    void findFootprintsByShortCode_deduplicatesParametersAndKeysResultsByShortCode() throws Exception {
        JdbcProxySupport.RecordingDatabase database = JdbcProxySupport.database(rowSet(
                new String[]{"SHORT_CODE", "ITEM_NBR", "PRTFAM", "UC_PARS_FLG", "UNITS_PER_PALLET"},
                new int[]{varchar(), varchar(), varchar(), integer(), integer()},
                new Object[]{"01830", "item1", "domestic", 0, 70}
        ));

        Map<String, RailFamilyFootprint> footprints = new OracleRailRepository(database.dataSource())
                .findFootprintsByShortCode(List.of("01830", " ", "01830"));

        assertEquals(List.of("01830"), database.parameters());
        assertEquals("ITEM1", footprints.get("01830").itemNumber());
        assertEquals("DOM", footprints.get("01830").familyCode());
        assertEquals(70, footprints.get("01830").casesPerPallet());
    }
}

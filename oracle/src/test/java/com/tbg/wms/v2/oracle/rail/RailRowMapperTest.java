package com.tbg.wms.v2.oracle.rail;

import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailStopRecord;
import org.junit.jupiter.api.Test;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailRowMapperTest {
    @Test
    void stopMapper_normalizesRailStopAndSingleItemQuantity() throws Exception {
        CachedRowSet row = rowSet(
                new String[]{"RUN_DATE", "SEQ", "TRAIN_NBR", "VEHICLE_ID", "DCS_WHSE", "LOAD_NBR", "SHORT_CODE"},
                new int[]{Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR},
                new Object[]{"03-22-26", 142, " train1 ", " car1 ", " br ", " load1 ", " 01830 "},
                "TOTAL_CASES",
                120
        );
        row.next();

        RailStopRecord mapped = new RailStopRowMapper().map(row);

        assertEquals("03-22-26", mapped.date());
        assertEquals("142", mapped.sequence());
        assertEquals("TRAIN1", mapped.trainNumber());
        assertEquals("CAR1", mapped.vehicleId());
        assertEquals("BR", mapped.warehouse());
        assertEquals("LOAD1", mapped.loadNumber());
        assertEquals("01830", mapped.items().get(0).itemNumber());
        assertEquals(120, mapped.items().get(0).cases());
    }

    @Test
    void footprintMapper_appliesParsCanOverrideAndDefaultsBlankFamilyToDom() throws Exception {
        CachedRowSet canRow = footprintRow("01830", "item1", "KEV", 1, 56);
        canRow.next();
        RailFamilyFootprint can = new RailFootprintRowMapper().map(canRow);

        CachedRowSet domRow = footprintRow("01831", "item2", " ", 0, 70);
        domRow.next();
        RailFamilyFootprint dom = new RailFootprintRowMapper().map(domRow);

        assertEquals("ITEM1", can.itemNumber());
        assertEquals("CAN", can.familyCode());
        assertEquals(56, can.casesPerPallet());
        assertEquals("DOM", dom.familyCode());
    }

    private static CachedRowSet footprintRow(
            String shortCode,
            String itemNumber,
            String family,
            int parsFlag,
            int unitsPerPallet
    ) throws Exception {
        return rowSet(
                new String[]{"SHORT_CODE", "ITEM_NBR", "PRTFAM", "UC_PARS_FLG"},
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER},
                new Object[]{shortCode, itemNumber, family, parsFlag},
                "UNITS_PER_PALLET",
                unitsPerPallet
        );
    }

    private static CachedRowSet rowSet(
            String[] names,
            int[] types,
            Object[] values,
            String finalIntColumn,
            int finalIntValue
    ) throws Exception {
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount(names.length + 1);
        for (int i = 0; i < names.length; i++) {
            metaData.setColumnName(i + 1, names[i]);
            metaData.setColumnType(i + 1, types[i]);
        }
        metaData.setColumnName(names.length + 1, finalIntColumn);
        metaData.setColumnType(names.length + 1, Types.INTEGER);
        rowSet.setMetaData(metaData);
        rowSet.moveToInsertRow();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof Integer integer) {
                rowSet.updateInt(i + 1, integer);
            } else {
                rowSet.updateString(i + 1, (String) value);
            }
        }
        rowSet.updateInt(names.length + 1, finalIntValue);
        rowSet.insertRow();
        rowSet.moveToCurrentRow();
        rowSet.beforeFirst();
        return rowSet;
    }
}

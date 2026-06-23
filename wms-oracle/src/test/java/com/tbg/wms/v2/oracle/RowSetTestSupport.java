package com.tbg.wms.v2.oracle;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.sql.Types;

public final class RowSetTestSupport {
    private RowSetTestSupport() {
    }

    public static CachedRowSet rowSet(String[] names, int[] types, Object[]... rows) throws Exception {
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount(names.length);
        for (int i = 0; i < names.length; i++) {
            metaData.setColumnName(i + 1, names[i]);
            metaData.setColumnType(i + 1, types[i]);
        }
        rowSet.setMetaData(metaData);
        for (Object[] row : rows) {
            rowSet.moveToInsertRow();
            for (int i = 0; i < row.length; i++) {
                Object value = row[i];
                if (value instanceof Integer integer) {
                    rowSet.updateInt(i + 1, integer);
                } else {
                    rowSet.updateString(i + 1, (String) value);
                }
            }
            rowSet.insertRow();
        }
        rowSet.moveToCurrentRow();
        rowSet.beforeFirst();
        return rowSet;
    }

    public static int varchar() {
        return Types.VARCHAR;
    }

    public static int integer() {
        return Types.INTEGER;
    }
}

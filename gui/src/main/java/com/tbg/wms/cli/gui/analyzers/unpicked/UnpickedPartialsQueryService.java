package com.tbg.wms.cli.gui.analyzers.unpicked;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.db.DataSourceFactory;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class UnpickedPartialsQueryService {

    private static final String SQL = """
            SELECT
                cp.wh_id,
                cp.appt,
                cp.ordnum,
                cp.stcust,
                SUM(cp.partial_qty) AS ordqty,
                CASE WHEN SUM(cp.pckqty) IS NULL THEN 0 ELSE SUM(cp.pckqty) END AS alloc_qty,
                CASE
                    WHEN SUM(cp.partial_qty) - SUM(cp.pckqty) IS NULL THEN 0
                    ELSE SUM(cp.partial_qty) - SUM(cp.pckqty)
                END AS unalloc_qty,
                CASE WHEN SUM(cp.appqty) IS NULL THEN 0 ELSE SUM(cp.appqty) END AS comp_qty,
                SUM(cp.partial_qty) - CASE WHEN SUM(cp.appqty) IS NULL THEN 0 ELSE SUM(cp.appqty) END AS remain_qty,
                am.adrnam,
                cusadr.adrnam AS sold_to_name,
                cusadr.adrln1,
                cusadr.adrcty,
                cusadr.adrstc
            FROM
            (
                SELECT
                    ol.wh_id,
                    a.start_dte AS appt,
                    sl.ordnum,
                    o.stcust,
                    ol.ordqty,
                    CASE
                        WHEN MOD(ol.ordqty, pd.untqty) = 0 THEN ol.ordqty
                        ELSE ol.ordqty - MOD(ol.ordqty, pd.untqty)
                    END AS full_qty,
                    CASE
                        WHEN MOD(ol.ordqty, pd.untqty) <> 0 THEN MOD(ol.ordqty, pd.untqty)
                        ELSE 0
                    END AS partial_qty,
                    pv.pckqty,
                    pv.appqty
                FROM
                    ship_struct_view ssv,
                    appt a,
                    shipment_line sl,
                    ord_line ol,
                    ord o,
                    prtftp_dtl pd,
                    dscmst,
                    (
                        SELECT
                            pv2.ordnum,
                            pv2.ordlin,
                            pv2.ordsln,
                            SUM(pv2.pckqty) AS pckqty,
                            SUM(pv2.appqty) AS appqty
                        FROM
                            pckwrk_view pv2
                        WHERE
                            (SUBSTR(pv2.ftpcod, 1, INSTR(pv2.ftpcod, 'X') - 1) <> pv2.pckqty)
                        GROUP BY
                            pv2.ordnum,
                            pv2.ordlin,
                            pv2.ordsln
                    ) pv
                WHERE
                    ssv.appt_id = a.appt_id(+)
                    AND dscmst.colnam = 'shpsts'
                    AND ssv.shpsts = dscmst.colval
                    AND sl.linsts <> 'C'
                    AND ssv.ship_id = sl.ship_id
                    AND ol.ordnum = sl.ordnum
                    AND ol.ordlin = sl.ordlin
                    AND ol.ordsln = sl.ordsln
                    AND ol.ordnum = o.ordnum
                    AND ol.prtnum = pd.prtnum
                    AND pd.uomcod = 'PA'
                    AND ssv.wh_id = pd.wh_id(+)
                    AND ol.ordnum = pv.ordnum(+)
                    AND ol.ordlin = pv.ordlin(+)
                    AND ol.ordsln = pv.ordsln(+)
                    AND CASE
                        WHEN MOD(ol.ordqty, pd.untqty) <> 0 THEN MOD(ol.ordqty, pd.untqty)
                        ELSE 0
                    END > 0
            ) cp,
            wh,
            adrmst am,
            (
                SELECT
                    ord.ordnum,
                    ord.stcust,
                    am2.adrnam,
                    am2.adrln1,
                    am2.adrcty,
                    am2.adrstc
                FROM
                    ord,
                    adrmst am2
                WHERE
                    ord.st_adr_id = am2.adr_id
            ) cusadr
            WHERE
                cp.wh_id = wh.wh_id
                AND wh.adr_id = am.adr_id
                AND cp.appt IS NOT NULL
                AND cp.appt > TRUNC(SYSDATE) - 7
                AND cp.ordnum = cusadr.ordnum(+)
                AND cp.wh_id = ?
            GROUP BY
                cp.wh_id,
                cp.appt,
                cp.ordnum,
                cp.stcust,
                am.adrnam,
                cusadr.adrnam,
                cusadr.adrln1,
                cusadr.adrcty,
                cusadr.adrstc
            HAVING
                SUM(cp.partial_qty) - CASE WHEN SUM(cp.appqty) IS NULL THEN 0 ELSE SUM(cp.appqty) END > 0
            ORDER BY
                2
            """;

    List<QueryRow> fetchRows(AppConfig config) throws Exception {
        DataSource dataSource = new DataSourceFactory(config).create();
        try {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SQL)) {
                statement.setString(1, warehouseId(config.activeSiteCode()));
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<QueryRow> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(new QueryRow(
                                resultSet.getString("wh_id"),
                                toLocalDateTime(resultSet, "appt"),
                                resultSet.getString("ordnum"),
                                resultSet.getString("stcust"),
                                resultSet.getInt("ordqty"),
                                resultSet.getInt("alloc_qty"),
                                resultSet.getInt("unalloc_qty"),
                                resultSet.getInt("comp_qty"),
                                resultSet.getInt("remain_qty"),
                                resultSet.getString("adrnam"),
                                resultSet.getString("sold_to_name"),
                                resultSet.getString("adrln1"),
                                resultSet.getString("adrcty"),
                                resultSet.getString("adrstc")
                        ));
                    }
                    return rows;
                }
            }
        } finally {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                hikariDataSource.close();
            }
        }
    }

    private String warehouseId(String siteCode) {
        return siteCode == null ? "" : siteCode.replaceFirst("^[A-Za-z]+", "");
    }

    private LocalDateTime toLocalDateTime(ResultSet resultSet, String column) throws Exception {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    record QueryRow(
            String warehouseId,
            LocalDateTime appointment,
            String orderNumber,
            String soldToCustomer,
            int orderedQuantity,
            int allocatedQuantity,
            int unallocatedQuantity,
            int completedQuantity,
            int remainingQuantity,
            String warehouseName,
            String soldToName,
            String addressLine1,
            String addressCity,
            String addressState
    ) {
    }
}

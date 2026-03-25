package com.tbg.wms.cli.gui.analyzers.openloads;

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

public final class OpenLoadsQueryService {

    private static final String SQL = """
            select ssv.wh_id,ssv.car_move_id,pickers.ordnum,ssv.ship_id,case when t.live_load_flg = 1 then 'LIVE' else 'DROP' end d_l,ssv.carcod,ssv.trlr_num,ssv.yard_loc,dm.lngdsc shpsts
            ,appt.start_dte appt,shd.dstloc,am.adrnam customer,ch.carnam
            ,pickers.casepicks,pickers.case_picks_comp,case when pickers.casepicks <= 0 then 0 else pickers.casepicks-case_picks_comp end picks_rem
            ,(select max(uc_sug_asset_typ) from ord_line ol where ol.ordnum = pickers.ordnum) platform
            ,a.shp_dck_flg,t.trlr_cod,ano.nottxt
            ,(SELECT COUNT(DISTINCT lodnum) FROM inventory_view,locmst,aremst
            WHERE ship_line_id IN (SELECT ship_line_id FROM shipment_line WHERE ship_id = ssv.ship_id)
            AND inventory_view.stoloc = locmst.stoloc AND inventory_view.wh_id = locmst.wh_id
            AND locmst.arecod = aremst.arecod AND aremst.wh_id = locmst.wh_id
            AND aremst.stgflg = 1 ) staged
            ,ssv.stop_seq,shorts.short
             from ship_struct_view ssv,adrmst am,appt,dscmst dm,carhdr ch,trlr t,wh,ord o,locmst l,aremst a,appt_note ano
             ,shp_dst_loc shd,
            (select ords.ordnum ship_id,case when dem > inv then 'SHORT' end short from
            (select ordnum,sum(ordqty) dem from ord_line group by ordnum) ords,
            (select ship_id,sum(untqty) inv from inventory_view iv,shipment_line sl
            where iv.ship_line_id = sl.ship_line_id group by ship_id) invs
            where ords.ordnum = invs.ship_id(+)
            and ords.dem > invs.inv) shorts,
             (select ordpicks.ordnum,ordpicks.ship_id,sum(picks) casepicks,sum(compicks) case_picks_comp,max(ordpicks.i_hold) i_hold
            from
            (select cp.ordnum,cp.ship_id,nvl(sum(cp.partial_qty),0) picks
            ,nvl(sum(cp.comp_qty),0) compicks
            ,max(inv_hold) i_hold
            from
            (select ol.ordnum,sl.ship_id,ol.ordlin,ol.ordqty
            ,case when mod(ol.ordqty,pd.untqty) <> 0 then mod(ol.ordqty,pd.untqty)else 0 end partial_qty
            ,sum(pv.appqty) comp_qty
            ,max(ol.uc_inv_hld_flg) inv_hold
            from ord_line ol,(select * from pckwrk_view where pckwrk_view.adddte > trunc(sysdate) - 7
            and pckqty <> to_number(substr(ftpcod,1,instr(ftpcod,'X')-1))) pv,prtftp_dtl pd,shipment_line sl,prtmst pm,prtftp
            where
            sl.ordsln = ol.ordsln
            and sl.ordlin = ol.ordlin
            and sl.ordnum = ol.ordnum
            and ol.prtnum = pm.prtnum
            and ol.wh_id = pm.wh_id_tmpl
            and ol.ordnum = pv.ordnum(+)
            and ol.ordlin = pv.ordlin(+)
            and ol.ordsln = pv.ordsln(+)
            and ol.prtnum = pd.prtnum
            and pd.uomcod = 'PA'
            and pd.prtnum = prtftp.prtnum
            and pd.ftpcod = prtftp.ftpcod
            and pd.wh_id = prtftp.wh_id
            and prtftp.defftp_flg = 1
            and ol.wh_id = pd.wh_id
            group by
             ol.ordnum,sl.ship_id,ol.ordlin,ol.ordqty
            ,case when mod(ol.ordqty,pd.untqty) <> 0 then mod(ol.ordqty,pd.untqty)else 0 end
              ) cp
            group by cp.ordnum,cp.ship_id) ordpicks
            group by ordpicks.ordnum,ordpicks.ship_id) pickers
             where o.st_adr_id = am.adr_id
             and ssv.appt_id = appt.appt_id
             and dm.colnam = 'shpsts'
             and ssv.shpsts <> 'C'
             and ssv.ship_id = shd.ship_id (+)
             and dm.colval = ssv.shpsts
             and ch.carcod(+) = ssv.carcod
             and ssv.wh_id = wh.wh_id
             and ssv.ship_id = o.ordnum
             and ssv.trlr_id = t.trlr_id(+)
             and ssv.ship_id = pickers.ship_id(+)
             and ssv.yard_loc = l.stoloc(+)
             and l.arecod = a.arecod(+)
             and appt.appt_id = ano.appt_id(+)
             and ano.notlin(+) = '0'
             and ssv.ship_id = shorts.ship_id(+)
            order by appt,car_move_id,stop_seq
            """;

    List<OpenLoadsRow> fetchRows(AppConfig config) throws Exception {
        DataSource dataSource = new DataSourceFactory(config).create();
        try {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                List<OpenLoadsRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapRow(new QueryRow(
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
                    )));
                }
                return rows;
            }
        } finally {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                hikariDataSource.close();
            }
        }
    }

    OpenLoadsRow mapRow(QueryRow row) {
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

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer integerValue(ResultSet resultSet, String column) throws Exception {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(ResultSet resultSet, String column) throws Exception {
        java.sql.Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    record QueryRow(
            String warehouseId,
            String carrierMoveId,
            String orderNumber,
            String shipmentId,
            String dropLive,
            String carrierCode,
            String trailerNumber,
            String yardLocation,
            String shipmentStatus,
            LocalDateTime appointment,
            String destinationLocation,
            String customer,
            String carrierName,
            Integer casePicks,
            Integer completedCasePicks,
            Integer picksRemaining,
            String platform,
            String shippingDockFlag,
            String trailerCode,
            String noteText,
            Integer staged,
            Integer stopSequence,
            String shortFlag
    ) {
    }
}

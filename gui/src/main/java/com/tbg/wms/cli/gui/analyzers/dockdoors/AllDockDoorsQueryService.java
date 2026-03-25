package com.tbg.wms.cli.gui.analyzers.dockdoors;

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

public final class AllDockDoorsQueryService {

    private static final String SQL = """
            select
                a.door,
                a.d_l,
                a.inb,
                a.trailer_id,
                a.mst_ship_num,
                a.appt_time,
                a.chkin,
                a.move_to_door,
                a.mins_at_door,
                a.shp_perc_cmpl,
                a.appt_over,
                a.stops,
                a.short,
                a.customer,
                a.soldto_nbr,
                a.soldto_match_key,
                a.airbag_flag,
                a.rossi_pals,
                a.rossi_cmpcks
            from (
                select
                    l.stoloc door,
                    case when t.live_load_flg = 0 or t.live_load_flg is null then 'DROP' else 'LIVE' end d_l,
                    case when t.trlr_cod = 'RCV' then 'INB' end inb,
                    t.trlr_num trailer_id,
                    c.car_move_id mst_ship_num,
                    ap.start_dte appt_time,
                    t.arrdte chkin,
                    (
                        select min(ta.trndte)
                        from trlract ta
                        where ta.trlr_id = t.trlr_id
                          and ta.yard_loc = t.yard_loc
                          and ta.trndte >= t.arrdte
                    ) move_to_door,
                    (
                        sysdate - (
                            select min(ta.trndte)
                            from trlract ta
                            where ta.trlr_id = t.trlr_id
                              and ta.yard_loc = t.yard_loc
                              and ta.trndte >= t.arrdte
                        )
                    ) * 60 * 24 mins_at_door,
                    percomp.shp_perc_cmpl,
                    (sysdate - ap.start_dte) * 24 appt_over,
                    (
                        case
                            when (
                                select count(distinct stop_id)
                                from ship_struct_view ssv
                                where ssv.car_move_id = c.car_move_id
                            ) = 0 then null
                            else (
                                select count(distinct ship_id)
                                from ship_struct_view ssv
                                where ssv.car_move_id = c.car_move_id
                            )
                        end
                    ) stops,
                    (
                        select distinct 'SHORT'
                        from rplwrk r,
                             ship_struct_view ssv
                        where ssv.car_move_id = c.car_move_id
                          and ssv.ship_id = r.ordnum
                    ) short,
                    adrnam customer,
                    percomp.soldto_nbr,
                    upper(nvl(adrnam, 'UNKNOWN')) || '|' || trim(percomp.soldto_nbr) soldto_match_key,
                    case
                        when trim(percomp.soldto_nbr) = '100003434' then 'AIRBAG'
                        else null
                    end airbag_flag,
                    (
                        select sum(palqty)
                        from (
                            select
                                car_move_id,
                                p.prtnum,
                                sum(ol.ordqty) / substr(pf.ftpcod, 1, instr(pf.ftpcod, 'X') - 1) palqty
                            from ord_line ol, shipment_line sl, ship_struct_view ssv, prtmst p, prtftp pf, wh
                            where p.prtfam in ('DOMCLUB', 'CANADIAN', 'GLASS')
                              and pf.prtnum = p.prtnum
                              and pf.wh_id = wh.wh_id
                              and pf.defftp_flg = 1
                              and ol.ordnum = sl.ordnum
                              and ol.ordlin = sl.ordlin
                              and ol.ordsln = sl.ordsln
                              and sl.ship_id = ssv.ship_id
                            group by ssv.car_move_id, p.prtnum, pf.ftpcod
                        ) a2
                        where car_move_id = c.car_move_id
                        group by car_move_id
                    ) rossi_pals,
                    (
                        select nvl(sum(pv.appqty / substr(pv.ftpcod, 1, instr(pv.ftpcod, 'X') - 1)), 0)
                        from pckwrk_view pv,
                             aremst ar2,
                             ship_struct_view ssv,
                             shipment_line sl
                        where pv.srcare = ar2.arecod
                          and ar2.bldg_id = 'ROSSI'
                          and pv.ship_line_id = sl.ship_line_id
                          and sl.ship_id = ssv.ship_id
                          and ssv.car_move_id = c.car_move_id
                    ) rossi_cmpcks
                from locmst l,
                     aremst ar,
                     trlr t,
                     car_move c,
                     appt ap,
                     (
                         select
                             ssv.car_move_id,
                             loaded / sum(ordqty) * 100 shp_perc_cmpl,
                             max(adrnam) adrnam,
                             max(stcust) soldto_nbr
                         from (
                             select
                                 ol.wh_id,
                                 am.adrnam,
                                 o.stcust,
                                 sl.ordnum,
                                 ol.ordqty
                             from ship_struct_view ssv,
                                  shipment_line sl,
                                  ord_line ol,
                                  ord o,
                                  cstmst c2,
                                  adrmst am
                             where ssv.ship_id = sl.ship_id
                               and ol.ordnum = sl.ordnum
                               and ol.ordlin = sl.ordlin
                               and ol.ordsln = sl.ordsln
                               and ol.ordnum = o.ordnum
                               and o.stcust = c2.cstnum(+)
                               and c2.adr_id = am.adr_id(+)
                         ) ordstuff,
                         ship_struct_view ssv,
                         (
                             select car_move_id, sum(loaded) loaded
                             from (
                                 select car_move_id,
                                     (case when iv.stoloc like 'TRL%' then sum(untqty) end) loaded
                                 from inventory_view iv,
                                      shipment_line sl,
                                      ship_struct_view ssv
                                 where iv.ship_line_id = sl.ship_line_id
                                   and sl.ship_id = ssv.ship_id
                                 group by car_move_id, iv.stoloc
                             )
                             group by car_move_id
                         ) loaded_pals
                         where ssv.ship_id = ordstuff.ordnum
                           and ssv.car_move_id = loaded_pals.car_move_id(+)
                         group by ssv.car_move_id, loaded_pals.loaded
                     ) percomp
                where l.arecod = ar.arecod
                  and ar.shp_dck_flg = 1
                  and l.stoloc not like 'V%'
                  and l.stoloc not like 'TRL%'
                  and l.stoloc = t.yard_loc(+)
                  and t.trlr_id = c.trlr_id(+)
                  and c.car_move_id = ap.car_move_id(+)
                  and c.car_move_id = percomp.car_move_id(+)
            ) a
            order by 1
            """;

    List<AllDockDoorsRow> fetchRows(AppConfig config) throws Exception {
        DataSource dataSource = new DataSourceFactory(config).create();
        try {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                List<AllDockDoorsRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapRow(new QueryRow(
                            resultSet.getString("door"),
                            resultSet.getString("d_l"),
                            resultSet.getString("inb"),
                            resultSet.getString("trailer_id"),
                            resultSet.getString("mst_ship_num"),
                            toLocalDateTime(resultSet, "appt_time"),
                            toLocalDateTime(resultSet, "chkin"),
                            toLocalDateTime(resultSet, "move_to_door"),
                            integerValue(resultSet, "mins_at_door"),
                            doubleValue(resultSet, "shp_perc_cmpl"),
                            doubleValue(resultSet, "appt_over"),
                            doubleValue(resultSet, "stops"),
                            resultSet.getString("short"),
                            resultSet.getString("customer"),
                            resultSet.getString("soldto_nbr"),
                            resultSet.getString("soldto_match_key"),
                            resultSet.getString("airbag_flag"),
                            doubleValue(resultSet, "rossi_pals"),
                            doubleValue(resultSet, "rossi_cmpcks")
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

    AllDockDoorsRow mapRow(QueryRow row) {
        return new AllDockDoorsRow(
                row.door(),
                row.dropLive(),
                row.inboundFlag(),
                row.trailerId(),
                row.moveNumber(),
                row.appointmentTime(),
                row.checkInTime(),
                row.movedToDoorAt(),
                zero(row.minutesAtDoor()),
                zero(row.shipmentPercentComplete()),
                zero(row.appointmentOverHours()),
                zero(row.stops()),
                row.shortFlag(),
                row.customer(),
                row.soldToNumber(),
                row.soldToMatchKey(),
                row.airbagFlag(),
                zero(row.rossiPallets()),
                zero(row.rossiCompletedPicks())
        );
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    private double zero(Double value) {
        return value == null ? 0 : value;
    }

    private Integer integerValue(ResultSet resultSet, String column) throws Exception {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Double doubleValue(ResultSet resultSet, String column) throws Exception {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(ResultSet resultSet, String column) throws Exception {
        java.sql.Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    record QueryRow(
            String door,
            String dropLive,
            String inboundFlag,
            String trailerId,
            String moveNumber,
            LocalDateTime appointmentTime,
            LocalDateTime checkInTime,
            LocalDateTime movedToDoorAt,
            Integer minutesAtDoor,
            Double shipmentPercentComplete,
            Double appointmentOverHours,
            Double stops,
            String shortFlag,
            String customer,
            String soldToNumber,
            String soldToMatchKey,
            String airbagFlag,
            Double rossiPallets,
            Double rossiCompletedPicks
    ) {
    }
}

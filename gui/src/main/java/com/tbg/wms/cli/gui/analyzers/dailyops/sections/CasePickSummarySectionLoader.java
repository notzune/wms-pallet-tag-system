package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.dailyops.DailyOperationsSectionLoader;
import com.tbg.wms.cli.gui.analyzers.dashboard.AnalyzerDashboardSectionSnapshot;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.db.DataSourceFactory;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CasePickSummarySectionLoader implements DailyOperationsSectionLoader {

    private final QueryService queryService;

    public CasePickSummarySectionLoader() {
        this(new QueryService());
    }

    CasePickSummarySectionLoader(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
    }

    @Override
    public String title() {
        return "Case Pick Summary";
    }

    @Override
    public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) throws Exception {
        List<CasePickSummaryRow> rows = queryService.fetchRows(context.config()).stream().map(this::mapRow).toList();
        return AnalyzerDashboardSectionSnapshot.success(title(), buildTable(rows));
    }

    CasePickSummaryRow mapRow(QueryRow row) {
        return new CasePickSummaryRow(
                row.casePicks(),
                zero(row.picks()),
                zero(row.remaining()),
                zero(row.domesticTotal()),
                zero(row.domesticRemaining()),
                zero(row.canadianTotal()),
                zero(row.canadianRemaining())
        );
    }

    private JTable buildTable(List<CasePickSummaryRow> rows) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Date", "Picks", "Remaining", "Domestic", "Domestic Remaining", "Canadian", "Canadian Remaining"},
                0
        );
        for (CasePickSummaryRow row : rows) {
            model.addRow(new Object[]{
                    row.casePicks(),
                    row.picks(),
                    row.remaining(),
                    row.domesticTotal(),
                    row.domesticRemaining(),
                    row.canadianTotal(),
                    row.canadianRemaining()
            });
        }
        return new JTable(model);
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    static final class QueryService {
        private static final String SQL = """
                select
                    casepicks,
                    sum(picks) as picks,
                    sum(remaining) as remaining,
                    sum(case when is_can = 0 then picks else 0 end) as domestic_total,
                    sum(case when is_can = 0 then remaining else 0 end) as domestic_remaining,
                    sum(case when is_can = 1 then picks else 0 end) as canadian_total,
                    sum(case when is_can = 1 then remaining else 0 end) as canadian_remaining
                from (
                    select
                        toms.dater as casepicks,
                        nvl(f.family, 'ALL') as family,
                        nvl(f.is_can, 0) as is_can,
                        nvl(f.picks, 0) as picks,
                        nvl(f.remaining, 0) as remaining
                    from (
                        select
                            trunc(appt) as casepicks,
                            prtfam as family,
                            is_can,
                            sum(ordqty) as picks,
                            sum(remain_qty) as remaining
                        from (
                            select
                                cp.wh_id,
                                cp.appt,
                                cp.ordnum,
                                cp.prtfam,
                                cp.is_can,
                                sum(cp.partial_qty) as ordqty,
                                case when sum(cp.pckqty) is null then 0 else sum(cp.pckqty) end as alloc_qty,
                                case when sum(cp.partial_qty)-sum(cp.pckqty) is null then 0 else sum(cp.partial_qty)-sum(cp.pckqty) end as unalloc_qty,
                                case when sum(cp.appqty) is null then 0 else sum(cp.appqty) end as comp_qty,
                                case
                                    when (sum(cp.partial_qty)-sum(cp.appqty) is null) or (sum(cp.partial_qty)-sum(cp.appqty) < 0)
                                    then 0
                                    else sum(cp.partial_qty)-sum(cp.appqty)
                                end as remain_qty
                            from (
                                select
                                    ol.wh_id,
                                    a.start_dte as appt,
                                    sl.ordnum,
                                    ol.ordqty,
                                    case when mod(ol.ordqty,pd.untqty)=0 then ol.ordqty else ol.ordqty-mod(ol.ordqty,pd.untqty) end as full_qty,
                                    case when mod(ol.ordqty,pd.untqty)<>0 then mod(ol.ordqty,pd.untqty) else 0 end as partial_qty,
                                    nvl(pv.pckqty,0) as pckqty,
                                    nvl(pv.appqty,0) as appqty,
                                    prtmst.prtfam,
                                    case
                                        when upper(prtmst.prtfam) = 'CANADIAN' or nvl(prtmst.uc_pars_flg,0) = 1 then 1
                                        else 0
                                    end as is_can
                                from ship_struct_view ssv,
                                     appt a,
                                     shipment_line sl,
                                     ord_line ol,
                                     ord o,
                                     prtmst,
                                     prtftp_dtl pd,
                                     dscmst,
                                     (
                                         select
                                             pv2.ordnum,
                                             pv2.ordlin,
                                             pv2.ordsln,
                                             sum(pv2.pckqty) as pckqty,
                                             sum(pv2.appqty) as appqty
                                         from pckwrk_view pv2
                                         where (
                                             case
                                                 when instr(nvl(pv2.ftpcod,''),'X') > 0 then substr(pv2.ftpcod,1,instr(pv2.ftpcod,'X')-1)
                                                 else nvl(pv2.ftpcod,'')
                                             end
                                         ) <> to_char(nvl(pv2.pckqty,0))
                                         group by pv2.ordnum, pv2.ordlin, pv2.ordsln
                                     ) pv
                                where ssv.appt_id = a.appt_id(+)
                                  and a.start_dte > trunc(sysdate)-1
                                  and dscmst.colnam = 'shpsts'
                                  and ssv.shpsts = dscmst.colval
                                  and ssv.ship_id = sl.ship_id
                                  and ol.ordnum = sl.ordnum
                                  and ol.ordlin = sl.ordlin
                                  and ol.ordsln = sl.ordsln
                                  and ol.ordnum = o.ordnum
                                  and ol.prtnum = pd.prtnum
                                  and ol.prtnum = prtmst.prtnum
                                  and ol.wh_id = prtmst.wh_id_tmpl
                                  and pd.uomcod = 'PA'
                                  and ssv.wh_id = pd.wh_id(+)
                                  and ol.ordnum = pv.ordnum(+)
                                  and ol.ordlin = pv.ordlin(+)
                                  and ol.ordsln = pv.ordsln(+)
                                  and case when mod(ol.ordqty,pd.untqty)<>0 then mod(ol.ordqty,pd.untqty) else 0 end > 0
                            ) cp
                            group by cp.wh_id, cp.appt, cp.ordnum, cp.prtfam, cp.is_can
                        ) pickme
                        group by trunc(appt), prtfam, is_can
                    ) f,
                    (
                        select trunc(sysdate)-1 as dater from dual
                        union all select trunc(sysdate) from dual
                        union all select trunc(sysdate)+1 from dual
                        union all select trunc(sysdate)+2 from dual
                        union all select trunc(sysdate)+3 from dual
                        union all select trunc(sysdate)+4 from dual
                        union all select trunc(sysdate)+5 from dual
                    ) toms
                    where toms.dater = f.casepicks(+)
                )
                group by casepicks
                order by 1
                """;

        List<QueryRow> fetchRows(AppConfig config) throws Exception {
            DataSource dataSource = new DataSourceFactory(config).create();
            try {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(SQL);
                     ResultSet resultSet = statement.executeQuery()) {
                    List<QueryRow> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(new QueryRow(
                                toLocalDate(resultSet, "casepicks"),
                                integerValue(resultSet, "picks"),
                                integerValue(resultSet, "remaining"),
                                integerValue(resultSet, "domestic_total"),
                                integerValue(resultSet, "domestic_remaining"),
                                integerValue(resultSet, "canadian_total"),
                                integerValue(resultSet, "canadian_remaining")
                        ));
                    }
                    return rows;
                }
            } finally {
                if (dataSource instanceof HikariDataSource hikariDataSource) {
                    hikariDataSource.close();
                }
            }
        }

        private Integer integerValue(ResultSet resultSet, String column) throws Exception {
            int value = resultSet.getInt(column);
            return resultSet.wasNull() ? null : value;
        }

        private LocalDate toLocalDate(ResultSet resultSet, String column) throws Exception {
            java.sql.Timestamp timestamp = resultSet.getTimestamp(column);
            return timestamp == null ? null : timestamp.toLocalDateTime().toLocalDate();
        }
    }

    static record QueryRow(
            LocalDate casePicks,
            Integer picks,
            Integer remaining,
            Integer domesticTotal,
            Integer domesticRemaining,
            Integer canadianTotal,
            Integer canadianRemaining
    ) {
    }
}

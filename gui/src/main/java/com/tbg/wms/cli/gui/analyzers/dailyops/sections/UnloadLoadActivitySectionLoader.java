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

public final class UnloadLoadActivitySectionLoader implements DailyOperationsSectionLoader {

    private final QueryService queryService;

    public UnloadLoadActivitySectionLoader() {
        this(new QueryService());
    }

    UnloadLoadActivitySectionLoader(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
    }

    @Override
    public String title() {
        return "Unload and Load Activity";
    }

    @Override
    public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) throws Exception {
        List<UnloadLoadActivityRow> rows = new ArrayList<>();
        rows.addAll(mapMetricRows("Unloads", queryService.fetchUnloads(context.config())));
        rows.addAll(mapMetricRows("Rail Unloads", queryService.fetchRailUnloads(context.config())));
        rows.addAll(mapMetricRows("Rail Loads", queryService.fetchRailLoads(context.config())));
        rows.addAll(mapMetricRows("Truck Loads", queryService.fetchTruckLoads(context.config())));
        return AnalyzerDashboardSectionSnapshot.success(title(), buildTable(rows));
    }

    List<UnloadLoadActivityRow> mapMetricRows(String metric, List<QueryRow> rows) {
        return rows.stream().map(row -> mapMetricRow(metric, row)).toList();
    }

    UnloadLoadActivityRow mapMetricRow(String metric, QueryRow row) {
        return new UnloadLoadActivityRow(
                metric,
                row.activityDate(),
                zero(row.thirdA()),
                zero(row.first()),
                zero(row.second()),
                zero(row.thirdB())
        );
    }

    private JTable buildTable(List<UnloadLoadActivityRow> rows) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Metric", "Date", "3A", "1st", "2nd", "3B"},
                0
        );
        for (UnloadLoadActivityRow row : rows) {
            model.addRow(new Object[]{row.metric(), row.activityDate(), row.thirdA(), row.first(), row.second(), row.thirdB()});
        }
        return new JTable(model);
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    static final class QueryService {
        private static final String UNLOADS_SQL = """
                select distinct ul.unloads,ul.thirda,ul.first,ul.second,ul.thirdb
                from
                (select trunc(clsdte) unloads
                ,count(case when to_char(clsdte,'hh24mi') < '0600' then 1 end) thirda
                ,count(case when to_char(clsdte,'hh24mi') >= '0600'
                and to_char(clsdte,'hh24mi') < '1400' then 1 end) first
                ,count(case when to_char(clsdte,'hh24mi') >= '1400'
                and to_char(clsdte,'hh24mi') < '2200' then 1 end) second
                ,count(case when to_char(clsdte,'hh24mi') >= '2200' then 1 end) thirdb
                 from rcvtrk,rcvinv,trlr t
                where clsdte is not null
                and rcvinv.trknum = rcvtrk.trknum
                and rcvtrk.trlr_id = t.trlr_id
                and t.carcod not in ('CSXT','FEC')
                and clsdte > trunc(sysdate) -1
                group by trunc(clsdte)) ul,
                (select trunc(sysdate)-1 dater from dual
                union all select trunc(sysdate) from dual) tom
                where tom.dater = ul.unloads(+)
                order by 1
                """;

        private static final String RAIL_UNLOADS_SQL = """
                select distinct ul.unloads,ul.thirda,ul.first,ul.second,ul.thirdb
                from
                (select trunc(clsdte) unloads
                ,count(case when to_char(clsdte,'hh24mi') < '0600' then 1 end) thirda
                ,count(case when to_char(clsdte,'hh24mi') >= '0600'
                and to_char(clsdte,'hh24mi') < '1400' then 1 end) first
                ,count(case when to_char(clsdte,'hh24mi') >= '1400'
                and to_char(clsdte,'hh24mi') < '2200' then 1 end) second
                ,count(case when to_char(clsdte,'hh24mi') >= '2200' then 1 end) thirdb
                from rcvtrk,rcvinv,trlr t
                where clsdte is not null
                and rcvinv.invtyp = 'STO'
                and rcvinv.trknum = rcvtrk.trknum
                and rcvtrk.trlr_id = t.trlr_id
                and t.carcod in ('CSXT','FEC')
                and clsdte > trunc(sysdate) -1
                group by trunc(clsdte)) ul,
                (select trunc(sysdate)-1 dater from dual
                union all select trunc(sysdate) from dual) tom
                where tom.dater = ul.unloads(+)
                order by 1
                """;

        private static final String RAIL_LOADS_SQL = """
                select l.railloads,l.thirda,l.first,l.second,l.thirdb
                from
                (select trunc(loddte) railloads
                ,count(case when to_char(loddte,'hh24mi') < '0600' then 1 end) thirda
                ,count(case when to_char(loddte,'hh24mi') >= '0600'
                and to_char(loddte,'hh24mi') < '1400' then 1 end) first
                ,count(case when to_char(loddte,'hh24mi') >= '1400'
                and to_char(loddte,'hh24mi') < '2200' then 1 end) second
                ,count(case when to_char(loddte,'hh24mi') >= '2200' then 1 end) thirdb
                 from ship_struct_view
                where loddte is not null
                and carcod in ('CSXT','FEC')
                and loddte > trunc(sysdate) - 1
                group by trunc(loddte)) l,
                (select trunc(sysdate)-1 dater from dual
                union all select trunc(sysdate) from dual) tom
                where tom.dater = l.railloads(+)
                order by 1
                """;

        private static final String TRUCK_LOADS_SQL = """
                select l.truckloads,l.thirda,l.first,l.second,l.thirdb
                from
                (select trunc(loddte) truckloads
                ,count(case when to_char(loddte,'hh24mi') < '0600' then 1 end) thirda
                ,count(case when to_char(loddte,'hh24mi') >= '0600'
                and to_char(loddte,'hh24mi') < '1400' then 1 end) first
                ,count(case when to_char(loddte,'hh24mi') >= '1400'
                and to_char(loddte,'hh24mi') < '2200' then 1 end) second
                ,count(case when to_char(loddte,'hh24mi') >= '2200' then 1 end) thirdb
                 from (select car_move_id,max(loddte) loddte from ship_struct_view
                where loddte is not null
                and carcod not in ('CSXT','FEC')
                and loddte > trunc(sysdate) - 1 group by car_move_id)
                group by trunc(loddte)) l,
                (select trunc(sysdate)-1 dater from dual
                union all select trunc(sysdate) from dual) tom
                where tom.dater = l.truckloads(+)
                order by 1
                """;

        List<QueryRow> fetchUnloads(AppConfig config) throws Exception {
            return fetchRows(config, UNLOADS_SQL, "unloads");
        }

        List<QueryRow> fetchRailUnloads(AppConfig config) throws Exception {
            return fetchRows(config, RAIL_UNLOADS_SQL, "unloads");
        }

        List<QueryRow> fetchRailLoads(AppConfig config) throws Exception {
            return fetchRows(config, RAIL_LOADS_SQL, "railloads");
        }

        List<QueryRow> fetchTruckLoads(AppConfig config) throws Exception {
            return fetchRows(config, TRUCK_LOADS_SQL, "truckloads");
        }

        private List<QueryRow> fetchRows(AppConfig config, String sql, String dateColumn) throws Exception {
            DataSource dataSource = new DataSourceFactory(config).create();
            try {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    List<QueryRow> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(new QueryRow(
                                toLocalDate(resultSet, dateColumn),
                                integerValue(resultSet, "thirda"),
                                integerValue(resultSet, "first"),
                                integerValue(resultSet, "second"),
                                integerValue(resultSet, "thirdb")
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
            LocalDate activityDate,
            Integer thirdA,
            Integer first,
            Integer second,
            Integer thirdB
    ) {
    }
}

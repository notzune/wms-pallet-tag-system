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

public final class CasePickShiftThroughputSectionLoader implements DailyOperationsSectionLoader {

    private final QueryService queryService;

    public CasePickShiftThroughputSectionLoader() {
        this(new QueryService());
    }

    CasePickShiftThroughputSectionLoader(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
    }

    @Override
    public String title() {
        return "Case Pick Shift Throughput";
    }

    @Override
    public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) throws Exception {
        List<CasePickShiftThroughputRow> rows = queryService.fetchRows(context.config()).stream().map(this::mapRow).toList();
        return AnalyzerDashboardSectionSnapshot.success(title(), buildTable(rows));
    }

    CasePickShiftThroughputRow mapRow(QueryRow row) {
        return new CasePickShiftThroughputRow(
                row.casePicks(),
                zero(row.thirdA()),
                zero(row.first()),
                zero(row.second()),
                zero(row.thirdB())
        );
    }

    private JTable buildTable(List<CasePickShiftThroughputRow> rows) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Date", "3A", "1st", "2nd", "3B"},
                0
        );
        for (CasePickShiftThroughputRow row : rows) {
            model.addRow(new Object[]{row.casePicks(), row.thirdA(), row.first(), row.second(), row.thirdB()});
        }
        return new JTable(model);
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    static final class QueryService {
        private static final String SQL = """
                select p.casepicks,p.thirda,p.first,p.second,p.thirdb
                from
                (select trunc(trndte) casepicks
                ,sum(case when to_char(trndte,'hh24mi') < '0600' then trnqty end) thirda
                ,sum(case when to_char(trndte,'hh24mi') >= '0600'
                and to_char(trndte,'hh24mi') < '1400' then trnqty end) first
                ,sum(case when to_char(trndte,'hh24mi') >= '1400'
                and to_char(trndte,'hh24mi') < '2200' then trnqty end) second
                ,sum(case when to_char(trndte,'hh24mi') >= '2200' then trnqty end) thirdb
                 from dlytrn
                where actcod = 'CASPCK'
                and to_arecod like 'RDT%'
                and fr_arecod not like 'MATERIAL'
                and fr_arecod not like 'PKGMAT'
                and trunc(trndte) >= trunc(sysdate) - 7
                group by trunc(trndte)) p
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
            LocalDate casePicks,
            Integer thirdA,
            Integer first,
            Integer second,
            Integer thirdB
    ) {
    }
}

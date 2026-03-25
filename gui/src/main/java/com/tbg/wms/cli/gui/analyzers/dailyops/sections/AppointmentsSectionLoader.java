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

public final class AppointmentsSectionLoader implements DailyOperationsSectionLoader {

    private final QueryService queryService;

    public AppointmentsSectionLoader() {
        this(new QueryService());
    }

    AppointmentsSectionLoader(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
    }

    @Override
    public String title() {
        return "Outbound / Inbound Appointments";
    }

    @Override
    public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) throws Exception {
        List<AppointmentsRow> rows = queryService.fetchRows(context.config()).stream().map(this::mapRow).toList();
        return AnalyzerDashboardSectionSnapshot.success(title(), buildTable(rows));
    }

    AppointmentsRow mapRow(QueryRow row) {
        int outbounds = zero(row.outbounds());
        int completed = zero(row.completed());
        int inbounds = zero(row.inbounds());
        int inboundCompleted = zero(row.inboundCompleted());
        return new AppointmentsRow(
                row.appointmentDay(),
                zero(row.trucks()),
                outbounds,
                completed,
                Math.max(0, outbounds - completed),
                inbounds,
                inboundCompleted,
                Math.max(0, inbounds - inboundCompleted)
        );
    }

    private JTable buildTable(List<AppointmentsRow> rows) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Date", "Trucks", "Outbounds", "Completed", "Out Remaining", "Inbounds", "In Completed", "In Remaining"},
                0
        );
        for (AppointmentsRow row : rows) {
            model.addRow(new Object[]{
                    row.appointmentDay(),
                    row.trucks(),
                    row.outbounds(),
                    row.completed(),
                    row.outRemaining(),
                    row.inbounds(),
                    row.inboundCompleted(),
                    row.inboundRemaining()
            });
        }
        return new JTable(model);
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    static final class QueryService {
        private static final String SQL = """
                select a.apptday trucks,count(distinct a.car_move_id) outbounds
                ,count(distinct loddte) completed
                ,count(distinct trknum) inbounds
                ,count(distinct clsdte) inb_completed
                from
                (
                select trunc(start_dte) apptday,appt.car_move_id, am.trknum,r.clsdte
                ,ssv.loddte
                from appt,appt_mstrcpt am,(select car_move_id,carcod,max(loddte) loddte from ship_struct_view
                where loddte is not null and loddte > trunc(sysdate) - 7 group by car_move_id,carcod) ssv,rcvtrk r
                where appt.appt_id = am.appt_id(+)
                and appt.car_move_id = ssv.car_move_id(+)
                and ssv.carcod(+) <> 'CSXT'
                and ssv.carcod(+) <> 'FEC'
                and appt.trlr_cod = case when appt.car_move_id is not null then 'SHIP' else 'RCV' end
                and am.trknum = r.trknum(+)
                and appt.start_dte >= trunc(sysdate) - 1
                and appt.start_dte < trunc(sysdate) + 5
                and appt.carcod not in ('CSXT','FEC')) a
                ,
                (select trunc(sysdate)-1 dater from dual
                union all select trunc(sysdate) from dual
                union all select trunc(sysdate) + 1 from dual
                union all select trunc(sysdate) + 2 from dual
                union all select trunc(sysdate) + 3 from dual
                union all select trunc(sysdate) + 4 from dual) toms
                where toms.dater = a.apptday(+)
                group by a.apptday
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
                                toLocalDate(resultSet, "trucks"),
                                integerValue(resultSet, "trucks"),
                                integerValue(resultSet, "outbounds"),
                                integerValue(resultSet, "completed"),
                                integerValue(resultSet, "inbounds"),
                                integerValue(resultSet, "inb_completed")
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
            LocalDate appointmentDay,
            Integer trucks,
            Integer outbounds,
            Integer completed,
            Integer inbounds,
            Integer inboundCompleted
    ) {
    }
}

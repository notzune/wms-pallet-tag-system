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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProductionSnapshotSectionLoader implements DailyOperationsSectionLoader {

    private final QueryService queryService;

    public ProductionSnapshotSectionLoader() {
        this(new QueryService());
    }

    ProductionSnapshotSectionLoader(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
    }

    @Override
    public String title() {
        return "Production Snapshot";
    }

    @Override
    public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) throws Exception {
        List<ProductionSnapshotRow> rows = queryService.fetchRows(context.config()).stream().map(this::mapRow).toList();
        return AnalyzerDashboardSectionSnapshot.success(title(), buildTable(rows));
    }

    ProductionSnapshotRow mapRow(QueryRow row) {
        return new ProductionSnapshotRow(
                row.workOrder(),
                row.itemNumber(),
                row.lastReceivedTime(),
                row.palletsProduced() == null ? 0 : row.palletsProduced()
        );
    }

    private JTable buildTable(List<ProductionSnapshotRow> rows) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Work Order", "Item", "Last Received", "Pallets Produced"},
                0
        );
        for (ProductionSnapshotRow row : rows) {
            model.addRow(new Object[]{row.workOrder(), row.itemNumber(), row.lastReceivedTime(), row.palletsProduced()});
        }
        return new JTable(model);
    }

    static final class QueryService {
        private static final String SQL = """
                select * from
                (select frstol work_order,prtnum item_num,max(trndte) last_rcvd_time,count(distinct lodnum) pallets_produced from dlytrn
                where trndte > trunc(sysdate) - 2/24
                and fr_arecod = 'WEXP'
                and supnum = 'PROD'
                group by frstol,prtnum)
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
                                resultSet.getString("work_order"),
                                resultSet.getString("item_num"),
                                toLocalDateTime(resultSet, "last_rcvd_time"),
                                integerValue(resultSet, "pallets_produced")
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

        private LocalDateTime toLocalDateTime(ResultSet resultSet, String column) throws Exception {
            java.sql.Timestamp timestamp = resultSet.getTimestamp(column);
            return timestamp == null ? null : timestamp.toLocalDateTime();
        }
    }

    static record QueryRow(
            String workOrder,
            String itemNumber,
            LocalDateTime lastReceivedTime,
            Integer palletsProduced
    ) {
    }
}

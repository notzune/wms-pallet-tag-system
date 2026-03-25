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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StorageCapacitySectionLoader implements DailyOperationsSectionLoader {

    private final QueryService queryService;

    public StorageCapacitySectionLoader() {
        this(new QueryService());
    }

    StorageCapacitySectionLoader(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService cannot be null");
    }

    @Override
    public String title() {
        return "Storage / Capacity Snapshot";
    }

    @Override
    public AnalyzerDashboardSectionSnapshot loadSection(AnalyzerContext context) throws Exception {
        List<StorageCapacityRow> rows = queryService.fetchRows(context.config()).stream().map(this::mapRow).toList();
        return AnalyzerDashboardSectionSnapshot.success(title(), buildTable(rows));
    }

    StorageCapacityRow mapRow(QueryRow row) {
        boolean separator = row.buildingId() == null;
        return new StorageCapacityRow(
                row.buildingId(),
                row.pallets(),
                row.positions(),
                row.pctFull(),
                row.emptyRacks(),
                separator
        );
    }

    private JTable buildTable(List<StorageCapacityRow> rows) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Building", "Pallets", "Positions", "Pct Full", "Empty Racks"},
                0
        );
        for (StorageCapacityRow row : rows) {
            if (row.separator()) {
                model.addRow(new Object[]{"", "", "", "", ""});
            } else {
                model.addRow(new Object[]{row.buildingId(), row.pallets(), row.positions(), row.pctFull(), row.emptyRacks()});
            }
        }
        return new JTable(model);
    }

    static final class QueryService {
        private static final String SQL = """
                select racked.bldg_id
                ,sum(curqvl) pallets,case when bldg_id = 'ASRS1' then 5450 when bldg_id = 'ABRK1' then 1629 else sum(maxqvl) end positions
                ,case when bldg_id = 'ASRS1' then sum(curqvl)/5450*100 else sum(curqvl)/sum(maxqvl)*100 end pct_full
                ,sum(empties) empty_racks
                from
                (select
                case when l.stoloc in ('ASRS','ASRSPK') then 'ASRS1' else a.arecod end bldg_id
                ,l.stoloc,curqvl,maxqvl,case when curqvl = 0 then 1 end empties
                from locmst l,aremst a
                where
                 l.arecod = a.arecod
                and a.fwiflg = 1
                and a.stoare_flg = 1
                and l.useflg = 1
                and l.stoflg = 1
                and a.arecod not like 'DAMAGEIB'
                and a.arecod not like 'DAMAGEWHSE'
                and a.arecod not like 'MAINPICKNJ'
                and a.arecod not like 'MAINPICKTR'
                and a.arecod not like 'ROSSIPICK'
                and a.arecod not like 'RCPICK'
                and a.arecod not like 'ROSSIQAHD'
                and a.arecod not like 'ROSSIWEXP'
                and a.arecod not like 'ROSSI2ASRS'
                and a.arecod not like 'AMBIENT'
                and a.arecod not like 'ROSSICLUBP'
                and a.arecod not like 'MAIN2ROSSI'
                and a.arecod not like 'MAININDUCT'
                and a.arecod not like 'ADJ'
                and a.arecod not like 'RDT%'
                and l.stoloc <> 'CDFLOOR'
                union all
                select 'ASRS1' ,'ASRSPK',count(distinct lodnum) pallets ,null positions ,null empties
                from inventory_view iv where iv.stoloc = 'ASRSPK' ) racked
                group by racked.bldg_id
                union all
                select null,null,null,null,null from dual
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
                                resultSet.getString("bldg_id"),
                                doubleValue(resultSet, "pallets"),
                                doubleValue(resultSet, "positions"),
                                doubleValue(resultSet, "pct_full"),
                                doubleValue(resultSet, "empty_racks")
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

        private Double doubleValue(ResultSet resultSet, String column) throws Exception {
            double value = resultSet.getDouble(column);
            return resultSet.wasNull() ? null : value;
        }
    }

    static record QueryRow(
            String buildingId,
            Double pallets,
            Double positions,
            Double pctFull,
            Double emptyRacks
    ) {
    }
}

package com.tbg.wms.cli.gui.analyzers.openloads;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

final class OpenLoadsQueryRepository {
    private final DataSource dataSource;
    private final OpenLoadsRowMapper rowMapper;

    OpenLoadsQueryRepository(DataSource dataSource, OpenLoadsRowMapper rowMapper) {
        this.dataSource = dataSource;
        this.rowMapper = rowMapper;
    }

    List<OpenLoadsRow> fetchRows() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(OpenLoadsSql.query());
             ResultSet resultSet = statement.executeQuery()) {
            List<OpenLoadsRow> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(rowMapper.read(resultSet));
            }
            return rows;
        }
    }
}

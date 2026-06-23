package com.tbg.wms.cli.gui.analyzers.openloads;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.db.DataSourceFactory;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.List;

final class OpenLoadsDataSourceSupport {
    private final DataSourceProvider dataSourceProvider;

    OpenLoadsDataSourceSupport() {
        this(config -> new DataSourceFactory(config).create());
    }

    OpenLoadsDataSourceSupport(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    List<OpenLoadsRow> fetchRows(AppConfig config, QueryOperation operation) throws Exception {
        DataSource dataSource = dataSourceProvider.create(config);
        try {
            return operation.fetchRows(dataSource);
        } finally {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                hikariDataSource.close();
            }
        }
    }

    interface DataSourceProvider {
        DataSource create(AppConfig config) throws Exception;
    }

    interface QueryOperation {
        List<OpenLoadsRow> fetchRows(DataSource dataSource) throws Exception;
    }
}

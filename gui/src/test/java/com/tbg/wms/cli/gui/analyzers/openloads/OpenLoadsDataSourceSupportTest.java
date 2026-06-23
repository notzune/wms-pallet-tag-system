package com.tbg.wms.cli.gui.analyzers.openloads;

import com.tbg.wms.core.AppConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenLoadsDataSourceSupportTest {

    @Test
    void fetchRows_shouldCreateDataSourceRunOperationAndCloseHikariDataSource() throws Exception {
        AppConfig config = new AppConfig();
        HikariDataSource dataSource = new HikariDataSource();
        AtomicReference<AppConfig> providerConfig = new AtomicReference<>();
        AtomicReference<DataSource> operationDataSource = new AtomicReference<>();

        OpenLoadsDataSourceSupport support = new OpenLoadsDataSourceSupport(requestedConfig -> {
            providerConfig.set(requestedConfig);
            return dataSource;
        });

        support.fetchRows(config, selectedDataSource -> {
            operationDataSource.set(selectedDataSource);
            return List.of();
        });

        assertSame(config, providerConfig.get());
        assertSame(dataSource, operationDataSource.get());
        assertTrue(dataSource.isClosed());
    }
}

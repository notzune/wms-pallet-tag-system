package com.tbg.wms.cli.gui.analyzers.openloads;

import com.tbg.wms.core.AppConfig;

import java.util.List;

public final class OpenLoadsQueryService {
    private final OpenLoadsRowMapper rowMapper = new OpenLoadsRowMapper();
    private final OpenLoadsDataSourceSupport dataSourceSupport = new OpenLoadsDataSourceSupport();

    List<OpenLoadsRow> fetchRows(AppConfig config) throws Exception {
        return dataSourceSupport.fetchRows(config,
                dataSource -> new OpenLoadsQueryRepository(dataSource, rowMapper).fetchRows());
    }
}

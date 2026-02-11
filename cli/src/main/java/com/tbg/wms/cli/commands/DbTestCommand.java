package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.db.DataSourceFactory;
import com.tbg.wms.core.db.DbHealthService;
import picocli.CommandLine.Command;

import javax.sql.DataSource;
import java.util.concurrent.Callable;

@Command(
        name = "db-test",
        description = "Validate DB connectivity for the active site + environment"
)
public final class DbTestCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        AppConfig c = RootCommand.config();

        System.out.println("Active Site: " + c.activeSiteCode() + " (" + c.siteName(c.activeSiteCode()) + ")");
        System.out.println("WMS Env   : " + c.wmsEnvironment());
        System.out.println("JDBC URL  : " + c.oracleJdbcUrl());

        DataSource ds = new DataSourceFactory(c).create();
        DbHealthService health = new DbHealthService(ds);

        boolean ok = health.ping();
        System.out.println(ok ? "DB Ping  : OK" : "DB Ping  : FAILED");

        return ok ? 0 : 2;
    }
}

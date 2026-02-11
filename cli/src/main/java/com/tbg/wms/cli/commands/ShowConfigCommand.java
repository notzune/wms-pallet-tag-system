package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "config",
        description = "Print resolved runtime config (safe fields only)."
)
public final class ShowConfigCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        AppConfig cfg = RootCommand.config();
        String site = cfg.activeSiteCode();

        System.out.println("ACTIVE_SITE: " + site + " (" + cfg.siteName(site) + ")");
        System.out.println("WMS_ENV:      " + cfg.wmsEnvironment());
        System.out.println("DB HOST:      " + cfg.siteHost(site));
        System.out.println("DB USER:      " + cfg.oracleUsername());
        System.out.println("DB SERVICE:   " + cfg.oracleService() + ":" + cfg.oraclePort());
        System.out.println("JDBC URL:     " + cfg.oracleJdbcUrl());
        System.out.println("DB POOL:      max=" + cfg.dbPoolMaxSize() + ", connTimeoutMs=" + cfg.dbPoolConnectionTimeoutMs());
        System.out.println();
        System.out.println("PRINTER ROUTING FILE: " + cfg.printerRoutingFile());
        System.out.println("PRINTER DEFAULT_ID : " + cfg.defaultPrinterId());
        System.out.println("PRINTER FORCE_ID   : " + (cfg.forcedPrinterIdOrNull() == null ? "(none)" : cfg.forcedPrinterIdOrNull()));

        return 0;
    }
}

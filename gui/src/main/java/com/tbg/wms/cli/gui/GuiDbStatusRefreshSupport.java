package com.tbg.wms.cli.gui;

import java.util.Objects;
import java.util.Optional;

final class GuiDbStatusRefreshSupport {

    private final GuiDbStatusSupport statusSupport;

    GuiDbStatusRefreshSupport() {
        this(new GuiDbStatusSupport());
    }

    GuiDbStatusRefreshSupport(GuiDbStatusSupport statusSupport) {
        this.statusSupport = Objects.requireNonNull(statusSupport, "statusSupport cannot be null");
    }

    GuiDbStatusSupport.StatusState checking(String oracleService) {
        return statusSupport.checking(oracleService);
    }

    GuiDbStatusSupport.StatusState verify(String oracleService, ConnectivityProbe probe) throws Exception {
        Objects.requireNonNull(probe, "probe cannot be null");
        probe.verify();
        return statusSupport.connected(oracleService);
    }

    Optional<GuiDbStatusSupport.StatusState> failure(String oracleService, Throwable throwable) {
        return statusSupport.failure(oracleService, throwable);
    }

    @FunctionalInterface
    interface ConnectivityProbe {
        void verify() throws Exception;
    }
}

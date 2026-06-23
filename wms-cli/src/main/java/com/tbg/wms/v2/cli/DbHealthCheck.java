package com.tbg.wms.v2.cli;

@FunctionalInterface
public interface DbHealthCheck {
    Result check() throws Exception;

    record Result(boolean connected, String message) {
    }
}

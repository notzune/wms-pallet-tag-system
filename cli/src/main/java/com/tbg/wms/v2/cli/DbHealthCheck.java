package com.tbg.wms.v2.cli;

/**
 * Defines the database health check contract used by CLI status commands.
 */
@FunctionalInterface
public interface DbHealthCheck {
    /**
     * Checks whether the configured database endpoint is reachable.
     *
     * @return connection status and display message
     * @throws Exception if the check cannot complete
     */
    Result check() throws Exception;

    /**
     * Carries database health status for CLI display.
     *
     * @param connected whether the database connection succeeded
     * @param message human-readable connection detail
     */
    record Result(boolean connected, String message) {
    }
}

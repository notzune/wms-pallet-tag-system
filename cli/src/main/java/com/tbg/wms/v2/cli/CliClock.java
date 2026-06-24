package com.tbg.wms.v2.cli;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Defines the contract for cli clock behavior in WMS 2.0 workflows.
 */
public interface CliClock {
    /**
     * Returns the current CLI timestamp.
     *
     * @return the timestamp.
     */
    String timestamp();

    /**
     * Provides system clock behavior for WMS 2.0 workflows.
     */
    final class SystemClock implements CliClock {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        private final Clock clock;

        /**
         * Creates a CLI clock backed by the supplied Java clock.
         *
         * @param clock source used to resolve the current timestamp
         */
        public SystemClock(Clock clock) {
            this.clock = clock;
        }

        /**
         * Returns the current timestamp formatted for CLI output paths.
         *
         * @return formatted local timestamp
         */
        @Override
        public String timestamp() {
            return LocalDateTime.now(clock).format(FORMATTER);
        }
    }

    /**
     * Carries fixed data across WMS 2.0 module boundaries.
     *
     * @param timestamp the timestamp.
     */
    record Fixed(String timestamp) implements CliClock {
    }
}

package com.tbg.wms.v2.cli;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface CliClock {
    String timestamp();

    final class SystemClock implements CliClock {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        private final Clock clock;

        public SystemClock(Clock clock) {
            this.clock = clock;
        }

        @Override
        public String timestamp() {
            return LocalDateTime.now(clock).format(FORMATTER);
        }
    }

    record Fixed(String timestamp) implements CliClock {
    }
}

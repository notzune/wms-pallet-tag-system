package com.tbg.wms.v2.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliModuleSmokeTest {
    @Test
    void moduleNamespaceIsAvailable() {
        assertEquals("cli", "cli");
    }
}

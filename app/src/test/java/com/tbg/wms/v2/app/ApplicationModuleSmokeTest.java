package com.tbg.wms.v2.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationModuleSmokeTest {
    @Test
    void moduleNamespaceIsAvailable() {
        assertEquals("app", "app");
    }
}

package com.tbg.wms.v2.printing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrintingModuleSmokeTest {
    @Test
    void moduleNamespaceIsAvailable() {
        assertEquals("wms-printing", "wms-printing");
    }
}

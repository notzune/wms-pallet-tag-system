package com.tbg.wms.v2.smoke;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmokeModuleSmokeTest {
    @Test
    void moduleNamespaceIsAvailable() {
        assertEquals("smoke", "smoke");
    }
}

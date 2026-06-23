package com.tbg.wms.v2.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DesktopModuleSmokeTest {
    @Test
    void moduleNamespaceIsAvailable() {
        assertEquals("wms-desktop", "wms-desktop");
    }
}

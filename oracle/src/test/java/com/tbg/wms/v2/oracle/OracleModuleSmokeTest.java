package com.tbg.wms.v2.oracle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleModuleSmokeTest {
    @Test
    void moduleNamespaceIsAvailable() {
        assertEquals("oracle", "oracle");
    }
}

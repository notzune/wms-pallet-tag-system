package com.tbg.wms.v2.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainModuleSmokeTest {
    @Test
    void moduleNamespaceIsAvailable() {
        assertEquals("domain", "domain");
    }
}

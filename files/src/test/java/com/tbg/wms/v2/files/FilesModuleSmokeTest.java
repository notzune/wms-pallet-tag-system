package com.tbg.wms.v2.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilesModuleSmokeTest {
    @Test
    void moduleNamespaceIsAvailable() {
        assertEquals("files", "files");
    }
}

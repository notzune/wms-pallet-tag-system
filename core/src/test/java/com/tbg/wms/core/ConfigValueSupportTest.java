package com.tbg.wms.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValueSupportTest {

    @Test
    void raw_shouldHonorEnvThenFileThenDefaults() {
        ConfigValueSupport support = new ConfigValueSupport(
                Map.of("A", " env ", "B", " "),
                Map.of("A", "file-a", "B", " file-b ", "C", "file-c"),
                Map.of("B", "default-b", "D", " default-d "),
                "wms-tags.env"
        );

        assertEquals("env", support.raw("A"));
        assertEquals("file-b", support.raw("B"));
        assertEquals("file-c", support.raw("C"));
        assertEquals("default-d", support.raw("D"));
        assertNull(support.raw("MISSING"));
    }

    @Test
    void rawFromEnvOrFile_shouldIgnoreClasspathDefaults() {
        ConfigValueSupport support = new ConfigValueSupport(
                Map.of(),
                Map.of(),
                Map.of("A", "default-a"),
                "wms-tags.env"
        );

        assertNull(support.rawFromEnvOrFile("A"));
    }

    @Test
    void requiredAndParsers_shouldUseSharedErrorHandling() {
        ConfigValueSupport support = new ConfigValueSupport(
                Map.of("INT_KEY", "bad-int", "REQ_KEY", " value "),
                Map.of(),
                Map.of(),
                "wms-tags.env"
        );

        assertEquals("value", support.required("REQ_KEY"));
        assertThrows(IllegalStateException.class, () -> support.required("MISSING_KEY"));
        assertThrows(IllegalStateException.class, () -> support.parseInt("INT_KEY", "1"));
        assertEquals(42L, support.parseLong("LONG_KEY", "42"));
        assertEquals(1.25, support.parseDouble("DOUBLE_KEY", "1.25"));
    }
}

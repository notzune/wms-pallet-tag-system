package com.tbg.wms.core.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionSupportTest {

    @Test
    void normalize_shouldStripLeadingVPrefix() {
        assertEquals("1.7.1", VersionSupport.normalize("v1.7.1"));
    }

    @Test
    void compare_shouldTreatNewerNumericSegmentsAsGreater() {
        assertTrue(VersionSupport.compare("1.7.2", "1.7.1") > 0);
        assertTrue(VersionSupport.compare("1.8.0", "1.7.9") > 0);
        assertEquals(0, VersionSupport.compare("v1.7.1", "1.7.1"));
    }

    @Test
    void resolvePackageVersion_shouldReturnBlankWhenUnavailable() {
        assertEquals("", VersionSupport.resolvePackageVersion(VersionSupportTest.class));
    }

    @Test
    void readFirstNonBlankProperty_shouldReturnBlankForMissingProperty() {
        assertEquals("", VersionSupport.readFirstNonBlankProperty(VersionSupportTest.class, "version", "/missing.properties"));
    }
}

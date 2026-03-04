package com.tbg.wms.core.location;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LocationNumberMappingServiceTest {

    @TempDir
    Path tempDir;

    private LocationNumberMappingService service;

    @BeforeEach
    void setUp() throws IOException {
        Path csv = tempDir.resolve("walm_loc_num_matrix.csv");
        String content = "Sold-To Name,Location #,Sold-To #\n"
                + "WAL-MART CANADA 7087R,7087R,0100003434\n"
                + "WAL-MART CANADA 7118R,7118R,0000009999\n";
        Files.writeString(csv, content);
        service = new LocationNumberMappingService(csv);
    }

    @Test
    void resolveDcLocationHandlesPrefixAndZeroPadding() {
        assertEquals("7087R", service.resolveDcLocation("C100003434"));
        assertEquals("7087R", service.resolveDcLocation("0100003434"));
    }

    @Test
    void resolveDcLocationTreatsLowercaseCPrefixTheSame() {
        assertEquals("7087R", service.resolveDcLocation("c100003434"));
    }

    @Test
    void resolveDcLocationReturnsInputWhenNotMapped() {
        assertEquals("ABC123", service.resolveDcLocation("ABC123"));
    }
}

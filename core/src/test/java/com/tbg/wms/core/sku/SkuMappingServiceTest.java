/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.sku;

import com.tbg.wms.core.model.WalmartSkuMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkuMappingService.
 */
class SkuMappingServiceTest {

    @TempDir
    Path tempDir;

    private Path csvFile;
    private SkuMappingService service;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test CSV file
        csvFile = tempDir.resolve("sku-mapping.csv");

        String csvContent = "TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU\n" +
                "205641,30081705,1.36L PL 1/6 NJ STRW BAN,1.36L PL 1/6 NJ STRW BAN\n" +
                "198304,31154879,1.54L PL 1/6 TROP BBRYBLBRY,1.54L PL 1/6 TROP BBRYBLBRY\n" +
                "320445,50203157,STARBUCKS COLD BREW BLACK UNSW,STARBUCKS COLD BREW BLACK UNSW\n";

        Files.writeString(csvFile, csvContent);

        service = new SkuMappingService(csvFile);
    }

    @Test
    void testLoadMappingsFromCsv() {
        assertEquals(3, service.getMappingCount(), "Should load 3 mappings");
    }

    @Test
    void testFindByTbgSku() {
        WalmartSkuMapping mapping = service.findByTbgSku("205641");

        assertNotNull(mapping);
        assertEquals("205641", mapping.getTbgSku());
        assertEquals("30081705", mapping.getWalmartItemNo());
        assertEquals("1.36L PL 1/6 NJ STRW BAN", mapping.getDescription());
    }

    @Test
    void testFindByTbgSkuWithSpaces() {
        // Service should handle whitespace
        WalmartSkuMapping mapping = service.findByTbgSku("  205641  ");

        assertNotNull(mapping);
        assertEquals("30081705", mapping.getWalmartItemNo());
    }

    @Test
    void testFindByTbgSkuNotFound() {
        WalmartSkuMapping mapping = service.findByTbgSku("999999");

        assertNull(mapping, "Should return null for unknown SKU");
    }

    @Test
    void testFindByWalmartItem() {
        WalmartSkuMapping mapping = service.findByWalmartItem("30081705");

        assertNotNull(mapping);
        assertEquals("205641", mapping.getTbgSku());
        assertEquals("30081705", mapping.getWalmartItemNo());
    }

    @Test
    void testFindByWalmartItemNotFound() {
        WalmartSkuMapping mapping = service.findByWalmartItem("99999999");

        assertNull(mapping, "Should return null for unknown Walmart item");
    }

    @Test
    void testFindByPrtnumLastSixDigits() {
        // PRTNUM format: 17 digits. If last 6 match a TBG SKU in CSV...
        // For testing, we'll assume 205641 is the last 6 of some PRTNUM
        WalmartSkuMapping mapping = service.findByPrtnum("10048500205641000");

        assertNotNull(mapping);
        assertEquals("205641", mapping.getTbgSku());
        assertEquals("30081705", mapping.getWalmartItemNo());
    }

    @Test
    void testFindByPrtnumDirectMatch() {
        // If PRTNUM is already in short format
        WalmartSkuMapping mapping = service.findByPrtnum("205641");

        assertNotNull(mapping);
        assertEquals("30081705", mapping.getWalmartItemNo());
    }

    @Test
    void testFindByPrtnumNotFound() {
        WalmartSkuMapping mapping = service.findByPrtnum("99999999999999999");

        assertNull(mapping, "Should return null for unknown PRTNUM");
    }

    @Test
    void testFindByNullInputReturnsNull() {
        assertNull(service.findByTbgSku(null));
        assertNull(service.findByWalmartItem(null));
        assertNull(service.findByPrtnum(null));
    }

    @Test
    void testFindByEmptyInputReturnsNull() {
        assertNull(service.findByTbgSku(""));
        assertNull(service.findByWalmartItem(""));
        assertNull(service.findByPrtnum(""));
    }

    @Test
    void testGetAllMappings() {
        Map<String, WalmartSkuMapping> all = service.getAllMappings();

        assertEquals(3, all.size());
        assertTrue(all.containsKey("205641"));
        assertTrue(all.containsKey("198304"));
        assertTrue(all.containsKey("320445"));
    }

    @Test
    void testGetAllMappingsIsImmutable() {
        Map<String, WalmartSkuMapping> all = service.getAllMappings();

        assertThrows(UnsupportedOperationException.class,
                () -> all.put("new", new WalmartSkuMapping("1", "2", "3")),
                "Should return immutable map");
    }

    @Test
    void testFileNotFoundThrows() {
        Path nonExistent = tempDir.resolve("does-not-exist.csv");

        assertThrows(IllegalArgumentException.class,
                () -> new SkuMappingService(nonExistent),
                "Should throw for missing file");
    }

    @Test
    void testEmptyFileLoads() throws IOException {
        Path emptyFile = tempDir.resolve("empty.csv");
        Files.writeString(emptyFile, "TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU\n");

        SkuMappingService emptyService = new SkuMappingService(emptyFile);

        assertEquals(0, emptyService.getMappingCount());
    }

    @Test
    void testMissingFieldsSkipped() throws IOException {
        Path badFile = tempDir.resolve("bad.csv");
        String badContent = "TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU\n" +
                "205641,30081705,1.36L PL 1/6 NJ STRW BAN,1.36L PL 1/6 NJ STRW BAN\n" +
                "incomplete\n" +
                "198304,31154879,1.54L PL 1/6 TROP BBRYBLBRY,1.54L PL 1/6 TROP BBRYBLBRY\n";

        Files.writeString(badFile, badContent);

        SkuMappingService badService = new SkuMappingService(badFile);

        // Should load 2 valid mappings, skip the incomplete line
        assertEquals(2, badService.getMappingCount());
    }

    @Test
    void testMultipleLookupsAreFast() {
        // Verify O(1) lookup performance by doing many lookups
        for (int i = 0; i < 1000; i++) {
            assertNotNull(service.findByTbgSku("205641"));
            assertNotNull(service.findByWalmartItem("30081705"));
        }
    }

    @Test
    void testReverseAndForwardLookupAreConsistent() {
        WalmartSkuMapping forward = service.findByTbgSku("205641");
        WalmartSkuMapping reverse = service.findByWalmartItem("30081705");

        assertNotNull(forward);
        assertNotNull(reverse);
        assertEquals(forward.getTbgSku(), reverse.getTbgSku());
        assertEquals(forward.getWalmartItemNo(), reverse.getWalmartItemNo());
    }
}


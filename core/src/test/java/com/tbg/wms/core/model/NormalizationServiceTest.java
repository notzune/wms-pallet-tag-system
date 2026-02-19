/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NormalizationService utility methods.
 *
 * Tests verify all normalization operations including null handling,
 * type conversions, validation, and error scenarios.
 */
class NormalizationServiceTest {

    // String Normalization Tests
    @Test
    void testNormalizeStringTrimsWhitespace() {
        assertEquals("hello", NormalizationService.normalizeString("  hello  "));
        assertEquals("hello\nworld", NormalizationService.normalizeString("\thello\nworld\t"));
    }

    @Test
    void testNormalizeStringHandlesNull() {
        assertEquals("", NormalizationService.normalizeString(null));
    }

    @Test
    void testNormalizeStringPreservesCase() {
        assertEquals("MiXeD", NormalizationService.normalizeString("MiXeD"));
    }

    @Test
    void testNormalizeStringEmptyString() {
        assertEquals("", NormalizationService.normalizeString(""));
    }

    // Uppercase Normalization Tests
    @Test
    void testNormalizeToUppercaseConvertsToUpper() {
        assertEquals("HELLO", NormalizationService.normalizeToUppercase("hello"));
        assertEquals("HELLO WORLD", NormalizationService.normalizeToUppercase("hello world"));
    }

    @Test
    void testNormalizeToUppercaseHandlesNull() {
        assertEquals("", NormalizationService.normalizeToUppercase(null));
    }

    @Test
    void testNormalizeToUppercaseTrimsAndConverts() {
        assertEquals("HELLO", NormalizationService.normalizeToUppercase("  hello  "));
    }

    // Integer Normalization Tests
    @Test
    void testNormalizeToIntSuccessfulParse() {
        assertEquals(42, NormalizationService.normalizeToInt("42", 0));
        assertEquals(-100, NormalizationService.normalizeToInt("-100", 0));
        assertEquals(0, NormalizationService.normalizeToInt("0", 999));
    }

    @Test
    void testNormalizeToIntHandlesNull() {
        assertEquals(0, NormalizationService.normalizeToInt(null, 0));
        assertEquals(99, NormalizationService.normalizeToInt(null, 99));
    }

    @Test
    void testNormalizeToIntHandlesEmptyString() {
        assertEquals(0, NormalizationService.normalizeToInt("", 0));
        assertEquals(50, NormalizationService.normalizeToInt("   ", 50));
    }

    @Test
    void testNormalizeToIntHandlesInvalidFormat() {
        assertEquals(0, NormalizationService.normalizeToInt("abc", 0));
        assertEquals(99, NormalizationService.normalizeToInt("12.5", 99));
    }

    @Test
    void testNormalizeToIntTrimsBeforeParsing() {
        assertEquals(42, NormalizationService.normalizeToInt("  42  ", 0));
    }

    // Double Normalization Tests
    @Test
    void testNormalizeToDoubleSuccessfulParse() {
        assertEquals(42.5, NormalizationService.normalizeToDouble("42.5", 0.0));
        assertEquals(-100.0, NormalizationService.normalizeToDouble("-100", 0.0));
        assertEquals(0.0, NormalizationService.normalizeToDouble("0", 999.0));
    }

    @Test
    void testNormalizeToDoubleHandlesNull() {
        assertEquals(0.0, NormalizationService.normalizeToDouble(null, 0.0));
        assertEquals(99.9, NormalizationService.normalizeToDouble(null, 99.9));
    }

    @Test
    void testNormalizeToDoubleHandlesEmptyString() {
        assertEquals(0.0, NormalizationService.normalizeToDouble("", 0.0));
        assertEquals(50.5, NormalizationService.normalizeToDouble("   ", 50.5));
    }

    @Test
    void testNormalizeToDoubleHandlesInvalidFormat() {
        assertEquals(0.0, NormalizationService.normalizeToDouble("abc", 0.0));
        assertEquals(99.9, NormalizationService.normalizeToDouble("invalid", 99.9));
    }

    @Test
    void testNormalizeToDoubleTrimsBeforeParsing() {
        assertEquals(42.5, NormalizationService.normalizeToDouble("  42.5  ", 0.0));
    }

    // Required Field Validation Tests
    @Test
    void testRequireNonEmptyWithValidValue() {
        assertEquals("hello", NormalizationService.requireNonEmpty("hello", "fieldName"));
        assertEquals("test", NormalizationService.requireNonEmpty("  test  ", "fieldName"));
    }

    @Test
    void testRequireNonEmptyThrowsOnNull() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.requireNonEmpty(null, "testField"));
        assertTrue(thrown.getMessage().contains("testField"));
        assertTrue(thrown.getMessage().contains("empty"));
    }

    @Test
    void testRequireNonEmptyThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.requireNonEmpty("", "fieldName"));
    }

    @Test
    void testRequireNonEmptyThrowsOnWhitespace() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.requireNonEmpty("   ", "fieldName"));
    }

    // SKU Normalization Tests
    @Test
    void testNormalizeSkuSuccess() {
        assertEquals("SKU123", NormalizationService.normalizeSku("SKU123"));
        assertEquals("ABC", NormalizationService.normalizeSku("abc"));
    }

    @Test
    void testNormalizeSkuThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.normalizeSku(""));
    }

    @Test
    void testNormalizeSkuThrowsOnNull() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.normalizeSku(null));
    }

    // Staging Location Normalization Tests
    @Test
    void testNormalizeStagingLocationSuccess() {
        assertEquals("ROSSI", NormalizationService.normalizeStagingLocation("rossi"));
        assertEquals("OFFICE", NormalizationService.normalizeStagingLocation("OFFICE"));
    }

    @Test
    void testNormalizeStagingLocationThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.normalizeStagingLocation(""));
    }

    // Barcode Normalization Tests
    @Test
    void testNormalizeBarcodeSuccess() {
        assertEquals("123456789012", NormalizationService.normalizeBarcode("123456789012"));
        assertEquals("test-barcode", NormalizationService.normalizeBarcode("  test-barcode  "));
    }

    @Test
    void testNormalizeBarcodeThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.normalizeBarcode(""));
    }

    @Test
    void testNormalizeBarcodeThrowsOnNull() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.normalizeBarcode(null));
    }

    // Carrier Code Normalization Tests
    @Test
    void testNormalizeCarrierCodeSuccess() {
        assertEquals("UPS", NormalizationService.normalizeCarrierCode("ups"));
        assertEquals("FEDEX", NormalizationService.normalizeCarrierCode("FedEx"));
    }

    @Test
    void testNormalizeCarrierCodeThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.normalizeCarrierCode(""));
    }

    @Test
    void testNormalizeCarrierCodeThrowsOnNull() {
        assertThrows(IllegalArgumentException.class,
                () -> NormalizationService.normalizeCarrierCode(null));
    }
}


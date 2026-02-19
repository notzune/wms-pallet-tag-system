/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Service for normalizing and transforming WMS data.
 *
 * This service applies data cleaning, trimming, uppercase conversion, and
 * validation to raw WMS data before it enters the domain model layer.
 *
 * All normalizations are idempotent - applying them multiple times produces
 * the same result.
 */
public final class NormalizationService {

    private static final Logger log = LoggerFactory.getLogger(NormalizationService.class);

    /**
     * Normalizes a string value: trim whitespace and handle nulls.
     *
     * @param value the raw string value
     * @return trimmed string, or empty string if null
     */
    public static String normalizeString(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Normalizes a string to uppercase.
     *
     * @param value the raw string value
     * @return uppercase trimmed string, or empty string if null
     */
    public static String normalizeToUppercase(String value) {
        String normalized = normalizeString(value);
        return normalized.isEmpty() ? normalized : normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes a numeric string to an integer.
     *
     * @param value the raw string value
     * @param defaultValue value to return if parsing fails
     * @return parsed integer or defaultValue if null or invalid
     */
    public static int normalizeToInt(String value, int defaultValue) {
        try {
            String normalized = normalizeString(value);
            return normalized.isEmpty() ? defaultValue : Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer from value: {}", value);
            return defaultValue;
        }
    }

    /**
     * Normalizes a numeric string to a double.
     *
     * @param value the raw string value
     * @param defaultValue value to return if parsing fails
     * @return parsed double or defaultValue if null or invalid
     */
    public static double normalizeToDouble(String value, double defaultValue) {
        try {
            String normalized = normalizeString(value);
            return normalized.isEmpty() ? defaultValue : Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse double from value: {}", value);
            return defaultValue;
        }
    }

    /**
     * Validates that a required string field is not empty.
     *
     * @param value the value to validate
     * @param fieldName the field name (for error messages)
     * @return the normalized value
     * @throws IllegalArgumentException if value is null or empty after normalization
     */
    public static String requireNonEmpty(String value, String fieldName) {
        String normalized = normalizeString(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Required field '" + fieldName + "' is empty");
        }
        return normalized;
    }

    /**
     * Normalizes a line item field name for consistency.
     *
     * Applies uppercase and trimming but preserves special characters.
     *
     * @param value the raw field value
     * @return normalized field value
     */
    public static String normalizeFieldName(String value) {
        return normalizeToUppercase(value);
    }

    /**
     * Normalizes a SKU for database lookup.
     *
     * SKUs are typically uppercase alphanumeric codes.
     *
     * @param sku the raw SKU value
     * @return normalized SKU (uppercase, trimmed)
     * @throws IllegalArgumentException if SKU is empty
     */
    public static String normalizeSku(String sku) {
        return requireNonEmpty(normalizeToUppercase(sku), "SKU");
    }

    /**
     * Normalizes a staging location for routing.
     *
     * Staging locations are uppercase identifiers (e.g., "ROSSI", "OFFICE").
     *
     * @param location the raw location value
     * @return normalized location (uppercase, trimmed)
     * @throws IllegalArgumentException if location is empty
     */
    public static String normalizeStagingLocation(String location) {
        return requireNonEmpty(normalizeToUppercase(location), "stagingLocation");
    }

    /**
     * Normalizes an optional staging location for scenarios where WMS may omit it.
     *
     * @param location raw location value
     * @return uppercase trimmed location, or null when input is null/blank
     */
    public static String normalizeOptionalStagingLocation(String location) {
        String normalized = normalizeToUppercase(location);
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Normalizes a barcode value for printing.
     *
     * Barcodes are case-sensitive and should not be modified beyond trimming.
     *
     * @param barcode the raw barcode value
     * @return trimmed barcode
     * @throws IllegalArgumentException if barcode is empty
     */
    public static String normalizeBarcode(String barcode) {
        return requireNonEmpty(barcode, "barcode");
    }

    /**
     * Normalizes a carrier code.
     *
     * Carrier codes are uppercase short codes (e.g., "UPS", "FDX").
     *
     * @param carrierCode the raw carrier code
     * @return normalized carrier code (uppercase, trimmed)
     * @throws IllegalArgumentException if carrier code is empty
     */
    public static String normalizeCarrierCode(String carrierCode) {
        return requireNonEmpty(normalizeToUppercase(carrierCode), "carrierCode");
    }

    private NormalizationService() {
        // Utility class - prevent instantiation
    }
}


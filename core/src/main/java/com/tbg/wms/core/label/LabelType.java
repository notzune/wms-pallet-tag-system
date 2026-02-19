/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.label;

/**
 * Enumeration of supported label formats.
 *
 * Different label templates are used for different purposes:
 * - WALMART_CANADA_GRID: Grid-based label for Walmart Canada (primary)
 * - WALMART_DETAILED: Extended format with lot tracking and dates (secondary)
 */
public enum LabelType {
    /**
     * Walmart Canada Grid Label.
     * 4x6 pallet label with bordered grid layout.
     * Used for shipments to Walmart Canada (ROSSI staging location).
     */
    WALMART_CANADA_GRID("walmart-canada-label.zpl"),

    /**
     * Walmart Detailed Carrier Label.
     * Extended format showing lot tracking, manufacture/expiration dates.
     * Used for Walmart US or carrier-specific requirements.
     */
    WALMART_DETAILED("walmart-detailed-label.zpl");

    private final String templateFilename;

    LabelType(String templateFilename) {
        this.templateFilename = templateFilename;
    }

    public String getTemplateFilename() {
        return templateFilename;
    }
}


/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.Locale;

/**
 * Centralized family-bucket normalization for rail pallet math.
 */
public final class RailFamilyClassifier {

    /**
     * Maps any family code variant to one of the supported rail buckets.
     *
     * @param familyCode WMS family code
     * @return normalized family bucket
     */
    public FamilyBucket classify(String familyCode) {
        String normalized = familyCode == null ? "" : familyCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("CAN")) {
            return FamilyBucket.CAN;
        }
        if (normalized.contains("KEV")) {
            return FamilyBucket.KEV;
        }
        return FamilyBucket.DOM;
    }

    /**
     * Family buckets used by rail calculations and rendering.
     */
    public enum FamilyBucket {
        CAN,
        DOM,
        KEV
    }
}


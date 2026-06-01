/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.Locale;
import java.util.Set;

/**
 * Centralized family-bucket normalization for rail pallet math.
 */
public final class RailFamilyClassifier {
    private static final Set<String> COSTCO_CLUB_SHORTCODES = Set.of("20557", "20558");
    private static final String COSTCO_CLUB_DISPLAY_CODE = "CLUB";

    /**
     * Maps any family code variant to one of the supported rail buckets.
     *
     * @param familyCode WMS family code
     * @return normalized family bucket
     */
    public FamilyBucket classify(String familyCode) {
        return classify(familyCode, "");
    }

    /**
     * Maps an item/family pair to one of the supported rail buckets.
     *
     * @param familyCode WMS family code
     * @param itemNumber WMS short code shown on the rail card
     * @return normalized family bucket
     */
    public FamilyBucket classify(String familyCode, String itemNumber) {
        if (isCostcoClubShortcode(itemNumber)) {
            return FamilyBucket.CLUB;
        }
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
     * Returns the family code that should be shown on rail label family callouts.
     *
     * @param familyCode WMS family code
     * @param itemNumber WMS short code shown on the rail card
     * @return display family such as {@code DOM}, {@code CAN}, {@code KEV}, or {@code CLUB}
     */
    public String displayFamilyCode(String familyCode, String itemNumber) {
        if (isCostcoClubShortcode(itemNumber)) {
            return COSTCO_CLUB_DISPLAY_CODE;
        }
        return familyCode == null ? "" : familyCode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isCostcoClubShortcode(String itemNumber) {
        String normalized = itemNumber == null ? "" : itemNumber.trim();
        return COSTCO_CLUB_SHORTCODES.contains(normalized);
    }

    /**
     * Family buckets used by rail calculations and rendering.
     */
    public enum FamilyBucket {
        CAN,
        DOM,
        KEV,
        CLUB
    }
}


/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.core.rail;

import java.util.Objects;

/**
 * Item-family footprint lookup row used for rail pallet-equivalent math.
 */
public final class RailFamilyFootprint {
    private final String itemNumber;
    private final String familyCode;
    private final int casesPerPallet;

    public RailFamilyFootprint(String itemNumber, String familyCode, int casesPerPallet) {
        this.itemNumber = normalize(itemNumber);
        this.familyCode = normalizeUpper(familyCode);
        this.casesPerPallet = casesPerPallet;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public String getFamilyCode() {
        return familyCode;
    }

    public int getCasesPerPallet() {
        return casesPerPallet;
    }

    public boolean isValid() {
        return !itemNumber.isBlank() && !familyCode.isBlank() && casesPerPallet > 0;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeUpper(String value) {
        return normalize(value).toUpperCase();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RailFamilyFootprint)) {
            return false;
        }
        RailFamilyFootprint that = (RailFamilyFootprint) other;
        return casesPerPallet == that.casesPerPallet
                && itemNumber.equals(that.itemNumber)
                && familyCode.equals(that.familyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemNumber, familyCode, casesPerPallet);
    }
}

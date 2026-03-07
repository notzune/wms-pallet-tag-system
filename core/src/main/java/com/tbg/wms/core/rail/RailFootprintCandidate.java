/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.1
 */
package com.tbg.wms.core.rail;

import java.util.Locale;
import java.util.Objects;

/**
 * One WMS footprint candidate resolved for a short code.
 */
public final class RailFootprintCandidate {
    private final String shortCode;
    private final String itemNumber;
    private final String familyCode;
    private final int casesPerPallet;

    public RailFootprintCandidate(String shortCode,
                                  String itemNumber,
                                  String familyCode,
                                  int casesPerPallet) {
        this.shortCode = normalize(shortCode);
        this.itemNumber = normalize(itemNumber);
        this.familyCode = normalizeUpper(familyCode);
        this.casesPerPallet = casesPerPallet;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeUpper(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }

    public String getShortCode() {
        return shortCode;
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
        return !shortCode.isBlank() && !itemNumber.isBlank() && !familyCode.isBlank() && casesPerPallet > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RailFootprintCandidate)) {
            return false;
        }
        RailFootprintCandidate that = (RailFootprintCandidate) other;
        return casesPerPallet == that.casesPerPallet
                && shortCode.equals(that.shortCode)
                && itemNumber.equals(that.itemNumber)
                && familyCode.equals(that.familyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortCode, itemNumber, familyCode, casesPerPallet);
    }
}

package com.tbg.wms.core.ems;

import java.util.Objects;

/**
 * Raw row extracted from the EMS reconciliation workbook.
 */
public final class EmsReconRow {
    private final String passLabel;
    private final int passIndex;
    private final int sheetRowNumber;
    private final String currentLocationId;
    private final String containerId;
    private final String sku;
    private final String asrsLocNum;
    private final String lodnum;
    private final String maxPrtnum;

    public EmsReconRow(String passLabel,
                       int passIndex,
                       int sheetRowNumber,
                       String currentLocationId,
                       String containerId,
                       String sku,
                       String asrsLocNum,
                       String lodnum,
                       String maxPrtnum) {
        this.passLabel = Objects.requireNonNull(passLabel, "passLabel cannot be null");
        this.passIndex = passIndex;
        this.sheetRowNumber = sheetRowNumber;
        this.currentLocationId = normalize(currentLocationId);
        this.containerId = normalize(containerId);
        this.sku = normalize(sku);
        this.asrsLocNum = normalize(asrsLocNum);
        this.lodnum = normalize(lodnum);
        this.maxPrtnum = normalize(maxPrtnum);
    }

    public String getPassLabel() {
        return passLabel;
    }

    public int getPassIndex() {
        return passIndex;
    }

    public int getSheetRowNumber() {
        return sheetRowNumber;
    }

    public String getCurrentLocationId() {
        return currentLocationId;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getSku() {
        return sku;
    }

    public String getAsrsLocNum() {
        return asrsLocNum;
    }

    public String getLodnum() {
        return lodnum;
    }

    public String getMaxPrtnum() {
        return maxPrtnum;
    }

    public boolean hasEmsData() {
        return !currentLocationId.isEmpty() || !containerId.isEmpty() || !sku.isEmpty();
    }

    public boolean hasWmsData() {
        return !asrsLocNum.isEmpty() || !lodnum.isEmpty() || !maxPrtnum.isEmpty();
    }

    public String stableKey() {
        if (!containerId.isEmpty()) {
            return "CONTAINER:" + containerId;
        }
        if (!lodnum.isEmpty()) {
            return "LODNUM:" + lodnum;
        }
        if (!maxPrtnum.isEmpty()) {
            return "PRTNUM:" + maxPrtnum;
        }
        return "ROW:" + passIndex + ":" + sheetRowNumber;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}

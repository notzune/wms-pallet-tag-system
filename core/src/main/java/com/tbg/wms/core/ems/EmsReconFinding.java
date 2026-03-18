package com.tbg.wms.core.ems;

import java.util.Objects;

/**
 * Operator-facing mismatch finding with a recommendation and fix intent.
 */
public final class EmsReconFinding {
    private final String passLabel;
    private final int passIndex;
    private final int sheetRowNumber;
    private final String containerId;
    private final String currentLocationId;
    private final String sku;
    private final String asrsLocNum;
    private final String lodnum;
    private final String maxPrtnum;
    private final EmsMismatchCategory category;
    private final boolean persistsIntoLaterPasses;
    private final String explanation;
    private final String recommendation;
    private final String suggestedFixCommand;

    public EmsReconFinding(String passLabel,
                           int passIndex,
                           int sheetRowNumber,
                           String containerId,
                           String currentLocationId,
                           String sku,
                           String asrsLocNum,
                           String lodnum,
                           String maxPrtnum,
                           EmsMismatchCategory category,
                           boolean persistsIntoLaterPasses,
                           String explanation,
                           String recommendation,
                           String suggestedFixCommand) {
        this.passLabel = Objects.requireNonNull(passLabel, "passLabel cannot be null");
        this.passIndex = passIndex;
        this.sheetRowNumber = sheetRowNumber;
        this.containerId = normalize(containerId);
        this.currentLocationId = normalize(currentLocationId);
        this.sku = normalize(sku);
        this.asrsLocNum = normalize(asrsLocNum);
        this.lodnum = normalize(lodnum);
        this.maxPrtnum = normalize(maxPrtnum);
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.persistsIntoLaterPasses = persistsIntoLaterPasses;
        this.explanation = Objects.requireNonNull(explanation, "explanation cannot be null");
        this.recommendation = Objects.requireNonNull(recommendation, "recommendation cannot be null");
        this.suggestedFixCommand = Objects.requireNonNull(suggestedFixCommand, "suggestedFixCommand cannot be null");
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

    public String getContainerId() {
        return containerId;
    }

    public String getCurrentLocationId() {
        return currentLocationId;
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

    public EmsMismatchCategory getCategory() {
        return category;
    }

    public boolean isPersistsIntoLaterPasses() {
        return persistsIntoLaterPasses;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getSuggestedFixCommand() {
        return suggestedFixCommand;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

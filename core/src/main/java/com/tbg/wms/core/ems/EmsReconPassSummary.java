package com.tbg.wms.core.ems;

import java.util.Objects;

/**
 * Summary information for a single pass in the reconciliation report.
 */
public final class EmsReconPassSummary {
    private final String passLabel;
    private final int passIndex;
    private final int mismatchCount;

    public EmsReconPassSummary(String passLabel, int passIndex, int mismatchCount) {
        this.passLabel = Objects.requireNonNull(passLabel, "passLabel cannot be null");
        this.passIndex = passIndex;
        this.mismatchCount = mismatchCount;
    }

    public String getPassLabel() {
        return passLabel;
    }

    public int getPassIndex() {
        return passIndex;
    }

    public int getMismatchCount() {
        return mismatchCount;
    }
}

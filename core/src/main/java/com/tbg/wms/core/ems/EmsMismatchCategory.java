package com.tbg.wms.core.ems;

/**
 * High-level mismatch classes derived from the daily EMS reconciliation report.
 */
public enum EmsMismatchCategory {
    EMS_ONLY,
    WMS_ONLY,
    LOCATION_DRIFT,
    MIXED_SKU,
    REINDUCT_CANDIDATE,
    UNKNOWN
}

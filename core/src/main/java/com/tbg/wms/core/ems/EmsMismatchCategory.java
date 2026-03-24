/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.core.ems;

/**
 * High-level mismatch classes derived from the daily EMS reconciliation report.
 *
 * <p>These categories are intentionally coarse so operator-facing reports can group likely causes
 * without implying that an automated correction is always safe.</p>
 */
public enum EmsMismatchCategory {
    EMS_ONLY,
    WMS_ONLY,
    LOCATION_DRIFT,
    MIXED_SKU,
    REINDUCT_CANDIDATE,
    UNKNOWN
}

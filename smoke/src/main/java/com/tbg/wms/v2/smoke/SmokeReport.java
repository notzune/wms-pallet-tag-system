package com.tbg.wms.v2.smoke;

import java.util.List;

/**
 * Carries smoke report data across WMS 2.0 module boundaries.
 *
 * @param mode the mode.
 * @param results the results.
 */
public record SmokeReport(SmokeMode mode, List<SmokeScenarioResult> results) {
    public SmokeReport {
        results = List.copyOf(results);
    }

    /**
     * Returns whether every smoke scenario passed.
     *
     * @return {@code true} when every scenario passed
     */
    public boolean passed() {
        return results.stream().allMatch(SmokeScenarioResult::passed);
    }
}

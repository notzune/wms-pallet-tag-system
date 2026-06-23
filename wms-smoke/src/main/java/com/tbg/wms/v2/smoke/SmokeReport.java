package com.tbg.wms.v2.smoke;

import java.util.List;

public record SmokeReport(SmokeMode mode, List<SmokeScenarioResult> results) {
    public SmokeReport {
        results = List.copyOf(results);
    }

    public boolean passed() {
        return results.stream().allMatch(SmokeScenarioResult::passed);
    }
}

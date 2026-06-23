package com.tbg.wms.v2.smoke;

public record SmokeScenarioResult(String scenario, boolean passed, int exitCode, String output) {
    public static SmokeScenarioResult pass(String scenario, String output) {
        return new SmokeScenarioResult(scenario, true, 0, output);
    }

    public static SmokeScenarioResult fail(String scenario, int exitCode, String output) {
        return new SmokeScenarioResult(scenario, false, exitCode, output);
    }
}

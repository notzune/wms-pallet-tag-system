package com.tbg.wms.v2.smoke;

/**
 * Carries the outcome of one smoke scenario execution.
 *
 * @param scenario scenario name
 * @param passed whether the scenario passed
 * @param exitCode process exit code observed for the scenario
 * @param output captured scenario output
 */
public record SmokeScenarioResult(String scenario, boolean passed, int exitCode, String output) {
    /**
     * Creates a passing smoke scenario result.
     *
     * @param scenario scenario name
     * @param output captured scenario output
     * @return passing result
     */
    public static SmokeScenarioResult pass(String scenario, String output) {
        return new SmokeScenarioResult(scenario, true, 0, output);
    }

    /**
     * Creates a failing smoke scenario result.
     *
     * @param scenario scenario name
     * @param exitCode process exit code observed for the scenario
     * @param output captured scenario output
     * @return failing result
     */
    public static SmokeScenarioResult fail(String scenario, int exitCode, String output) {
        return new SmokeScenarioResult(scenario, false, exitCode, output);
    }
}

package com.tbg.wms.v2.smoke;

import java.util.List;
import java.util.Set;

/**
 * Carries smoke scenario data across WMS 2.0 module boundaries.
 *
 * @param name the name.
 * @param tier the tier.
 * @param kind the kind.
 * @param modes the modes.
 * @param command the command.
 * @param expectedExitCode the expected exit code.
 * @param expectedArtifacts the expected artifacts.
 * @param releaseOnly the release only.
 * @param expectedConfigSourcePattern the expected config source pattern.
 */
public record SmokeScenario(
        String name,
        int tier,
        String kind,
        Set<SmokeMode> modes,
        String command,
        int expectedExitCode,
        List<String> expectedArtifacts,
        boolean releaseOnly,
        String expectedConfigSourcePattern
) {
    public SmokeScenario {
        modes = Set.copyOf(modes);
        expectedArtifacts = List.copyOf(expectedArtifacts);
    }
}

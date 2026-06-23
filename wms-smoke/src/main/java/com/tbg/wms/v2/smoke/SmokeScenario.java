package com.tbg.wms.v2.smoke;

import java.util.List;
import java.util.Set;

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

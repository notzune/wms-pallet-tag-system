package com.tbg.wms.core.update;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdatePolicyServiceTest {

    @Test
    void evaluate_shouldMarkSameLineSingleStableReleaseBehindAsRecommendedOnly() {
        UpdateCatalogService.ReleaseCatalog catalog = new UpdateCatalogService.ReleaseCatalog(
                "1.7.1",
                List.of(
                        stable("1.7.2"),
                        stable("1.7.1")
                ),
                List.of(
                        stable("1.7.2"),
                        stable("1.7.1")
                ),
                List.of(),
                stable("1.7.2"),
                null
        );

        UpdatePolicyService.UpdateDecision decision = new UpdatePolicyService().evaluate(catalog, false);

        assertEquals(UpdatePolicyService.UpdateSeverity.RECOMMENDED, decision.severity());
        assertEquals(1, decision.stableUpdatesBehind());
        assertTrue(decision.updateAvailable());
        assertFalse(decision.hardPromptRequired());
        assertEquals("1.7.2", decision.latestStableVersion());
        assertEquals("", decision.latestExperimentalVersion());
    }

    @Test
    void evaluate_shouldRequireHardPromptWhenNewerStableLineExists() {
        UpdateCatalogService.ReleaseCatalog catalog = new UpdateCatalogService.ReleaseCatalog(
                "1.7.1",
                List.of(
                        stable("1.8.0"),
                        stable("1.7.2"),
                        stable("1.7.1")
                ),
                List.of(
                        stable("1.8.0"),
                        stable("1.7.2"),
                        stable("1.7.1")
                ),
                List.of(prerelease("1.8.0-rc1")),
                stable("1.8.0"),
                prerelease("1.8.0-rc1")
        );

        UpdatePolicyService.UpdateDecision decision = new UpdatePolicyService().evaluate(catalog, true);

        assertEquals(UpdatePolicyService.UpdateSeverity.REQUIRED, decision.severity());
        assertEquals(2, decision.stableUpdatesBehind());
        assertTrue(decision.updateAvailable());
        assertTrue(decision.hardPromptRequired());
        assertEquals("1.8.0", decision.latestStableVersion());
        assertEquals("1.8.0-rc1", decision.latestExperimentalVersion());
    }

    private static UpdateCatalogService.ReleaseEntry stable(String version) {
        return release(version, false);
    }

    private static UpdateCatalogService.ReleaseEntry prerelease(String version) {
        return release(version, true);
    }

    private static UpdateCatalogService.ReleaseEntry release(String version, boolean prerelease) {
        return new UpdateCatalogService.ReleaseEntry(
                version,
                "v" + version,
                "v" + version,
                "https://example.test/releases/v" + version,
                prerelease,
                false,
                List.of(new ReleaseCheckService.ReleaseAsset(
                        "WMS.Pallet.Tag.System-" + version + ".exe",
                        "https://example.test/releases/v" + version + "/installer.exe"))
        );
    }
}

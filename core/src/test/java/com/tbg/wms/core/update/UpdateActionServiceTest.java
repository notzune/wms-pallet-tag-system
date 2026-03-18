package com.tbg.wms.core.update;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateActionServiceTest {

    @Test
    void selectTargets_shouldIncludeOlderStableVersionsWhenInstallerAndChecksumExist() {
        UpdateCatalogService.ReleaseCatalog catalog = new UpdateCatalogService.ReleaseCatalog(
                "1.7.1",
                List.of(
                        release("1.7.2", false, true),
                        release("1.7.1", false, true),
                        release("1.7.0", false, true),
                        release("1.6.9", false, false)
                ),
                List.of(
                        release("1.7.2", false, true),
                        release("1.7.1", false, true),
                        release("1.7.0", false, true),
                        release("1.6.9", false, false)
                ),
                List.of(),
                release("1.7.2", false, true),
                null
        );

        List<UpdateActionService.InstallTarget> targets = new UpdateActionService().selectInstallTargets(catalog);

        assertEquals(List.of("1.7.2", "1.7.1", "1.7.0"),
                targets.stream().map(UpdateActionService.InstallTarget::version).toList());
    }

    @Test
    void warningPolicy_shouldFlagPrereleaseAndDowngradeTargets() {
        UpdateActionService service = new UpdateActionService();

        UpdateActionService.TargetWarning prereleaseWarning =
                service.buildWarning("1.7.1", release("1.7.2-rc1", true, true));
        UpdateActionService.TargetWarning downgradeWarning =
                service.buildWarning("1.7.1", release("1.7.0", false, true));

        assertTrue(prereleaseWarning.requiresConfirmation());
        assertTrue(prereleaseWarning.experimentalTarget());
        assertTrue(downgradeWarning.requiresConfirmation());
        assertTrue(downgradeWarning.downgradeTarget());
    }

    private static UpdateCatalogService.ReleaseEntry release(String version, boolean prerelease, boolean includeChecksum) {
        List<ReleaseCheckService.ReleaseAsset> assets = includeChecksum
                ? List.of(
                new ReleaseCheckService.ReleaseAsset(
                        "WMS.Pallet.Tag.System-" + version + ".exe",
                        "https://example.test/releases/v" + version + "/installer.exe"),
                new ReleaseCheckService.ReleaseAsset(
                        "WMS.Pallet.Tag.System-" + version + ".exe.sha256",
                        "https://example.test/releases/v" + version + "/installer.exe.sha256"))
                : List.of(new ReleaseCheckService.ReleaseAsset(
                "WMS.Pallet.Tag.System-" + version + ".exe",
                "https://example.test/releases/v" + version + "/installer.exe"));
        return new UpdateCatalogService.ReleaseEntry(
                version,
                "v" + version,
                "v" + version,
                "https://example.test/releases/v" + version,
                prerelease,
                false,
                assets
        );
    }
}

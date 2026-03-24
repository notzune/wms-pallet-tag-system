package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.ReleaseCheckService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiUpdateExecutionSupportTest {

    private final GuiUpdateExecutionSupport support = new GuiUpdateExecutionSupport(new GuiUpdateFlowSupport());

    @Test
    void buildCheckOutcomes_shouldProvideStatusAndErrorMessages() {
        GuiUpdateExecutionSupport.CheckCompletionOutcome completion =
                support.buildCheckCompletion(releaseInfo(true, List.of(installerAsset(), checksumAsset())));
        GuiUpdateExecutionSupport.FailureOutcome failure =
                support.buildCheckFailure(new IllegalStateException("top", new IllegalArgumentException("root")));

        assertEquals("Update available: 1.7.6 (guided install ready)", completion.statusMessage());
        assertEquals("Update available: 1.7.6", completion.tooltip());
        assertTrue(completion.updateAvailable());
        assertEquals("Update check failed.", failure.statusMessage());
        assertEquals("Update check failed: root", failure.errorMessage());
    }

    @Test
    void resolvePromptAction_shouldFollowUpdateFlowPolicy() {
        ReleaseCheckService.ReleaseInfo browserOnly = releaseInfo(true, List.of(installerAsset()));
        ReleaseCheckService.ReleaseInfo guided = releaseInfo(true, List.of(installerAsset(), checksumAsset()));

        assertEquals(GuiUpdateExecutionSupport.PromptAction.OPEN_RELEASE_PAGE, support.resolvePromptAction(browserOnly, 0));
        assertEquals(GuiUpdateExecutionSupport.PromptAction.GUIDED_UPGRADE, support.resolvePromptAction(guided, 0));
        assertEquals(GuiUpdateExecutionSupport.PromptAction.OPEN_RELEASE_PAGE, support.resolvePromptAction(guided, 1));
        assertEquals(GuiUpdateExecutionSupport.PromptAction.NONE, support.resolvePromptAction(guided, 2));
    }

    @Test
    void planGuidedUpgrade_shouldFallbackWithoutInstallScript() {
        GuiUpdateExecutionSupport.GuidedUpgradePlan fallback = support.planGuidedUpgrade(
                releaseInfo(true, List.of(installerAsset(), checksumAsset())),
                null
        );
        GuiUpdateExecutionSupport.GuidedUpgradePlan download = support.planGuidedUpgrade(
                releaseInfo(true, List.of(installerAsset(), checksumAsset())),
                Path.of("install.ps1")
        );

        assertTrue(fallback.openReleasePageFallback());
        assertEquals("https://example.com/releases/latest", fallback.releaseUrl());
        assertFalse(download.openReleasePageFallback());
        assertEquals(Path.of("install.ps1"), download.installScript());
        assertEquals("Downloading update 1.7.6...", download.busyMessage());
    }

    @Test
    void planReleasePageOpen_shouldValidateInputs() {
        assertEquals("Release download URL is unavailable.", support.planReleasePageOpen(null, true).errorMessage());
        assertEquals(
                "Desktop browser launch is not supported on this machine.",
                support.planReleasePageOpen("https://example.com/releases/latest", false).errorMessage()
        );
        assertTrue(support.planReleasePageOpen("https://example.com/releases/latest", true).shouldOpenBrowser());
    }

    @Test
    void uninstallPlanning_shouldHandleMissingScriptCancelAndLaunch() {
        GuiUpdateExecutionSupport.UninstallPlan missing = support.planUninstall(null, 1);
        GuiUpdateExecutionSupport.UninstallPlan cancelled = support.planUninstall(Path.of("uninstall.ps1"), 0);
        GuiUpdateExecutionSupport.UninstallPlan launch = support.planUninstall(Path.of("uninstall.ps1"), 2);
        GuiUpdateExecutionSupport.FailureOutcome failure =
                support.buildUninstallFailure(new IllegalStateException("top", new IllegalArgumentException("root")));

        assertEquals("Uninstall script not found in the packaged app or repo scripts directory.", missing.errorMessage());
        assertFalse(cancelled.shouldLaunch());
        assertTrue(launch.shouldLaunch());
        assertTrue(launch.wipeInstallRoot());
        assertEquals("Failed to launch uninstall: root", failure.errorMessage());
    }

    private static ReleaseCheckService.ReleaseInfo releaseInfo(
            boolean updateAvailable,
            List<ReleaseCheckService.ReleaseAsset> assets
    ) {
        return new ReleaseCheckService.ReleaseInfo(
                "1.7.5",
                updateAvailable ? "1.7.6" : "1.7.5",
                "WMS Pallet Tag System",
                "https://example.com/releases/latest",
                assets,
                updateAvailable
        );
    }

    private static ReleaseCheckService.ReleaseAsset installerAsset() {
        return new ReleaseCheckService.ReleaseAsset(
                "WMS Pallet Tag System-1.7.6.exe",
                "https://example.com/WMS-Pallet-Tag-System-1.7.6.exe"
        );
    }

    private static ReleaseCheckService.ReleaseAsset checksumAsset() {
        return new ReleaseCheckService.ReleaseAsset(
                "WMS Pallet Tag System-1.7.6.exe.sha256",
                "https://example.com/WMS-Pallet-Tag-System-1.7.6.exe.sha256"
        );
    }
}

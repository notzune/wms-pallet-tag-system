package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.ReleaseCheckService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiUpdateFlowSupportTest {

    private final GuiUpdateFlowSupport support = new GuiUpdateFlowSupport();

    @Test
    void formatUpdateStatus_shouldReflectGuidedUpgradeAvailability() {
        assertEquals("No update check has completed yet.", support.formatUpdateStatus(null));
        assertEquals("Up to date on 1.7.5.", support.formatUpdateStatus(releaseInfo(false, List.of())));
        assertEquals("Update available: 1.7.6", support.formatUpdateStatus(releaseInfo(true, List.of(installerAsset()))));
        assertEquals(
                "Update available: 1.7.6 (guided install ready)",
                support.formatUpdateStatus(releaseInfo(true, List.of(installerAsset(), checksumAsset())))
        );
    }

    @Test
    void updatePromptRouting_shouldMatchReleaseAssets() {
        ReleaseCheckService.ReleaseInfo browserOnly = releaseInfo(true, List.of(installerAsset()));
        ReleaseCheckService.ReleaseInfo guided = releaseInfo(true, List.of(installerAsset(), checksumAsset()));

        assertArrayEquals(new Object[]{"Open Download Page", "Close"}, support.updatePromptOptions(browserOnly));
        assertArrayEquals(new Object[]{"Download and Install 1.7.6", "Open Download Page", "Close"}, support.updatePromptOptions(guided));

        assertFalse(support.shouldLaunchGuidedUpgrade(browserOnly, 0));
        assertTrue(support.shouldOpenReleasePage(browserOnly, 0));

        assertTrue(support.shouldLaunchGuidedUpgrade(guided, 0));
        assertTrue(support.shouldOpenReleasePage(guided, 1));
        assertFalse(support.shouldOpenReleasePage(guided, 0));
    }

    @Test
    void updateTooltipAndPromptMessage_shouldBeConsistent() {
        ReleaseCheckService.ReleaseInfo upToDate = releaseInfo(false, List.of());
        ReleaseCheckService.ReleaseInfo guided = releaseInfo(true, List.of(installerAsset(), checksumAsset()));

        assertNull(support.updateTooltip(upToDate));
        assertEquals("Update available: 1.7.6", support.updateTooltip(guided));
        assertTrue(support.updatePromptMessage(upToDate).contains("up to date on version 1.7.5"));
        assertTrue(support.updatePromptMessage(guided).contains("verified packaged installer is available"));
        assertTrue(support.updatePromptMessage(guided).contains("install version 1.7.6"));
        assertTrue(support.updatePromptMessage(guided).contains("relaunch automatically after success"));
    }

    @Test
    void uninstallChoiceMapping_shouldBeExplicit() {
        assertArrayEquals(new Object[]{"Cancel", "Uninstall Only", "Clean Wipe"}, support.uninstallOptions());
        assertTrue(support.uninstallMessage().contains("Clean Wipe"));
        assertFalse(support.shouldLaunchUninstall(0));
        assertTrue(support.shouldLaunchUninstall(1));
        assertTrue(support.shouldLaunchUninstall(2));
        assertFalse(support.shouldWipeInstallRoot(1));
        assertTrue(support.shouldWipeInstallRoot(2));
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

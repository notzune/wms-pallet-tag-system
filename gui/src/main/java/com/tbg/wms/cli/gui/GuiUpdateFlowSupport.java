/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.ReleaseAssetSupport;
import com.tbg.wms.core.update.ReleaseCheckService;

import java.util.Objects;

/**
 * Shared policy helpers for GUI update and uninstall flows.
 */
final class GuiUpdateFlowSupport {

    private static final Object[] GUIDED_UPDATE_OPTIONS = {"Download and Install", "Open Download Page", "Close"};
    private static final Object[] DOWNLOAD_PAGE_OPTIONS = {"Open Download Page", "Close"};
    private static final Object[] UNINSTALL_OPTIONS = {"Cancel", "Uninstall Only", "Clean Wipe"};

    boolean isGuidedUpgradeReady(ReleaseCheckService.ReleaseInfo releaseInfo) {
        Objects.requireNonNull(releaseInfo, "releaseInfo cannot be null");
        ReleaseCheckService.ReleaseAsset installerAsset = releaseInfo.preferredInstallerAsset();
        return installerAsset != null
                && ReleaseAssetSupport.findChecksumAsset(releaseInfo.assets(), installerAsset) != null;
    }

    String formatUpdateStatus(ReleaseCheckService.ReleaseInfo releaseInfo) {
        if (releaseInfo == null) {
            return "No update check has completed yet.";
        }
        if (!releaseInfo.updateAvailable()) {
            return "Up to date on " + releaseInfo.currentVersion() + ".";
        }
        return isGuidedUpgradeReady(releaseInfo)
                ? "Update available: " + releaseInfo.latestVersion() + " (guided install ready)"
                : "Update available: " + releaseInfo.latestVersion();
    }

    String updateTooltip(ReleaseCheckService.ReleaseInfo releaseInfo) {
        return releaseInfo != null && releaseInfo.updateAvailable()
                ? "Update available: " + releaseInfo.latestVersion()
                : null;
    }

    Object[] updatePromptOptions(ReleaseCheckService.ReleaseInfo releaseInfo) {
        return isGuidedUpgradeReady(releaseInfo)
                ? GUIDED_UPDATE_OPTIONS.clone()
                : DOWNLOAD_PAGE_OPTIONS.clone();
    }

    String updatePromptMessage(ReleaseCheckService.ReleaseInfo releaseInfo) {
        Objects.requireNonNull(releaseInfo, "releaseInfo cannot be null");
        if (!releaseInfo.updateAvailable()) {
            return "You are up to date on version " + releaseInfo.currentVersion() + ".";
        }
        return "Current version: " + releaseInfo.currentVersion()
                + "\nLatest version: " + releaseInfo.latestVersion()
                + (!isGuidedUpgradeReady(releaseInfo)
                ? "\n\nA verified guided upgrade is unavailable because the published release does not include both the installer and its checksum. Open the latest release page now?"
                : "\n\nA verified packaged installer is available. Download it and start the upgrade now?");
    }

    boolean shouldLaunchGuidedUpgrade(ReleaseCheckService.ReleaseInfo releaseInfo, int choice) {
        return isGuidedUpgradeReady(releaseInfo) && choice == 0;
    }

    boolean shouldOpenReleasePage(ReleaseCheckService.ReleaseInfo releaseInfo, int choice) {
        return (!isGuidedUpgradeReady(releaseInfo) && choice == 0)
                || (isGuidedUpgradeReady(releaseInfo) && choice == 1);
    }

    Object[] uninstallOptions() {
        return UNINSTALL_OPTIONS.clone();
    }

    String uninstallMessage() {
        return "Choose uninstall mode:\n"
                + "- Uninstall Only removes the installed product.\n"
                + "- Clean Wipe also removes the install directory and runtime settings to prepare for a clean reinstall.\n\n"
                + "The app will close after launch so the uninstall can complete.";
    }

    boolean shouldLaunchUninstall(int choice) {
        return choice == 1 || choice == 2;
    }

    boolean shouldWipeInstallRoot(int choice) {
        return choice == 2;
    }
}

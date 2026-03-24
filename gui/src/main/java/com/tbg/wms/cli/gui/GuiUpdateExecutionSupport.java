/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.ReleaseCheckService;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Pure execution and messaging helpers for GUI update and uninstall flows.
 */
final class GuiUpdateExecutionSupport {

    private final GuiUpdateFlowSupport updateFlowSupport;

    GuiUpdateExecutionSupport(GuiUpdateFlowSupport updateFlowSupport) {
        this.updateFlowSupport = Objects.requireNonNull(updateFlowSupport, "updateFlowSupport cannot be null");
    }

    String alreadyInProgressMessage() {
        return "Update check already in progress...";
    }

    String checkingForUpdatesMessage() {
        return "Checking for updates...";
    }

    CheckCompletionOutcome buildCheckCompletion(ReleaseCheckService.ReleaseInfo releaseInfo) {
        Objects.requireNonNull(releaseInfo, "releaseInfo cannot be null");
        return new CheckCompletionOutcome(
                updateFlowSupport.formatUpdateStatus(releaseInfo),
                updateFlowSupport.updateTooltip(releaseInfo),
                releaseInfo.updateAvailable()
        );
    }

    FailureOutcome buildCheckFailure(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        return new FailureOutcome("Update check failed.", "Update check failed: " + GuiExceptionMessageSupport.rootMessage(throwable));
    }

    PromptAction resolvePromptAction(ReleaseCheckService.ReleaseInfo releaseInfo, int choice) {
        Objects.requireNonNull(releaseInfo, "releaseInfo cannot be null");
        if (updateFlowSupport.shouldLaunchGuidedUpgrade(releaseInfo, choice)) {
            return PromptAction.GUIDED_UPGRADE;
        }
        if (updateFlowSupport.shouldOpenReleasePage(releaseInfo, choice)) {
            return PromptAction.OPEN_RELEASE_PAGE;
        }
        return PromptAction.NONE;
    }

    GuidedUpgradePlan planGuidedUpgrade(ReleaseCheckService.ReleaseInfo releaseInfo, Path installScript) {
        Objects.requireNonNull(releaseInfo, "releaseInfo cannot be null");
        if (installScript == null) {
            return GuidedUpgradePlan.openReleasePage(releaseInfo.releaseUrl());
        }
        return GuidedUpgradePlan.downloadAndInstall(
                installScript,
                "Downloading update " + releaseInfo.latestVersion() + "..."
        );
    }

    FailureOutcome buildGuidedUpgradeFailure(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        return new FailureOutcome("Update download failed.", "Could not start guided update: " + GuiExceptionMessageSupport.rootMessage(throwable));
    }

    ReleasePagePlan planReleasePageOpen(String releaseUrl, boolean desktopBrowseSupported) {
        if (releaseUrl == null || releaseUrl.isBlank()) {
            return ReleasePagePlan.error("Release download URL is unavailable.");
        }
        if (!desktopBrowseSupported) {
            return ReleasePagePlan.error("Desktop browser launch is not supported on this machine.");
        }
        return ReleasePagePlan.open(releaseUrl);
    }

    UninstallPlan planUninstall(Path uninstallScript, int choice) {
        if (uninstallScript == null) {
            return UninstallPlan.error("Uninstall script not found in the packaged app or repo scripts directory.");
        }
        if (!updateFlowSupport.shouldLaunchUninstall(choice)) {
            return UninstallPlan.cancelled();
        }
        return UninstallPlan.launch(uninstallScript, updateFlowSupport.shouldWipeInstallRoot(choice));
    }

    FailureOutcome buildUninstallFailure(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        return new FailureOutcome("Ready.", "Failed to launch uninstall: " + GuiExceptionMessageSupport.rootMessage(throwable));
    }

    enum PromptAction {
        NONE,
        GUIDED_UPGRADE,
        OPEN_RELEASE_PAGE
    }

    record CheckCompletionOutcome(
            String statusMessage,
            String tooltip,
            boolean updateAvailable
    ) {
    }

    record FailureOutcome(
            String statusMessage,
            String errorMessage
    ) {
    }

    record GuidedUpgradePlan(
            Path installScript,
            String busyMessage,
            String releaseUrl,
            boolean openReleasePageFallback
    ) {
        static GuidedUpgradePlan openReleasePage(String releaseUrl) {
            return new GuidedUpgradePlan(null, null, releaseUrl, true);
        }

        static GuidedUpgradePlan downloadAndInstall(Path installScript, String busyMessage) {
            return new GuidedUpgradePlan(installScript, busyMessage, null, false);
        }
    }

    record ReleasePagePlan(
            String releaseUrl,
            String errorMessage
    ) {
        static ReleasePagePlan open(String releaseUrl) {
            return new ReleasePagePlan(releaseUrl, null);
        }

        static ReleasePagePlan error(String errorMessage) {
            return new ReleasePagePlan(null, errorMessage);
        }

        boolean shouldOpenBrowser() {
            return releaseUrl != null;
        }
    }

    record UninstallPlan(
            Path uninstallScript,
            boolean wipeInstallRoot,
            String errorMessage
    ) {
        static UninstallPlan launch(Path uninstallScript, boolean wipeInstallRoot) {
            return new UninstallPlan(uninstallScript, wipeInstallRoot, null);
        }

        static UninstallPlan cancelled() {
            return new UninstallPlan(null, false, null);
        }

        static UninstallPlan error(String errorMessage) {
            return new UninstallPlan(null, false, errorMessage);
        }

        boolean shouldLaunch() {
            return uninstallScript != null;
        }
    }
}

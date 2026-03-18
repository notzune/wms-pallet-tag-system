package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.UpdateActionService;
import com.tbg.wms.core.update.UpdateCatalogService;
import com.tbg.wms.core.update.UpdatePolicyService;

import java.util.List;

/**
 * Immutable GUI snapshot for the update manager surface.
 */
public record UpdateManagerSnapshot(
        UpdateCatalogService.ReleaseCatalog catalog,
        UpdatePolicyService.UpdateDecision decision,
        UpdateUiSupport.UpdateUiState uiState,
        List<UpdateActionService.InstallTarget> installTargets,
        boolean experimentalUpdatesEnabled
) {
    public UpdateManagerSnapshot {
        installTargets = List.copyOf(installTargets);
    }

    public String currentVersion() {
        return decision == null ? "" : decision.currentVersion();
    }

    public String latestStableVersion() {
        return decision == null ? "" : decision.latestStableVersion();
    }

    public String latestExperimentalVersion() {
        if (catalog == null || catalog.latestPrerelease() == null) {
            return "";
        }
        return catalog.latestPrerelease().version();
    }

    public String summary() {
        return uiState == null ? "No update check has completed yet." : uiState.summary();
    }

    public int stableUpdatesBehind() {
        return decision == null ? 0 : decision.stableUpdatesBehind();
    }

    public boolean showToolbarWarning() {
        return uiState != null && uiState.showToolbarWarning();
    }

    public boolean showStartupPrompt() {
        return uiState != null && uiState.showStartupPrompt();
    }

    public String latestStableReleaseUrl() {
        return catalog == null || catalog.latestStable() == null ? "" : catalog.latestStable().releaseUrl();
    }
}

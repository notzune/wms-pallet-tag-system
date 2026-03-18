package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.UpdatePolicyService;

/**
 * Maps update policy decisions into GUI-facing summaries and notification flags.
 */
public final class UpdateUiSupport {

    public UpdateUiState buildState(UpdatePolicyService.UpdateDecision decision, UpdatePromptStateStore promptStateStore) {
        boolean showToolbarWarning = decision != null && decision.hardPromptRequired();
        boolean showStartupPrompt = showToolbarWarning
                && promptStateStore != null
                && promptStateStore.shouldShowPrompt(decision.latestStableVersion());
        return new UpdateUiState(
                buildSummary(decision),
                showToolbarWarning,
                showStartupPrompt
        );
    }

    private String buildSummary(UpdatePolicyService.UpdateDecision decision) {
        if (decision == null || !decision.updateAvailable()) {
            String currentVersion = decision == null ? "" : decision.currentVersion();
            return currentVersion.isBlank()
                    ? "No update check has completed yet."
                    : "Up to date on " + currentVersion + ".";
        }
        if (decision.severity() == UpdatePolicyService.UpdateSeverity.RECOMMENDED) {
            int behind = Math.max(1, decision.stableUpdatesBehind());
            return behind + " update" + (behind == 1 ? "" : "s") + " behind, update recommended.";
        }
        return "New stable update available: " + decision.latestStableVersion() + ".";
    }

    public record UpdateUiState(
            String summary,
            boolean showToolbarWarning,
            boolean showStartupPrompt
    ) {
    }
}

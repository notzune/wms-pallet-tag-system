package com.tbg.wms.core.update;

/**
 * Applies product update rules to the fetched release catalog.
 */
public final class UpdatePolicyService {

    public UpdateDecision evaluate(UpdateCatalogService.ReleaseCatalog catalog, boolean experimentalUpdatesEnabled) {
        String currentVersion = catalog == null ? "" : catalog.currentVersion();
        UpdateCatalogService.ReleaseEntry latestStable = catalog == null ? null : catalog.latestStable();
        UpdateCatalogService.ReleaseEntry latestExperimental = catalog == null ? null : catalog.latestPrerelease();
        int stableUpdatesBehind = countStableUpdatesBehind(catalog, currentVersion);
        boolean updateAvailable = stableUpdatesBehind > 0;
        boolean hardPromptRequired = updateAvailable
                && latestStable != null
                && VersionSupport.compareReleaseLine(latestStable.version(), currentVersion) > 0;
        UpdateSeverity severity = !updateAvailable
                ? UpdateSeverity.CURRENT
                : (hardPromptRequired ? UpdateSeverity.REQUIRED : UpdateSeverity.RECOMMENDED);
        return new UpdateDecision(
                currentVersion,
                latestStable == null ? "" : latestStable.version(),
                experimentalUpdatesEnabled && latestExperimental != null ? latestExperimental.version() : "",
                stableUpdatesBehind,
                updateAvailable,
                hardPromptRequired,
                severity
        );
    }

    private int countStableUpdatesBehind(UpdateCatalogService.ReleaseCatalog catalog, String currentVersion) {
        if (catalog == null || currentVersion == null || currentVersion.isBlank()) {
            return 0;
        }
        int count = 0;
        for (UpdateCatalogService.ReleaseEntry release : catalog.stableReleases()) {
            if (VersionSupport.compare(release.version(), currentVersion) > 0) {
                count++;
            }
        }
        return count;
    }

    public enum UpdateSeverity {
        CURRENT,
        RECOMMENDED,
        REQUIRED
    }

    public record UpdateDecision(
            String currentVersion,
            String latestStableVersion,
            String latestExperimentalVersion,
            int stableUpdatesBehind,
            boolean updateAvailable,
            boolean hardPromptRequired,
            UpdateSeverity severity
    ) {
    }
}

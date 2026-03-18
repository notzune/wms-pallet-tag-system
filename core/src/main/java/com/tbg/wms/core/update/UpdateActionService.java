package com.tbg.wms.core.update;

import java.util.ArrayList;
import java.util.List;

/**
 * Prepares installable release targets and warning metadata for upgrade or downgrade actions.
 */
public final class UpdateActionService {

    public List<InstallTarget> selectInstallTargets(UpdateCatalogService.ReleaseCatalog catalog) {
        if (catalog == null) {
            return List.of();
        }
        List<InstallTarget> targets = new ArrayList<>();
        for (UpdateCatalogService.ReleaseEntry release : catalog.releases()) {
            ReleaseCheckService.ReleaseAsset installerAsset = release.preferredInstallerAsset();
            if (installerAsset == null) {
                continue;
            }
            ReleaseCheckService.ReleaseAsset checksumAsset = ReleaseAssetSupport.findChecksumAsset(release.assets(), installerAsset);
            if (checksumAsset == null) {
                continue;
            }
            targets.add(new InstallTarget(
                    release.version(),
                    release.tagName(),
                    release.releaseName(),
                    release.releaseUrl(),
                    release.prerelease(),
                    installerAsset,
                    checksumAsset
            ));
        }
        return List.copyOf(targets);
    }

    public TargetWarning buildWarning(String currentVersion, UpdateCatalogService.ReleaseEntry targetRelease) {
        String normalizedCurrentVersion = VersionSupport.normalize(currentVersion);
        String normalizedTargetVersion = targetRelease == null ? "" : targetRelease.version();
        boolean downgradeTarget = !normalizedCurrentVersion.isBlank()
                && !normalizedTargetVersion.isBlank()
                && VersionSupport.compare(normalizedTargetVersion, normalizedCurrentVersion) < 0;
        boolean experimentalTarget = targetRelease != null && targetRelease.prerelease();
        boolean requiresConfirmation = downgradeTarget || experimentalTarget;
        String message = !requiresConfirmation
                ? ""
                : "This target may be missing features or include unstable behavior. Continue?";
        return new TargetWarning(downgradeTarget, experimentalTarget, requiresConfirmation, message);
    }

    public TargetWarning buildWarning(String currentVersion, InstallTarget target) {
        String normalizedCurrentVersion = VersionSupport.normalize(currentVersion);
        String normalizedTargetVersion = target == null ? "" : VersionSupport.normalize(target.version());
        boolean downgradeTarget = !normalizedCurrentVersion.isBlank()
                && !normalizedTargetVersion.isBlank()
                && VersionSupport.compare(normalizedTargetVersion, normalizedCurrentVersion) < 0;
        boolean experimentalTarget = target != null && target.prerelease();
        boolean requiresConfirmation = downgradeTarget || experimentalTarget;
        String message = !requiresConfirmation
                ? ""
                : "Older or experimental versions may be missing features or behave unexpectedly. Continue?";
        return new TargetWarning(downgradeTarget, experimentalTarget, requiresConfirmation, message);
    }

    public record InstallTarget(
            String version,
            String tagName,
            String releaseName,
            String releaseUrl,
            boolean prerelease,
            ReleaseCheckService.ReleaseAsset installerAsset,
            ReleaseCheckService.ReleaseAsset checksumAsset
    ) {
    }

    public record TargetWarning(
            boolean downgradeTarget,
            boolean experimentalTarget,
            boolean requiresConfirmation,
            String message
    ) {
    }
}

package com.tbg.wms.core.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Looks up the latest published GitHub release for the app.
 */
public final class ReleaseCheckService {
    private final UpdateCatalogService updateCatalogService;

    public ReleaseCheckService() {
        this(new UpdateCatalogService());
    }

    ReleaseCheckService(UpdateCatalogService updateCatalogService) {
        this.updateCatalogService = Objects.requireNonNull(updateCatalogService, "updateCatalogService cannot be null");
    }

    public ReleaseInfo checkLatestRelease(String currentVersion) throws IOException, InterruptedException {
        UpdateCatalogService.ReleaseCatalog catalog = updateCatalogService.loadCatalog(currentVersion);
        String normalizedCurrentVersion = catalog.currentVersion();
        UpdateCatalogService.ReleaseEntry latestStable = catalog.latestStable();
        String latestVersion = latestStable == null ? "" : latestStable.version();
        String releaseName = latestStable == null ? "" : latestStable.releaseName();
        String releaseUrl = latestStable == null ? "" : latestStable.releaseUrl();
        List<ReleaseAsset> assets = latestStable == null ? List.of() : latestStable.assets();
        boolean updateAvailable = !latestVersion.isBlank()
                && !normalizedCurrentVersion.isBlank()
                && VersionSupport.compare(latestVersion, normalizedCurrentVersion) > 0;
        return new ReleaseInfo(
                normalizedCurrentVersion,
                latestVersion,
                releaseName,
                releaseUrl,
                assets,
                updateAvailable
        );
    }

    public record ReleaseInfo(
            String currentVersion,
            String latestVersion,
            String releaseName,
            String releaseUrl,
            List<ReleaseAsset> assets,
            boolean updateAvailable
    ) {
        public ReleaseAsset preferredInstallerAsset() {
            return assets.stream()
                    .filter(asset -> asset.name().endsWith(".exe"))
                    .findFirst()
                    .orElse(null);
        }
    }

    public record ReleaseAsset(
            String name,
            String downloadUrl
    ) {
    }
}

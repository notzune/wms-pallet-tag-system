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
    private static final URI LATEST_RELEASE_URI =
            URI.create("https://api.github.com/repos/notzune/wms-pallet-tag-system/releases/latest");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ReleaseCheckService() {
        this(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                new ObjectMapper());
    }

    ReleaseCheckService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public ReleaseInfo checkLatestRelease(String currentVersion) throws IOException, InterruptedException {
        String normalizedCurrentVersion = VersionSupport.normalize(currentVersion);
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_URI)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "wms-pallet-tag-system")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Release check failed with HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String latestVersion = VersionSupport.normalize(root.path("tag_name").asText(""));
        String releaseName = root.path("name").asText("");
        String releaseUrl = root.path("html_url").asText("");
        List<ReleaseAsset> assets = parseAssets(root.path("assets"));
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

    private List<ReleaseAsset> parseAssets(JsonNode assetsNode) {
        if (assetsNode == null || !assetsNode.isArray()) {
            return List.of();
        }
        List<ReleaseAsset> assets = new ArrayList<>();
        for (JsonNode assetNode : assetsNode) {
            String name = assetNode.path("name").asText("");
            String downloadUrl = assetNode.path("browser_download_url").asText("");
            if (!name.isBlank() && !downloadUrl.isBlank()) {
                assets.add(new ReleaseAsset(name, downloadUrl));
            }
        }
        return List.copyOf(assets);
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

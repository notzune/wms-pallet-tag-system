package com.tbg.wms.core.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and classifies the published GitHub release catalog for update decisions.
 */
public final class UpdateCatalogService {
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final int MAX_RELEASE_PAGES = 10;
    private static final URI RELEASES_URI =
            URI.create("https://api.github.com/repos/notzune/wms-pallet-tag-system/releases?per_page=100");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UpdateCatalogService() {
        this(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                new ObjectMapper());
    }

    UpdateCatalogService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public ReleaseCatalog loadCatalog(String currentVersion) throws IOException, InterruptedException {
        List<ReleaseEntry> releases = new ArrayList<>();
        URI nextUri = RELEASES_URI;
        int pagesLoaded = 0;
        while (nextUri != null) {
            pagesLoaded++;
            if (pagesLoaded > MAX_RELEASE_PAGES) {
                throw new IOException("Release catalog exceeded the max page limit of " + MAX_RELEASE_PAGES + ".");
            }

            HttpResponse<String> response = sendCatalogRequest(nextUri);
            JsonNode root = objectMapper.readTree(response.body());
            if (root == null || !root.isArray()) {
                throw new IOException("Release catalog response did not contain a release array.");
            }

            for (JsonNode releaseNode : root) {
                ReleaseEntry parsed = parseRelease(releaseNode);
                if (parsed != null) {
                    releases.add(parsed);
                }
            }

            nextUri = parseNextLink(response.headers()).orElse(null);
        }
        releases.sort(Comparator.comparing(ReleaseEntry::version, VersionSupport::compare).reversed());

        List<ReleaseEntry> stableReleases = releases.stream()
                .filter(release -> !release.prerelease())
                .toList();
        List<ReleaseEntry> prereleaseReleases = releases.stream()
                .filter(ReleaseEntry::prerelease)
                .toList();
        return new ReleaseCatalog(
                VersionSupport.normalize(currentVersion),
                List.copyOf(releases),
                stableReleases,
                prereleaseReleases,
                stableReleases.isEmpty() ? null : stableReleases.get(0),
                prereleaseReleases.isEmpty() ? null : prereleaseReleases.get(0)
        );
    }

    private HttpResponse<String> sendCatalogRequest(URI requestUri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "wms-pallet-tag-system")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Release catalog check failed with HTTP " + response.statusCode());
        }
        return response;
    }

    private Optional<URI> parseNextLink(HttpHeaders headers) {
        if (headers == null) {
            return Optional.empty();
        }
        String linkHeader = headers.firstValue("link").orElse("");
        if (linkHeader.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(URI.create(matcher.group(1)));
    }

    private ReleaseEntry parseRelease(JsonNode releaseNode) {
        if (releaseNode == null || releaseNode.isMissingNode()) {
            return null;
        }
        boolean draft = releaseNode.path("draft").asBoolean(false);
        String version = VersionSupport.normalize(releaseNode.path("tag_name").asText(""));
        if (draft || version.isBlank()) {
            return null;
        }
        boolean prerelease = releaseNode.path("prerelease").asBoolean(false) || VersionSupport.isPrerelease(version);
        return new ReleaseEntry(
                version,
                releaseNode.path("tag_name").asText(""),
                releaseNode.path("name").asText(""),
                releaseNode.path("html_url").asText(""),
                prerelease,
                draft,
                parseAssets(releaseNode.path("assets"))
        );
    }

    private List<ReleaseCheckService.ReleaseAsset> parseAssets(JsonNode assetsNode) {
        if (assetsNode == null || !assetsNode.isArray()) {
            return List.of();
        }
        List<ReleaseCheckService.ReleaseAsset> assets = new ArrayList<>();
        for (JsonNode assetNode : assetsNode) {
            String name = assetNode.path("name").asText("");
            String downloadUrl = assetNode.path("browser_download_url").asText("");
            if (!name.isBlank() && !downloadUrl.isBlank()) {
                assets.add(new ReleaseCheckService.ReleaseAsset(name, downloadUrl));
            }
        }
        return List.copyOf(assets);
    }

    public record ReleaseCatalog(
            String currentVersion,
            List<ReleaseEntry> releases,
            List<ReleaseEntry> stableReleases,
            List<ReleaseEntry> prereleaseReleases,
            ReleaseEntry latestStable,
            ReleaseEntry latestPrerelease
    ) {
        public ReleaseCatalog {
            currentVersion = VersionSupport.normalize(currentVersion);
            releases = List.copyOf(releases);
            stableReleases = List.copyOf(stableReleases);
            prereleaseReleases = List.copyOf(prereleaseReleases);
        }
    }

    public record ReleaseEntry(
            String version,
            String tagName,
            String releaseName,
            String releaseUrl,
            boolean prerelease,
            boolean draft,
            List<ReleaseCheckService.ReleaseAsset> assets
    ) {
        public ReleaseEntry {
            version = VersionSupport.normalize(version);
            assets = List.copyOf(assets);
        }

        public ReleaseCheckService.ReleaseAsset preferredInstallerAsset() {
            return assets.stream()
                    .filter(asset -> asset.name().endsWith(".exe"))
                    .findFirst()
                    .orElse(null);
        }
    }
}

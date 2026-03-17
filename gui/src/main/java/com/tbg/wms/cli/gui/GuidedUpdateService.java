package com.tbg.wms.cli.gui;

import com.tbg.wms.core.RuntimePathResolver;
import com.tbg.wms.core.update.ReleaseCheckService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Downloads published installer assets and launches the packaged installer helper.
 */
public final class GuidedUpdateService {
    private final HttpClient httpClient;

    public GuidedUpdateService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    GuidedUpdateService(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    }

    public Path downloadInstaller(Class<?> anchorType, ReleaseCheckService.ReleaseAsset asset)
            throws IOException, InterruptedException {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Objects.requireNonNull(asset, "asset cannot be null");

        Path updatesDir = RuntimePathResolver.resolveAppHome(anchorType).resolve("updates");
        Files.createDirectories(updatesDir);
        Path target = updatesDir.resolve(asset.name());

        HttpRequest request = HttpRequest.newBuilder(URI.create(asset.downloadUrl()))
                .timeout(Duration.ofMinutes(3))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "wms-pallet-tag-system")
                .GET()
                .build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Installer download failed with HTTP " + response.statusCode());
        }
        return target;
    }
}

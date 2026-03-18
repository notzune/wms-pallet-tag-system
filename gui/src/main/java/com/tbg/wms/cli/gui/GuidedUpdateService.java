package com.tbg.wms.cli.gui;

import com.tbg.wms.core.RuntimePathResolver;
import com.tbg.wms.core.update.ReleaseAssetSupport;
import com.tbg.wms.core.update.ReleaseCheckService;
import com.tbg.wms.core.update.UpdateActionService;

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
 * Downloads published installer assets for verified guided upgrades.
 *
 * <p>The service intentionally does not launch installers directly or decide update eligibility.
 * It only handles transport and integrity prerequisites, including cleanup of partial or failed
 * downloads so later update attempts start from a known-good state.</p>
 */
public final class GuidedUpdateService {
    private final HttpClient httpClient;
    private final InstallerVerificationService installerVerificationService;

    public GuidedUpdateService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    GuidedUpdateService(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.installerVerificationService = new InstallerVerificationService(httpClient);
    }

    public Path downloadInstaller(Class<?> anchorType, ReleaseCheckService.ReleaseInfo releaseInfo)
            throws IOException, InterruptedException {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Objects.requireNonNull(releaseInfo, "releaseInfo cannot be null");
        ReleaseCheckService.ReleaseAsset installerAsset = releaseInfo.preferredInstallerAsset();
        if (installerAsset == null) {
            throw new IOException("No installer asset is available for guided upgrade.");
        }
        ReleaseCheckService.ReleaseAsset checksumAsset =
                ReleaseAssetSupport.findChecksumAsset(releaseInfo.assets(), installerAsset);
        if (checksumAsset == null) {
            throw new IOException("No published checksum is available for the installer asset.");
        }

        Path updatesDir = RuntimePathResolver.resolveAppHome(anchorType).resolve("updates");
        Files.createDirectories(updatesDir);
        Path target = updatesDir.resolve(installerAsset.name());

        HttpRequest request = HttpRequest.newBuilder(URI.create(installerAsset.downloadUrl()))
                .timeout(Duration.ofMinutes(3))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "wms-pallet-tag-system")
                .GET()
                .build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(target);
            throw new IOException("Installer download failed with HTTP " + response.statusCode());
        }
        try {
            installerVerificationService.verifyInstaller(target, checksumAsset.downloadUrl());
            return target;
        } catch (Exception ex) {
            Files.deleteIfExists(target);
            throw ex;
        }
    }

    public Path downloadInstaller(Class<?> anchorType, UpdateActionService.InstallTarget installTarget)
            throws IOException, InterruptedException {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Objects.requireNonNull(installTarget, "installTarget cannot be null");
        return downloadInstaller(anchorType, installTarget.installerAsset(), installTarget.checksumAsset());
    }

    private Path downloadInstaller(
            Class<?> anchorType,
            ReleaseCheckService.ReleaseAsset installerAsset,
            ReleaseCheckService.ReleaseAsset checksumAsset
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Objects.requireNonNull(installerAsset, "installerAsset cannot be null");
        Objects.requireNonNull(checksumAsset, "checksumAsset cannot be null");

        Path updatesDir = RuntimePathResolver.resolveAppHome(anchorType).resolve("updates");
        Files.createDirectories(updatesDir);
        Path target = updatesDir.resolve(installerAsset.name());

        HttpRequest request = HttpRequest.newBuilder(URI.create(installerAsset.downloadUrl()))
                .timeout(Duration.ofMinutes(3))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "wms-pallet-tag-system")
                .GET()
                .build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(target);
            throw new IOException("Installer download failed with HTTP " + response.statusCode());
        }
        try {
            installerVerificationService.verifyInstaller(target, checksumAsset.downloadUrl());
            return target;
        } catch (Exception ex) {
            Files.deleteIfExists(target);
            throw ex;
        }
    }
}

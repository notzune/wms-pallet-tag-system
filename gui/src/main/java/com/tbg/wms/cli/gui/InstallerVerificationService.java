package com.tbg.wms.cli.gui;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Verifies downloaded installer assets against published SHA-256 checksum sidecars.
 *
 * <p>Hashing is streamed from disk instead of loading the whole installer into memory so large
 * packaged releases can be verified without avoidable heap spikes.</p>
 */
public final class InstallerVerificationService {
    private final HttpClient httpClient;

    public InstallerVerificationService(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    }

    public void verifyInstaller(Path installerPath, String checksumUrl) throws IOException, InterruptedException {
        Objects.requireNonNull(installerPath, "installerPath cannot be null");
        if (checksumUrl == null || checksumUrl.isBlank()) {
            throw new IOException("Checksum URL is required for guided installer verification.");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(checksumUrl))
                .header("Accept", "text/plain")
                .header("User-Agent", "wms-pallet-tag-system")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Checksum download failed with HTTP " + response.statusCode());
        }

        String expectedSha256 = extractExpectedHash(response.body());
        String actualSha256 = calculateSha256(installerPath);
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            throw new IOException("Downloaded installer checksum did not match published SHA-256.");
        }
    }

    private String extractExpectedHash(String checksumBody) throws IOException {
        String normalized = Objects.requireNonNullElse(checksumBody, "").trim();
        if (normalized.isEmpty()) {
            throw new IOException("Published checksum file was empty.");
        }
        String firstToken = normalized.split("\\s+")[0].trim().toLowerCase(Locale.ROOT);
        if (!firstToken.matches("[0-9a-f]{64}")) {
            throw new IOException("Published checksum file did not contain a valid SHA-256 hash.");
        }
        return firstToken;
    }

    private String calculateSha256(Path installerPath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream in = Files.newInputStream(installerPath)) {
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ex) {
            throw new IOException("Failed to calculate installer SHA-256.", ex);
        }
    }
}

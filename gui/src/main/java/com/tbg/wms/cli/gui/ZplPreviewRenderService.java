package com.tbg.wms.cli.gui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Renders ZPL through a remote preview service.
 *
 * <p>Default implementation targets the Labelary API because it renders full ZPL features
 * such as orientation, lines, graphics, and margins to a real image preview.</p>
 */
final class ZplPreviewRenderService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private final HttpClient httpClient;
    private final String baseUrl;

    ZplPreviewRenderService() {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
                System.getProperty("wms.tags.zplPreviewBaseUrl", "https://api.labelary.com"));
    }

    ZplPreviewRenderService(HttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    BufferedImage render(String zpl, int dpmm, double widthInches, double heightInches, int labelIndex)
            throws IOException, InterruptedException {
        validate(zpl, dpmm, widthInches, heightInches, labelIndex);
        HttpRequest request = HttpRequest.newBuilder(buildRenderUri(dpmm, widthInches, heightInches, labelIndex))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "image/png")
                .POST(HttpRequest.BodyPublishers.ofString(zpl, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            String message = new String(response.body(), StandardCharsets.UTF_8);
            throw new IOException("Preview render failed (" + response.statusCode() + "): " + message);
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
        if (image == null) {
            throw new IOException("Preview render returned unreadable image data.");
        }
        return image;
    }

    URI buildRenderUri(int dpmm, double widthInches, double heightInches, int labelIndex) {
        validateDimensions(dpmm, widthInches, heightInches, labelIndex);
        String width = normalizeDecimal(widthInches);
        String height = normalizeDecimal(heightInches);
        String path = String.format(Locale.ROOT,
                "%s/v1/printers/%ddpmm/labels/%sx%s/%d/",
                trimTrailingSlash(baseUrl),
                dpmm,
                URLEncoder.encode(width, StandardCharsets.UTF_8),
                URLEncoder.encode(height, StandardCharsets.UTF_8),
                labelIndex);
        return URI.create(path);
    }

    private void validate(String zpl, int dpmm, double widthInches, double heightInches, int labelIndex) {
        if (zpl == null || zpl.isBlank()) {
            throw new IllegalArgumentException("ZPL content is required.");
        }
        validateDimensions(dpmm, widthInches, heightInches, labelIndex);
    }

    private void validateDimensions(int dpmm, double widthInches, double heightInches, int labelIndex) {
        if (dpmm <= 0) {
            throw new IllegalArgumentException("DPMM must be > 0.");
        }
        if (widthInches <= 0.0d || heightInches <= 0.0d) {
            throw new IllegalArgumentException("Label dimensions must be > 0.");
        }
        if (labelIndex < 0) {
            throw new IllegalArgumentException("Label index must be >= 0.");
        }
    }

    private static String normalizeDecimal(double value) {
        String text = String.format(Locale.ROOT, "%.3f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

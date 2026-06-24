package com.tbg.wms.v2.app.barcode;

import com.tbg.wms.v2.domain.barcode.BarcodePreset;
import com.tbg.wms.v2.domain.barcode.BarcodeRequest;

import java.util.Locale;
import java.util.Objects;

/**
 * Application use case for generating standalone barcode label ZPL.
 */
public final class GenerateBarcodeLabel {
    private static final int MAX_ARTIFACT_SLUG_LENGTH = 64;

    private final Renderer renderer;

    /**
     * Creates a barcode-label generation use case.
     *
     * @param renderer the renderer.
     */
    public GenerateBarcodeLabel(Renderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Generates the requested barcode label artifact.
     *
     * @param request the request.
     * @return generated barcode label
     */
    public BarcodeLabel generate(BarcodeRequest request) {
        Objects.requireNonNull(request, "request");
        return new BarcodeLabel("barcode-" + safeSlug(request.data()) + ".zpl", renderer.render(request));
    }

    /**
     * Generates the requested barcode label artifact.
     *
     * @param preset the preset.
     * @return generated barcode label
     */
    public BarcodeLabel generate(BarcodePreset preset) {
        Objects.requireNonNull(preset, "preset");
        return new BarcodeLabel("barcode-" + preset.fileSlug() + ".zpl", renderer.render(preset.toRequest()));
    }

    /**
     * Defines the contract for renderer behavior in WMS 2.0 workflows.
     */
    public interface Renderer {
        /**
         * Renders the requested output.
         *
         * @param request the request.
         * @return the rendered output path.
         */
        String render(BarcodeRequest request);
    }

    private static String safeSlug(String value) {
        String source = value == null ? "" : value.trim();
        if (source.isEmpty()) {
            source = "data";
        }
        String slug = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        if (slug.isEmpty()) {
            slug = "data";
        }
        return slug.length() <= MAX_ARTIFACT_SLUG_LENGTH ? slug : slug.substring(0, MAX_ARTIFACT_SLUG_LENGTH);
    }
}

package com.tbg.wms.v2.desktop.zplpreview;

import java.util.Objects;

/**
 * Tracks ZPL preview input, rendering status, and the latest rendered preview description.
 */
public final class ZplPreviewViewModel {
    private final Renderer renderer;
    private String zpl = "";
    private String lastPreviewDescription = "";
    private String status = "Paste ZPL to preview.";

    /**
     * Creates a ZPL preview view model.
     *
     * @param renderer renderer used to turn ZPL into a preview description
     */
    public ZplPreviewViewModel(Renderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Updates the ZPL source text.
     *
     */
    public void updateZpl(String zpl) {
        this.zpl = zpl == null ? "" : zpl;
        status = this.zpl.isBlank() ? "Paste ZPL to preview." : "Preview changed.";
    }

    /**
     * Renders the current ZPL preview.
     */
    public void render() {
        if (zpl.isBlank()) {
            throw new IllegalStateException("ZPL is required.");
        }
        lastPreviewDescription = renderer.render(zpl);
        status = "Rendered preview image.";
    }

    /**
     * Returns the latest preview description produced by the renderer.
     *
     * @return latest preview description, or an empty string before rendering
     */
    public String lastPreviewDescription() {
        return lastPreviewDescription;
    }

    /**
     * Returns the current ZPL preview status.
     *
     * @return current status message
     */
    public String status() {
        return status;
    }

    /**
     * Defines the renderer contract for ZPL preview generation.
     */
    public interface Renderer {
        /**
         * Renders ZPL into a preview description.
         *
         * @param zpl the zpl.
         * @return preview description
         */
        String render(String zpl);
    }
}

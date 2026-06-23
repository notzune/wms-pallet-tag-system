package com.tbg.wms.v2.desktop.zplpreview;

import java.util.Objects;

public final class ZplPreviewViewModel {
    private final Renderer renderer;
    private String zpl = "";
    private String lastPreviewDescription = "";
    private String status = "Paste ZPL to preview.";

    public ZplPreviewViewModel(Renderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public void updateZpl(String zpl) {
        this.zpl = zpl == null ? "" : zpl;
        status = this.zpl.isBlank() ? "Paste ZPL to preview." : "Preview changed.";
    }

    public void render() {
        if (zpl.isBlank()) {
            throw new IllegalStateException("ZPL is required.");
        }
        lastPreviewDescription = renderer.render(zpl);
        status = "Rendered preview image.";
    }

    public String lastPreviewDescription() {
        return lastPreviewDescription;
    }

    public String status() {
        return status;
    }

    public interface Renderer {
        String render(String zpl);
    }
}

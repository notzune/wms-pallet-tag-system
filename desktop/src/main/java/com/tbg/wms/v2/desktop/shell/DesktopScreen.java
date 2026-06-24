package com.tbg.wms.v2.desktop.shell;

/**
 * Carries desktop screen data across WMS 2.0 module boundaries.
 *
 * @param id the id.
 * @param title the title.
 */
public record DesktopScreen(String id, String title) {
    public DesktopScreen {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("screen id is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("screen title is required");
        }
        id = id.trim();
        title = title.trim();
    }
}

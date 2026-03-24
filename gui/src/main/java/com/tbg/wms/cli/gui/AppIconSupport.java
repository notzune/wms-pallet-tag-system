/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the bundled application window icons in descending size order.
 *
 * <p>This remains separate from frame setup so icon-resource handling stays best-effort and does
 * not add image-loading noise to the main GUI shell.</p>
 */
final class AppIconSupport {

    private static final String[] ICON_RESOURCE_PATHS = {
            "/icons/orange-slice-256.png",
            "/icons/orange-slice-64.png",
            "/icons/orange-slice-32.png"
    };

    private AppIconSupport() {
    }

    /**
     * Loads all available window icons from bundled GUI resources.
     *
     * @return immutable icon list, empty when no icon resource can be loaded
     */
    static List<Image> loadWindowIcons() {
        List<Image> icons = new ArrayList<>();
        for (String resourcePath : ICON_RESOURCE_PATHS) {
            try (InputStream stream = AppIconSupport.class.getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    continue;
                }
                Image image = ImageIO.read(stream);
                if (image != null) {
                    icons.add(image);
                }
            } catch (IOException ignored) {
                // Keep icon loading best-effort so the GUI still launches if a resource is missing.
            }
        }
        return List.copyOf(icons);
    }
}

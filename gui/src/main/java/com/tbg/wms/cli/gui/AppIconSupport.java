package com.tbg.wms.cli.gui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads the packaged application icon resources for Swing runtime windows.
 */
public final class AppIconSupport {
    private static final String WINDOW_ICON_RESOURCE = "/icons/wms-placeholder-icon.png";

    private AppIconSupport() {
        // Utility class.
    }

    public static Image loadWindowIcon() {
        try (InputStream in = AppIconSupport.class.getResourceAsStream(WINDOW_ICON_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Window icon resource not found: " + WINDOW_ICON_RESOURCE);
            }
            return ImageIO.read(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load window icon resource.", ex);
        }
    }
}

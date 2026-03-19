package com.tbg.wms.cli.gui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

final class AppIconSupport {

    private static final String[] ICON_RESOURCE_PATHS = {
            "/icons/orange-slice-256.png",
            "/icons/orange-slice-64.png",
            "/icons/orange-slice-32.png"
    };

    private AppIconSupport() {
    }

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

package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.awt.Image;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppIconSupportTest {

    @Test
    void loadWindowIcons_shouldLoadBundledOrangeSliceIcons() {
        List<Image> icons = AppIconSupport.loadWindowIcons();

        assertFalse(icons.isEmpty());
        assertTrue(icons.stream().allMatch(image -> image.getWidth(null) > 0 && image.getHeight(null) > 0));
    }
}

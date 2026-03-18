package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.awt.Image;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppIconSupportTest {

    @Test
    void loadWindowIcon_shouldReturnPlaceholderIconResource() {
        Image icon = AppIconSupport.loadWindowIcon();

        assertNotNull(icon);
    }
}

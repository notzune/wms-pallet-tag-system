package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZplPreviewRenderServiceTest {

    @Test
    void buildRenderUriProducesExpectedLabelaryPath() {
        ZplPreviewRenderService service = new ZplPreviewRenderService(null, "https://api.labelary.com");

        URI uri = service.buildRenderUri(8, 4.0d, 6.0d, 0);

        assertEquals("https://api.labelary.com/v1/printers/8dpmm/labels/4x6/0/", uri.toString());
    }

    @Test
    void renderRejectsBlankZplAndInvalidDimensions() {
        ZplPreviewRenderService service = new ZplPreviewRenderService(null, "https://api.labelary.com");

        assertThrows(IllegalArgumentException.class, () -> service.render(" ", 8, 4.0d, 6.0d, 0));
        assertThrows(IllegalArgumentException.class, () -> service.buildRenderUri(8, 4.0d, 6.0d, -1));
    }
}

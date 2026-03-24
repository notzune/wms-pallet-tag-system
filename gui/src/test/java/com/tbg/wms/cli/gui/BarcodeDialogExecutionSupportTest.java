package com.tbg.wms.cli.gui;

import com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarcodeDialogExecutionSupportTest {

    private final BarcodeDialogExecutionSupport support = new BarcodeDialogExecutionSupport(
            Path.of("out"),
            DateTimeFormatter.ofPattern("'TS'"),
            40
    );

    @Test
    void requireBarcodeData_shouldRejectBlankInput() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> support.requireBarcodeData(" "));

        assertEquals("Barcode data is required.", ex.getMessage());
        assertEquals("HELLO", support.requireBarcodeData(" HELLO "));
    }

    @Test
    void resolveOutputPath_shouldUsePrintToFileDirAndSluggedName() {
        Path path = support.resolveOutputPath("", "Hello World 123", true);

        assertTrue(path.toString().endsWith("out\\barcode-TS-hello-world-123.zpl")
                || path.toString().endsWith("out/barcode-TS-hello-world-123.zpl"));
    }

    @Test
    void buildMessages_shouldRemainStable() {
        assertEquals("ZPL saved to out\\file.zpl", support.buildGeneratedMessage(Path.of("out", "file.zpl")));
        assertTrue(support.buildPrintedMessage(Path.of("out", "file.zpl")).contains("Printed barcode label."));
        assertEquals("Failed to write ZPL file: root",
                support.buildWriteFailureMessage(new IllegalStateException("top", new IllegalArgumentException("root"))));
        assertEquals("Failed to print barcode: root",
                support.buildPrintFailureMessage(new IllegalStateException("top", new IllegalArgumentException("root"))));
    }

    @Test
    void buildZpl_shouldDelegateToBarcodeBuilder() {
        String zpl = support.buildZpl(new BarcodeRequest(
                "12345",
                Symbology.CODE128,
                Orientation.PORTRAIT,
                812,
                1218,
                60,
                60,
                3,
                3,
                220,
                true,
                1
        ));

        assertTrue(zpl.contains("^XA"));
        assertTrue(zpl.contains("12345"));
    }
}

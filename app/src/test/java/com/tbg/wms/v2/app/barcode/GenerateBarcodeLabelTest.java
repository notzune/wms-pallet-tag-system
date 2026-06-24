package com.tbg.wms.v2.app.barcode;

import com.tbg.wms.v2.domain.barcode.BarcodePreset;
import com.tbg.wms.v2.domain.barcode.BarcodeRequest;
import com.tbg.wms.v2.domain.barcode.BarcodeSymbology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerateBarcodeLabelTest {
    @Test
    void generateBarcodeLabel_rendersRequestAndReturnsArtifactName() {
        GenerateBarcodeLabel generator = new GenerateBarcodeLabel(request -> "ZPL:" + request.data());

        BarcodeLabel label = generator.generate(new BarcodeRequest(
                "ABC123",
                BarcodeSymbology.CODE128,
                BarcodeRequest.Orientation.PORTRAIT,
                812,
                1218,
                40,
                40,
                2,
                3,
                120,
                true,
                1,
                null,
                false
        ));

        assertEquals("barcode-abc123.zpl", label.artifactName());
        assertEquals("ZPL:ABC123", label.zpl());
    }

    @Test
    void generateBarcodeLabel_supportsKnownBreakStartPreset() {
        GenerateBarcodeLabel generator = new GenerateBarcodeLabel(request -> request.data() + ":" + request.captionText());

        BarcodeLabel label = generator.generate(BarcodePreset.BREAK_START);

        assertEquals("barcode-break-start.zpl", label.artifactName());
        assertEquals("BRKSTART:BREAK START", label.zpl());
    }
}

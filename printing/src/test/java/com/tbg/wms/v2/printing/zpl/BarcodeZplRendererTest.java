package com.tbg.wms.v2.printing.zpl;

import com.tbg.wms.v2.domain.barcode.BarcodeRequest;
import com.tbg.wms.v2.domain.barcode.BarcodeSymbology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarcodeZplRendererTest {
    @Test
    void render_matchesLegacyCode128PortraitOutput() {
        BarcodeRequest request = new BarcodeRequest(
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
        );

        String zpl = new BarcodeZplRenderer().render(request);

        assertEquals("""
                ^XA
                ^PON
                ^PW812
                ^LL1218
                ^FWN
                ^BY2,3,120
                ^FO285,489
                ^BCN,120,Y,N,N
                ^FDABC123^FS
                ^XZ
                """, zpl);
    }
}

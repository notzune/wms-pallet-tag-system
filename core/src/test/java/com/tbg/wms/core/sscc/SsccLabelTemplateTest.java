package com.tbg.wms.core.sscc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SsccLabelTemplateTest {
    @Test
    void render_matchesWorkingZplStructure() {
        SsccLabelGroup group = new SsccLabelGroup(
                "8000614777",
                "5PNKXYCY",
                "258916",
                "BBIL",
                "2450",
                "AMAZON.COM NON-SORT FORT WAYNE FWA4",
                "9800 SMITH RD, 000000  FORT WAYNE,IN 468099771",
                "Tropicana Manufacturing Company Inc.",
                "Brockport",
                "00008402279494097449",
                12.0d,
                List.of("206864000"),
                List.of("102126W")
        );

        String zpl = new SsccLabelTemplate().render(group, 1, 2);

        assertTrue(zpl.contains("Tropicana Manufacturing Company Inc."));
        assertTrue(zpl.contains("4 Owens Rd."));
        assertTrue(zpl.contains("CARRIER: BBIL"));
        assertTrue(zpl.contains("Items: 206864000"));
        assertTrue(zpl.contains("^FO95,735^BCN,190,N,N,N^FD>;>800008402279494097449^FS"));
    }
}

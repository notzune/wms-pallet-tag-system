package com.tbg.wms.core.sscc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SsccCsvSupportTest {
    @Test
    void parse_readsRequiredHeadersAndRows() {
        List<String> csv = List.of(
                "Sales Order #,Purchase Order #,Shipment #,Carrier Code,Trailer ID,Destination,Destination Address,Customer Name,Facility,Item #,Level 2 Reference #,Originally Shipped LPN,Sum of Ship Cases,New Received LPN",
                "8000614777,5PNKXYCY,258916,BBIL,2450,AMAZON.COM NON-SORT FORT WAYNE FWA4,\"9800 SMITH RD, 000000 FORT WAYNE,IN 468099771\",Tropicana Manufacturing Company Inc.,Brockport,206864000,102126W,00008402279494097449,12,00008402279494097449"
        );

        List<SsccLabelRow> rows = SsccCsvSupport.parse(csv);

        assertEquals(1, rows.size());
        assertEquals("8000614777", rows.get(0).salesOrder());
        assertEquals("00008402279494097449", rows.get(0).newReceivedLpn());
    }
}

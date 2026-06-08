package com.tbg.wms.core.sscc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsccLabelPlannerTest {
    @Test
    void groupRows_combinesRowsBySalesOrderAndLpn_andFlagsMixedSku() {
        List<SsccLabelRow> rows = List.of(
                new SsccLabelRow("8000614777", "PO1", "258916", "BBIL", "2450", "DEST", "ADDR", "CUSTOMER", "FAC", "206864000", "102126W", "LPN1", 7.0d, "LPN2"),
                new SsccLabelRow("8000614777", "PO1", "258916", "BBIL", "2450", "DEST", "ADDR", "CUSTOMER", "FAC", "999999999", "102127W", "LPN1", 5.0d, "LPN2"),
                new SsccLabelRow("8000614777", "PO1", "258916", "BBIL", "2450", "DEST", "ADDR", "CUSTOMER", "FAC", "123456789", "102128W", "LPN3", 4.0d, "LPN4")
        );

        List<SsccLabelGroup> groups = new SsccLabelPlanner().groupRows(rows);

        assertEquals(2, groups.size());
        assertTrue(groups.get(0).mixedSku());
        assertEquals("MIXED SKU", groups.get(0).bannerText());
        assertEquals("12", groups.get(0).cartonText());
        assertEquals("SKU 123456789", groups.get(1).bannerText());
    }
}

package com.tbg.wms.cli.gui.sscc;

import com.tbg.wms.core.sscc.SsccLabelGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SsccGroupTableModelTest {
    @Test
    void setGroups_exposesGroupedLabelSummaryColumns() {
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

        SsccGroupTableModel model = new SsccGroupTableModel();
        model.setGroups(List.of(group));

        assertEquals(1, model.getRowCount());
        assertEquals("8000614777", model.getValueAt(0, 0));
        assertEquals("00008402279494097449", model.getValueAt(0, 1));
        assertEquals("Single SKU", model.getValueAt(0, 2));
        assertEquals("206864000", model.getValueAt(0, 3));
        assertEquals("102126W", model.getValueAt(0, 4));
        assertEquals("12", model.getValueAt(0, 5));
    }
}

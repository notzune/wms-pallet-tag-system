package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewStopDetailsSupportTest {

    private final LabelPreviewFormatter formatter = new LabelPreviewFormatter();
    private final PreviewStopDetailsSupport support = new PreviewStopDetailsSupport(formatter);

    @Test
    void buildStopDetailsText_shouldIncludeTotalsAndPreviewNoticeWhenTruncated() {
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipmentOne = PreviewSelectionTestData.shipmentJob("S1", List.of(lpn));
        LabelWorkflowService.PreparedJob shipmentTwo = PreviewSelectionTestData.shipmentJob("S2", List.of(lpn));
        AdvancedPrintWorkflowService.PreparedStopGroup stop =
                PreviewSelectionTestData.stopGroup(1, 4, List.of(shipmentOne, shipmentTwo));

        String details = support.buildStopDetailsText(stop, 1, 10);

        assertTrue(details.contains("Shipment Summary: S1"));
        assertTrue(details.contains("Preview Notice: Showing first 1 shipments for this stop."));
        assertTrue(details.contains("Stop Totals -> Full:"));
    }

    @Test
    void stopPreviewLabel_shouldIncludeSequenceWhenAvailable() {
        AdvancedPrintWorkflowService.PreparedStopGroup stop =
                PreviewSelectionTestData.stopGroup(3, 7, List.of());

        String label = support.stopPreviewLabel(stop);

        assertTrue(label.contains("Stop 7"));
        assertTrue(label.contains("Seq 3"));
    }
}

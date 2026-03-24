package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiPreviewDisplaySupportTest {

    private final GuiPreviewDisplaySupport support =
            new GuiPreviewDisplaySupport(new LabelPreviewFormatter(), 1, 10, 10);

    @Test
    void buildShipmentPreview_shouldRenderSummaryAndMath() {
        Lpn first = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob preparedJob = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(first));
        PreviewSelectionSupport.SelectionSnapshot selection = new PreviewSelectionSupport.SelectionSnapshot(
                List.of(new PreviewSelectionSupport.LabelOption("01", first, null)),
                List.of(first),
                List.of(),
                1
        );

        GuiPreviewDisplaySupport.PreviewText text = support.buildShipmentPreview(preparedJob, selection);

        assertTrue(text.summaryText().contains("SHIP1"));
        assertTrue(text.mathText().contains("Selected Labels"));
    }

    @Test
    void buildCarrierMovePreview_shouldAppendPreviewNoticeWhenStopsTruncated() {
        Lpn first = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn second = new Lpn("LPN-2", "S2", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipmentOne = PreviewSelectionTestData.shipmentJob("S1", List.of(first));
        LabelWorkflowService.PreparedJob shipmentTwo = PreviewSelectionTestData.shipmentJob("S2", List.of(second));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentOne)),
                        PreviewSelectionTestData.stopGroup(2, 2, List.of(shipmentTwo))
                ));

        GuiPreviewDisplaySupport.PreviewText text = support.buildCarrierMovePreview(
                carrierJob,
                List.of(
                        LabelSelectionRef.forCarrierMove(1, "S1", "LPN-1", 1),
                        LabelSelectionRef.forCarrierMove(2, "S2", "LPN-2", 2)
                ),
                2
        );

        assertTrue(text.summaryText().contains("Preview Notice: Showing first 1 stops of 2."));
        assertTrue(text.mathText().contains("Selected Labels"));
    }
}

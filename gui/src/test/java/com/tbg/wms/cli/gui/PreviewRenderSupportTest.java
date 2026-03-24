package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewRenderSupportTest {

    private final PreviewRenderSupport support =
            new PreviewRenderSupport(new LabelPreviewFormatter(), 1, 10, 10);

    @Test
    void addStopPreviewSections_shouldRespectPreviewLimit() {
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipmentOne = PreviewSelectionTestData.shipmentJob("S1", List.of(lpn));
        LabelWorkflowService.PreparedJob shipmentTwo = PreviewSelectionTestData.shipmentJob("S2", List.of(lpn));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentOne)),
                        PreviewSelectionTestData.stopGroup(2, 2, List.of(shipmentTwo))
                ));
        JPanel panel = new JPanel();

        int shown = support.addStopPreviewSections(panel, carrierJob);

        assertEquals(1, shown);
        assertEquals(1, panel.getComponentCount());
    }

    @Test
    void buildStopPreviewSection_shouldToggleExpandedLabel() {
        Lpn lpn = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipment = PreviewSelectionTestData.shipmentJob("S1", List.of(lpn));
        AdvancedPrintWorkflowService.PreparedStopGroup stop =
                PreviewSelectionTestData.stopGroup(1, 1, List.of(shipment));

        JComponent section = support.buildStopPreviewSection(stop);
        JToggleButton toggle = null;
        for (Component component : section.getComponents()) {
            if (component instanceof JToggleButton) {
                toggle = (JToggleButton) component;
                break;
            }
        }

        assertTrue(toggle != null && toggle.getText().contains("[expanded]"));
        toggle.doClick();
        assertTrue(toggle.getText().contains("[collapsed]"));
    }

    @Test
    void renderShipmentPreview_shouldReplacePanelContents() {
        JPanel previewPanel = new JPanel();
        JTextArea shipmentArea = new JTextArea("shipment");
        JPanel selectionPanel = new JPanel();

        support.renderShipmentPreview(previewPanel, shipmentArea, selectionPanel);

        assertEquals(2, previewPanel.getComponentCount());
        assertEquals(shipmentArea, previewPanel.getComponent(0));
        assertEquals(selectionPanel, previewPanel.getComponent(1));
    }
}

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreviewSelectionSupportTest {

    private final PreviewSelectionSupport support = new PreviewSelectionSupport();

    @Test
    void snapshotSelection_shouldCollectShipmentSelectionsAndInfoTags() {
        Lpn first = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn second = new Lpn("LPN-2", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        List<PreviewSelectionSupport.LabelOption> options = List.of(
                new PreviewSelectionSupport.LabelOption("01. LPN-1", first, null),
                new PreviewSelectionSupport.LabelOption("02. LPN-2", second, null)
        );
        JCheckBox firstBox = new JCheckBox("01. LPN-1", true);
        JCheckBox secondBox = new JCheckBox("02. LPN-2", false);

        PreviewSelectionSupport.SelectionSnapshot snapshot = support.snapshotSelection(
                List.of(firstBox, secondBox),
                options,
                true,
                false,
                true
        );

        assertEquals(1, snapshot.selectedLabelCount());
        assertEquals(List.of(first), snapshot.selectedShipmentLpns());
        assertEquals(1, snapshot.infoTagCount());
        assertEquals(2, snapshot.totalDocuments());
    }

    @Test
    void snapshotSelection_shouldCollectCarrierMoveSelectionsAndDistinctStopInfoTags() {
        List<PreviewSelectionSupport.LabelOption> options = List.of(
                new PreviewSelectionSupport.LabelOption("01", null, LabelSelectionRef.forCarrierMove(1, "S1", "L1", 1)),
                new PreviewSelectionSupport.LabelOption("02", null, LabelSelectionRef.forCarrierMove(2, "S1", "L2", 1)),
                new PreviewSelectionSupport.LabelOption("03", null, LabelSelectionRef.forCarrierMove(3, "S2", "L3", 3))
        );
        JCheckBox firstBox = new JCheckBox("01", true);
        JCheckBox secondBox = new JCheckBox("02", true);
        JCheckBox thirdBox = new JCheckBox("03", true);

        PreviewSelectionSupport.SelectionSnapshot snapshot = support.snapshotSelection(
                List.of(firstBox, secondBox, thirdBox),
                options,
                true,
                true,
                false
        );

        assertEquals(3, snapshot.selectedLabelCount());
        assertEquals(3, snapshot.selectedCarrierLabels().size());
        assertEquals(3, snapshot.infoTagCount());
        assertEquals(2, support.countSelectedCarrierMoveStops(snapshot.selectedCarrierLabels()));
    }

    @Test
    void buildCarrierMoveLabelOptions_shouldProduceStableLabels() {
        Lpn first = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn second = new Lpn("LPN-2", "S2", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipmentOne = PreviewSelectionTestData.shipmentJob("S1", List.of(first));
        LabelWorkflowService.PreparedJob shipmentTwo = PreviewSelectionTestData.shipmentJob("S2", List.of(second));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentOne)),
                        PreviewSelectionTestData.stopGroup(2, 2, List.of(shipmentTwo))
                ));

        List<PreviewSelectionSupport.LabelOption> options = support.buildCarrierMoveLabelOptions(carrierJob);

        assertEquals(2, options.size());
        assertEquals("01. Stop 01 | Shipment S1 | LPN-1", options.get(0).labelText());
        assertEquals("02. Stop 02 | Shipment S2 | LPN-2", options.get(1).labelText());
    }
}

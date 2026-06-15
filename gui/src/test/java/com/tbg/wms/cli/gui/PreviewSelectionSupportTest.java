package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PreviewSelectionSupportTest {

    private final PreviewSelectionSupport support = new PreviewSelectionSupport();

    @Test
    void buildShipmentLabelOptions_shouldIncludeValidLpnAndItemDetails() {
        Lpn first = lpn("901427186", "30081705", "Vanilla Yogurt");
        Lpn second = lpn("64362111_OOL", "30081706", "Blueberry Yogurt");
        List<PreviewSelectionSupport.LabelOption> options = support.buildShipmentLabelOptions(
                PreviewSelectionTestData.shipmentJob("S1", List.of(first, second))
        );

        assertEquals("01. LPN 901427186 | ITEM#: 30081705 | ORD#: S1", options.get(0).labelText());
        assertEquals("02. LPN 64362111_OOL | ITEM#: 30081706 | ORD#: S1", options.get(1).labelText());
    }

    @Test
    void buildShipmentLabelOptions_shouldDropInvalidLpnValuesFromCardText() {
        Lpn invalid = lpn("PERM-CRE-LOD-3002", "30081707", "Strawberry Yogurt");
        Lpn synthetic = lpn("NO_LPN_1", "30081708", "Plain Yogurt");
        List<PreviewSelectionSupport.LabelOption> options = support.buildShipmentLabelOptions(
                PreviewSelectionTestData.shipmentJob("S1", List.of(invalid, synthetic))
        );

        assertEquals("01. ITEM#: 30081707 | ORD#: S1", options.get(0).labelText());
        assertEquals("02. ITEM#: 30081708 | ORD#: S1", options.get(1).labelText());
        assertFalse(options.stream()
                .map(PreviewSelectionSupport.LabelOption::labelText)
                .anyMatch(text -> text.contains("NO_LPN")));
    }

    @Test
    void snapshotSelection_shouldCollectShipmentSelectionsAndInfoTags() {
        Lpn first = lpn("901427186", "30081705", "Vanilla Yogurt");
        Lpn second = lpn("901427191", "30081706", "Blueberry Yogurt");
        List<PreviewSelectionSupport.LabelOption> options = List.of(
                new PreviewSelectionSupport.LabelOption("01. LPN 901427186 | ITEM#: 30081705 | ORD#: S1", first, null),
                new PreviewSelectionSupport.LabelOption("02. LPN 901427191 | ITEM#: 30081706 | ORD#: S1", second, null)
        );
        JCheckBox firstBox = new JCheckBox("01. LPN 901427186 | ITEM#: 30081705 | ORD#: S1", true);
        JCheckBox secondBox = new JCheckBox("02. LPN 901427191 | ITEM#: 30081706 | ORD#: S1", false);

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
        Lpn first = lpn("901427186", "30081705", "Vanilla Yogurt");
        Lpn second = lpn("901427191", "30081706", "Blueberry Yogurt");
        LabelWorkflowService.PreparedJob shipmentOne = PreviewSelectionTestData.shipmentJob("S1", List.of(first));
        LabelWorkflowService.PreparedJob shipmentTwo = PreviewSelectionTestData.shipmentJob("S2", List.of(second));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipmentOne)),
                        PreviewSelectionTestData.stopGroup(2, 2, List.of(shipmentTwo))
                ));

        List<PreviewSelectionSupport.LabelOption> options = support.buildCarrierMoveLabelOptions(carrierJob);

        assertEquals(2, options.size());
        assertEquals("01. LPN 901427186 | ITEM#: 30081705 | ORD#: S1", options.get(0).labelText());
        assertEquals("02. LPN 901427191 | ITEM#: 30081706 | ORD#: S2", options.get(1).labelText());
    }

    @Test
    void buildCarrierMoveLabelOptions_shouldHideInvalidLpnValues() {
        Lpn invalid = lpn("NO_LPN_1", "30081708", "Plain Yogurt");
        LabelWorkflowService.PreparedJob shipment = PreviewSelectionTestData.shipmentJob("S1", List.of(invalid));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob =
                PreviewSelectionTestData.carrierMoveJob("CM1", List.of(
                        PreviewSelectionTestData.stopGroup(1, 1, List.of(shipment))
                ));

        List<PreviewSelectionSupport.LabelOption> options = support.buildCarrierMoveLabelOptions(carrierJob);

        assertEquals("01. ITEM#: 30081708 | ORD#: S1", options.get(0).labelText());
    }

    private static Lpn lpn(String lpnId, String itemNumber, String description) {
        return new Lpn(
                lpnId,
                "S1",
                null,
                0,
                0,
                0.0,
                null,
                null,
                null,
                null,
                null,
                List.of(new LineItem(
                        "1",
                        "0",
                        itemNumber,
                        description,
                        null,
                        "ORDER-1",
                        null,
                        null,
                        1,
                        1,
                        "EA",
                        0.0,
                        null,
                        null,
                        null
                ))
        );
    }
}

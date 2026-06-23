package com.tbg.wms.v2.app.labels;

import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import com.tbg.wms.v2.domain.print.PrintTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipmentLabelCharacterizationTest {
    @Test
    void shipmentPrintPlan_matchesCurrentLabelAndInfoTagCounts() {
        PreparedShipmentLabels shipment = PreparedShipmentLabels.of(
                "800123456",
                List.of(
                        LabelSelectionRef.palletLabel("LPN-001", 1),
                        LabelSelectionRef.palletLabel("LPN-002", 2)
                ),
                true
        );

        BuildShipmentPrintPlan.PrintPlan plan = new BuildShipmentPrintPlan()
                .build(shipment, List.of("LPN-001", "LPN-002"));

        assertEquals(3, plan.tasks().size());
        assertEquals(PrintTask.Kind.PALLET_LABEL, plan.tasks().get(0).kind());
        assertEquals(PrintTask.Kind.PALLET_LABEL, plan.tasks().get(1).kind());
        assertEquals(PrintTask.Kind.SHIPMENT_INFO_TAG, plan.tasks().get(2).kind());
        assertEquals("800123456_LPN-001_1_of_2.zpl", plan.tasks().get(0).artifactName());
        assertEquals("800123456_LPN-002_2_of_2.zpl", plan.tasks().get(1).artifactName());
        assertEquals("info-shipment-800123456.zpl", plan.tasks().get(2).artifactName());
    }

    @Test
    void shipmentPrintPlan_printsOnlySelectedPalletLabelsBeforeInfoTag() {
        PreparedShipmentLabels shipment = PreparedShipmentLabels.of(
                "800123456",
                List.of(
                        LabelSelectionRef.palletLabel("LPN-001", 1),
                        LabelSelectionRef.palletLabel("LPN-002", 2),
                        LabelSelectionRef.palletLabel("LPN-003", 3)
                ),
                true
        );

        BuildShipmentPrintPlan.PrintPlan plan = new BuildShipmentPrintPlan()
                .build(shipment, List.of("LPN-002"));

        assertEquals(2, plan.tasks().size());
        assertEquals("800123456_LPN-002_1_of_1.zpl", plan.tasks().get(0).artifactName());
        assertEquals("info-shipment-800123456.zpl", plan.tasks().get(1).artifactName());
    }
}

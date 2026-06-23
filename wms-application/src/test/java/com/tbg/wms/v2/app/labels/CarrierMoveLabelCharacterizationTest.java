package com.tbg.wms.v2.app.labels;

import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.carriermove.PreparedStopGroup;
import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import com.tbg.wms.v2.domain.print.PrintTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarrierMoveLabelCharacterizationTest {
    @Test
    void carrierMovePrintPlan_ordersShipmentLabelsStopTagsAndFinalTag() {
        CarrierMoveLabels carrierMove = CarrierMoveLabels.of(
                "CM-100",
                List.of(
                        PreparedStopGroup.of(1, 10, List.of(shipment("800111111", "LPN-001"))),
                        PreparedStopGroup.of(2, 20, List.of(shipment("800222222", "LPN-002")))
                ),
                true
        );

        BuildCarrierMovePrintPlan.PrintPlan plan = new BuildCarrierMovePrintPlan().build(carrierMove);

        assertEquals(5, plan.tasks().size());
        assertTask(plan.tasks().get(0), PrintTask.Kind.PALLET_LABEL, "800111111_LPN-001_1_of_1.zpl");
        assertTask(plan.tasks().get(1), PrintTask.Kind.STOP_INFO_TAG, "info-stop-01-of-02.zpl");
        assertTask(plan.tasks().get(2), PrintTask.Kind.PALLET_LABEL, "800222222_LPN-002_1_of_1.zpl");
        assertTask(plan.tasks().get(3), PrintTask.Kind.STOP_INFO_TAG, "info-stop-02-of-02.zpl");
        assertTask(plan.tasks().get(4), PrintTask.Kind.FINAL_INFO_TAG, "info-final-cmid-CM-100.zpl");
    }

    @Test
    void carrierMovePrintPlan_omitsUnselectedStopsAndKeepsOriginalTotalStopCount() {
        CarrierMoveLabels carrierMove = CarrierMoveLabels.of(
                "CM-100",
                List.of(
                        PreparedStopGroup.of(1, 10, List.of(shipment("800111111", "LPN-001"))),
                        PreparedStopGroup.of(2, 20, List.of(shipment("800222222", "LPN-002")))
                ),
                true
        );

        BuildCarrierMovePrintPlan.PrintPlan plan = new BuildCarrierMovePrintPlan()
                .build(carrierMove, List.of("800222222:LPN-002"));

        assertEquals(3, plan.tasks().size());
        assertTask(plan.tasks().get(0), PrintTask.Kind.PALLET_LABEL, "800222222_LPN-002_1_of_1.zpl");
        assertTask(plan.tasks().get(1), PrintTask.Kind.STOP_INFO_TAG, "info-stop-02-of-02.zpl");
        assertTask(plan.tasks().get(2), PrintTask.Kind.FINAL_INFO_TAG, "info-final-cmid-CM-100.zpl");
    }

    private static PreparedShipmentLabels shipment(String shipmentId, String labelId) {
        return PreparedShipmentLabels.of(
                shipmentId,
                List.of(LabelSelectionRef.palletLabel(labelId, 1)),
                false
        );
    }

    private static void assertTask(PrintTask task, PrintTask.Kind kind, String artifactName) {
        assertEquals(kind, task.kind());
        assertEquals(artifactName, task.artifactName());
    }
}

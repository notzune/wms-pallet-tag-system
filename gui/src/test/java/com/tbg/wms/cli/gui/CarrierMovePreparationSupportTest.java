/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.CarrierMoveStopRef;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarrierMovePreparationSupportTest {
    private final CarrierMovePreparationSupport support = new CarrierMovePreparationSupport();

    @Test
    void buildStopShipmentPlans_shouldGroupByStopSortNullStopsLastAndDeduplicateShipments() {
        List<CarrierMovePreparationSupport.StopShipmentPlan> plans = support.buildStopShipmentPlans(List.of(
                ref(2, "S200"),
                ref(1, "S101"),
                ref(1, "S100"),
                ref(1, "S100"),
                ref(null, "S999")
        ));

        assertEquals(3, plans.size());
        assertEquals(Integer.valueOf(1), plans.get(0).stopSequence());
        assertEquals(List.of("S100", "S101"), plans.get(0).shipmentIds());
        assertEquals(Integer.valueOf(2), plans.get(1).stopSequence());
        assertEquals(List.of("S200"), plans.get(1).shipmentIds());
        assertEquals(null, plans.get(2).stopSequence());
        assertEquals(List.of("S999"), plans.get(2).shipmentIds());
    }

    @Test
    void buildStopShipmentPlans_shouldSkipNullRefsAndBlankShipments() {
        List<CarrierMovePreparationSupport.StopShipmentPlan> plans = support.buildStopShipmentPlans(Arrays.asList(
                null,
                ref(1, " "),
                ref(2, null),
                ref(3, "S300")
        ));

        assertEquals(1, plans.size());
        assertEquals(Integer.valueOf(3), plans.get(0).stopSequence());
        assertEquals(List.of("S300"), plans.get(0).shipmentIds());
    }

    private CarrierMoveStopRef ref(Integer stopSequence, String shipmentId) {
        return new CarrierMoveStopRef("CM1", "STOP-" + stopSequence, stopSequence, stopSequence, shipmentId, "READY", null);
    }
}

/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.CarrierMoveStopRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure carrier-move stop planning helpers shared by GUI workflow preparation.
 */
final class CarrierMovePreparationSupport {
    List<StopShipmentPlan> buildStopShipmentPlans(List<CarrierMoveStopRef> refs) {
        Map<Integer, List<CarrierMoveStopRef>> byStop = groupRefsByStop(refs);
        List<StopShipmentPlan> plans = new ArrayList<>(byStop.size());
        for (List<CarrierMoveStopRef> stopRefs : byStop.values()) {
            if (stopRefs == null || stopRefs.isEmpty()) {
                continue;
            }
            List<String> shipmentIds = resolveShipmentIds(stopRefs);
            if (!shipmentIds.isEmpty()) {
                plans.add(new StopShipmentPlan(firstStopSequence(stopRefs), shipmentIds));
            }
        }
        return plans;
    }

    private Map<Integer, List<CarrierMoveStopRef>> groupRefsByStop(List<CarrierMoveStopRef> refs) {
        Map<Integer, List<CarrierMoveStopRef>> byStop = new TreeMap<>();
        for (CarrierMoveStopRef ref : refs) {
            if (ref == null) {
                continue;
            }
            int key = ref.getStopSequence() == null ? Integer.MAX_VALUE : ref.getStopSequence();
            byStop.computeIfAbsent(key, ignored -> new ArrayList<>()).add(ref);
        }
        return byStop;
    }

    private List<String> resolveShipmentIds(List<CarrierMoveStopRef> stopRefs) {
        List<CarrierMoveStopRef> orderedRefs = new ArrayList<>(stopRefs);
        orderedRefs.sort(Comparator.comparing(
                CarrierMoveStopRef::getShipmentId,
                Comparator.nullsLast(String::compareTo)
        ));

        LinkedHashSet<String> uniqueShipments = new LinkedHashSet<>(orderedRefs.size());
        for (CarrierMoveStopRef ref : orderedRefs) {
            if (ref.getShipmentId() != null && !ref.getShipmentId().isBlank()) {
                uniqueShipments.add(ref.getShipmentId());
            }
        }
        return List.copyOf(uniqueShipments);
    }

    private Integer firstStopSequence(List<CarrierMoveStopRef> stopRefs) {
        for (CarrierMoveStopRef stopRef : stopRefs) {
            if (stopRef != null) {
                return stopRef.getStopSequence();
            }
        }
        return null;
    }

    record StopShipmentPlan(Integer stopSequence, List<String> shipmentIds) {
    }
}

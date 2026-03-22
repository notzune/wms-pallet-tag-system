package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalizes and validates shipment/carrier-move label selections for print planning.
 */
final class PrintTaskSelectionSupport {

    private PrintTaskSelectionSupport() {
    }

    static List<Lpn> filterLpnsForPrint(List<Lpn> availableLpns, List<Lpn> selectedLpns) {
        Objects.requireNonNull(availableLpns, "availableLpns cannot be null");
        Objects.requireNonNull(selectedLpns, "selectedLpns cannot be null");

        LinkedHashSet<String> selectedIds = collectSelectedLpnIds(selectedLpns);
        List<Lpn> filtered = new ArrayList<>(selectedIds.size());
        for (Lpn lpn : availableLpns) {
            if (lpn != null && selectedIds.contains(lpn.getLpnId())) {
                filtered.add(lpn);
            }
        }
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("Selected labels are no longer available for printing.");
        }
        return filtered;
    }

    static List<LabelSelectionRef> collectAllCarrierMoveLabelSelections(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job
    ) {
        Objects.requireNonNull(job, "job cannot be null");
        List<LabelSelectionRef> selected = new ArrayList<>();
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                for (Lpn lpn : shipmentJob.getLpnsForLabels()) {
                    if (lpn != null && lpn.getLpnId() != null) {
                        selected.add(LabelSelectionRef.forCarrierMove(
                                selected.size() + 1,
                                shipmentJob.getShipmentId(),
                                lpn.getLpnId(),
                                stop.getStopPosition()
                        ));
                    }
                }
            }
        }
        return selected;
    }

    static int countShipmentInfoTags(int selectedLabels, boolean includeInfoTags) {
        return includeInfoTags && selectedLabels > 0 ? 1 : 0;
    }

    static int countCarrierMoveInfoTags(List<LabelSelectionRef> selectedLabels, boolean includeInfoTags) {
        Objects.requireNonNull(selectedLabels, "selectedLabels cannot be null");
        if (!includeInfoTags || selectedLabels.isEmpty()) {
            return 0;
        }
        LinkedHashSet<Integer> distinctStops = new LinkedHashSet<>();
        for (LabelSelectionRef selectedLabel : selectedLabels) {
            if (selectedLabel != null && selectedLabel.getStopPosition() != null) {
                distinctStops.add(selectedLabel.getStopPosition());
            }
        }
        return distinctStops.isEmpty() ? 0 : distinctStops.size() + 1;
    }

    static Map<String, LinkedHashSet<String>> indexCarrierMoveSelections(List<LabelSelectionRef> selectedLabels) {
        Objects.requireNonNull(selectedLabels, "selectedLabels cannot be null");
        Map<String, LinkedHashSet<String>> selectedLpnsByShipment = new LinkedHashMap<>();
        for (LabelSelectionRef selection : selectedLabels) {
            if (selection == null || selection.getShipmentId() == null || selection.getShipmentId().isBlank()
                    || selection.getLpnId() == null || selection.getLpnId().isBlank()) {
                continue;
            }
            selectedLpnsByShipment
                    .computeIfAbsent(selection.getShipmentId(), ignored -> new LinkedHashSet<>())
                    .add(selection.getLpnId());
        }
        return selectedLpnsByShipment;
    }

    static List<Lpn> filterCarrierMoveShipmentLpns(
            LabelWorkflowService.PreparedJob shipmentJob,
            Map<String, LinkedHashSet<String>> selectedLpnsByShipment
    ) {
        Objects.requireNonNull(shipmentJob, "shipmentJob cannot be null");
        Objects.requireNonNull(selectedLpnsByShipment, "selectedLpnsByShipment cannot be null");
        LinkedHashSet<String> selectedIds = selectedLpnsByShipment.get(shipmentJob.getShipmentId());
        if (selectedIds == null || selectedIds.isEmpty()) {
            return List.of();
        }
        List<Lpn> selectedLpns = new ArrayList<>(selectedIds.size());
        for (Lpn lpn : shipmentJob.getLpnsForLabels()) {
            if (lpn != null && selectedIds.contains(lpn.getLpnId())) {
                selectedLpns.add(lpn);
            }
        }
        return selectedLpns;
    }

    private static LinkedHashSet<String> collectSelectedLpnIds(List<Lpn> selectedLpns) {
        if (selectedLpns.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }
        LinkedHashSet<String> selectedIds = new LinkedHashSet<>();
        for (Lpn lpn : selectedLpns) {
            if (lpn != null && lpn.getLpnId() != null) {
                selectedIds.add(lpn.getLpnId());
            }
        }
        if (selectedIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }
        return selectedIds;
    }
}

/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;

import javax.swing.JCheckBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure preview-selection helpers shared by GUI selection/rendering flows.
 */
final class PreviewSelectionSupport {

    List<LabelOption> buildShipmentLabelOptions(LabelWorkflowService.PreparedJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        List<LabelOption> options = new ArrayList<>(job.getLpnsForLabels().size());
        int index = 1;
        for (Lpn lpn : job.getLpnsForLabels()) {
            options.add(new LabelOption(String.format("%02d. %s", index, resolveLpnId(lpn)), lpn, null));
            index++;
        }
        return options;
    }

    List<LabelOption> buildCarrierMoveLabelOptions(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        List<LabelOption> options = new ArrayList<>();
        int index = 1;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                for (Lpn lpn : shipmentJob.getLpnsForLabels()) {
                    String labelText = String.format(
                            "%02d. Stop %02d | Shipment %s | %s",
                            index,
                            stop.getStopPosition(),
                            shipmentJob.getShipmentId(),
                            resolveLpnId(lpn)
                    );
                    LabelSelectionRef selection = LabelSelectionRef.forCarrierMove(
                            index,
                            shipmentJob.getShipmentId(),
                            resolveLpnId(lpn),
                            stop.getStopPosition()
                    );
                    options.add(new LabelOption(labelText, lpn, selection));
                    index++;
                }
            }
        }
        return options;
    }

    SelectionSnapshot snapshotSelection(
            List<JCheckBox> checkboxes,
            List<LabelOption> options,
            boolean includeInfoTags,
            boolean carrierMoveMode,
            boolean shipmentModeActive
    ) {
        Objects.requireNonNull(checkboxes, "checkboxes cannot be null");
        Objects.requireNonNull(options, "options cannot be null");
        if (checkboxes.isEmpty() || options.isEmpty()) {
            return new SelectionSnapshot(List.of(), List.of(), List.of(), 0);
        }

        List<LabelOption> selectedOptions = new ArrayList<>();
        List<Lpn> selectedShipmentLpns = new ArrayList<>();
        List<LabelSelectionRef> selectedCarrierLabels = new ArrayList<>();
        for (int i = 0; i < checkboxes.size() && i < options.size(); i++) {
            if (!checkboxes.get(i).isSelected()) {
                continue;
            }
            LabelOption option = options.get(i);
            selectedOptions.add(option);
            if (option.lpn() != null) {
                selectedShipmentLpns.add(option.lpn());
            }
            if (option.carrierMoveSelection() != null) {
                selectedCarrierLabels.add(option.carrierMoveSelection());
            }
        }

        int infoTagCount = computeInfoTagCount(
                includeInfoTags,
                carrierMoveMode,
                shipmentModeActive,
                selectedShipmentLpns.size(),
                selectedCarrierLabels
        );
        return new SelectionSnapshot(selectedOptions, selectedShipmentLpns, selectedCarrierLabels, infoTagCount);
    }

    int countSelectedCarrierMoveStops(List<LabelSelectionRef> selectedLabels) {
        Objects.requireNonNull(selectedLabels, "selectedLabels cannot be null");
        return (int) selectedLabels.stream()
                .map(LabelSelectionRef::getStopPosition)
                .distinct()
                .count();
    }

    private int computeInfoTagCount(
            boolean includeInfoTags,
            boolean carrierMoveMode,
            boolean shipmentModeActive,
            int selectedShipmentLabels,
            List<LabelSelectionRef> selectedCarrierLabels
    ) {
        if (!includeInfoTags) {
            return 0;
        }
        if (carrierMoveMode) {
            return PrintTaskPlanner.countCarrierMoveInfoTags(selectedCarrierLabels, true);
        }
        if (shipmentModeActive) {
            return PrintTaskPlanner.countShipmentInfoTags(selectedShipmentLabels, true);
        }
        return 0;
    }

    private String resolveLpnId(Lpn lpn) {
        return lpn == null || lpn.getLpnId() == null || lpn.getLpnId().isBlank() ? "UNKNOWN" : lpn.getLpnId();
    }

    record LabelOption(
            String labelText,
            Lpn lpn,
            LabelSelectionRef carrierMoveSelection
    ) {
        LabelOption {
            labelText = Objects.requireNonNull(labelText, "labelText");
        }
    }

    record SelectionSnapshot(
            List<LabelOption> selectedOptions,
            List<Lpn> selectedShipmentLpns,
            List<LabelSelectionRef> selectedCarrierLabels,
            int infoTagCount
    ) {
        SelectionSnapshot {
            selectedOptions = List.copyOf(selectedOptions);
            selectedShipmentLpns = List.copyOf(selectedShipmentLpns);
            selectedCarrierLabels = List.copyOf(selectedCarrierLabels);
        }

        int selectedLabelCount() {
            return selectedOptions.size();
        }

        int totalDocuments() {
            return selectedLabelCount() + infoTagCount;
        }
    }
}

package com.tbg.wms.v2.app.labels;

import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.carriermove.PreparedStopGroup;
import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import com.tbg.wms.v2.domain.print.PrintTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds the deterministic print-task order for one prepared carrier move.
 */
public final class BuildCarrierMovePrintPlan {
    private final BuildShipmentPrintPlan shipmentPlanner;

    public BuildCarrierMovePrintPlan() {
        this(new BuildShipmentPrintPlan());
    }

    BuildCarrierMovePrintPlan(BuildShipmentPrintPlan shipmentPlanner) {
        this.shipmentPlanner = Objects.requireNonNull(shipmentPlanner, "shipmentPlanner cannot be null");
    }

    public PrintPlan build(CarrierMoveLabels carrierMove) {
        return build(carrierMove, List.of());
    }

    public PrintPlan build(CarrierMoveLabels carrierMove, List<String> selectedShipmentLabelIds) {
        Objects.requireNonNull(carrierMove, "carrierMove cannot be null");
        Map<String, Set<String>> selectedByShipment = indexSelections(selectedShipmentLabelIds);

        List<PrintTask> tasks = new ArrayList<>();
        int totalStops = carrierMove.stops().size();
        boolean anySelectedStop = false;
        for (PreparedStopGroup stop : carrierMove.stops()) {
            List<PrintTask> stopTasks = buildStopShipmentTasks(stop, selectedByShipment);
            if (stopTasks.isEmpty()) {
                continue;
            }
            anySelectedStop = true;
            tasks.addAll(stopTasks);
            if (carrierMove.includeInfoTags()) {
                tasks.add(new PrintTask(
                        PrintTask.Kind.STOP_INFO_TAG,
                        String.format("info-stop-%02d-of-%02d.zpl", stop.stopPosition(), totalStops),
                        "INFO-STOP " + stop.stopPosition()
                ));
            }
        }

        if (carrierMove.includeInfoTags() && anySelectedStop) {
            tasks.add(new PrintTask(
                    PrintTask.Kind.FINAL_INFO_TAG,
                    "info-final-cmid-" + safeSlug(carrierMove.carrierMoveId(), "carrier-move") + ".zpl",
                    "INFO-FINAL " + carrierMove.carrierMoveId()
            ));
        }

        return new PrintPlan(tasks);
    }

    private List<PrintTask> buildStopShipmentTasks(
            PreparedStopGroup stop,
            Map<String, Set<String>> selectedByShipment
    ) {
        List<PrintTask> tasks = new ArrayList<>();
        for (PreparedShipmentLabels shipment : stop.shipments()) {
            List<String> selectedLabels = selectedLabelsForShipment(shipment, selectedByShipment);
            if (!selectedByShipment.isEmpty() && selectedLabels.isEmpty()) {
                continue;
            }
            tasks.addAll(shipmentPlanner.build(shipment, selectedLabels).tasks());
        }
        return tasks;
    }

    private static List<String> selectedLabelsForShipment(
            PreparedShipmentLabels shipment,
            Map<String, Set<String>> selectedByShipment
    ) {
        if (selectedByShipment.isEmpty()) {
            return shipment.palletLabels().stream()
                    .map(LabelSelectionRef::labelId)
                    .toList();
        }
        Set<String> selected = selectedByShipment.get(normalize(shipment.shipmentId()));
        return selected == null ? List.of() : List.copyOf(selected);
    }

    private static Map<String, Set<String>> indexSelections(List<String> selectedShipmentLabelIds) {
        if (selectedShipmentLabelIds == null || selectedShipmentLabelIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> indexed = new LinkedHashMap<>();
        for (String selection : selectedShipmentLabelIds) {
            if (selection == null || selection.isBlank()) {
                continue;
            }
            String[] parts = selection.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("Carrier move label selection must be shipmentId:labelId");
            }
            indexed.computeIfAbsent(normalize(parts[0]), ignored -> new LinkedHashSet<>())
                    .add(parts[1].trim());
        }
        return indexed;
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeSlug(String value, String fallback) {
        String source = value == null ? "" : value.trim();
        if (source.isEmpty()) {
            source = fallback;
        }
        String slug = source.replaceAll("[^A-Za-z0-9._-]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        return slug.isEmpty() ? fallback : slug;
    }

    public record PrintPlan(List<PrintTask> tasks) {
        public PrintPlan {
            tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks cannot be null"));
        }
    }
}

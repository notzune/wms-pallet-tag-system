package com.tbg.wms.v2.app.labels;

import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import com.tbg.wms.v2.domain.print.PrintTask;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Builds the deterministic print-task order for one prepared shipment.
 */
public final class BuildShipmentPrintPlan {
    private static final int MAX_ARTIFACT_SLUG_LENGTH = 64;

    public PrintPlan build(PreparedShipmentLabels shipment, List<String> selectedLabelIds) {
        Objects.requireNonNull(shipment, "shipment cannot be null");
        Set<String> selected = normalizeSelection(selectedLabelIds);

        List<LabelSelectionRef> labelsToPrint = shipment.palletLabels().stream()
                .filter(label -> selected.isEmpty() || selected.contains(normalizeLabelId(label.labelId())))
                .toList();

        List<PrintTask> tasks = new ArrayList<>(labelsToPrint.size() + 1);
        String safeShipmentId = safeSlug(shipment.shipmentId(), "shipment");
        int total = labelsToPrint.size();
        for (int i = 0; i < labelsToPrint.size(); i++) {
            LabelSelectionRef label = labelsToPrint.get(i);
            String safeLabelId = safeSlug(label.labelId(), "lpn");
            int taskSequence = i + 1;
            tasks.add(new PrintTask(
                    PrintTask.Kind.PALLET_LABEL,
                    String.format("%s_%s_%d_of_%d.zpl", safeShipmentId, safeLabelId, taskSequence, total),
                    shipment.shipmentId() + ":" + label.labelId()
            ));
        }

        if (shipment.includeShipmentInfoTag()) {
            tasks.add(new PrintTask(
                    PrintTask.Kind.SHIPMENT_INFO_TAG,
                    "info-shipment-" + safeShipmentId + ".zpl",
                    "INFO-SHIPMENT " + shipment.shipmentId()
            ));
        }

        return new PrintPlan(tasks);
    }

    private static Set<String> normalizeSelection(List<String> selectedLabelIds) {
        if (selectedLabelIds == null || selectedLabelIds.isEmpty()) {
            return Set.of();
        }
        Set<String> selected = new LinkedHashSet<>();
        for (String selectedLabelId : selectedLabelIds) {
            if (selectedLabelId != null && !selectedLabelId.isBlank()) {
                selected.add(normalizeLabelId(selectedLabelId));
            }
        }
        return selected;
    }

    private static String normalizeLabelId(String labelId) {
        return labelId.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeSlug(String value, String fallback) {
        String source = value == null ? "" : value.trim();
        if (source.isEmpty()) {
            source = fallback;
        }
        String slug = source.replaceAll("[^A-Za-z0-9._-]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        if (slug.isEmpty()) {
            slug = fallback;
        }
        return slug.length() <= MAX_ARTIFACT_SLUG_LENGTH
                ? slug
                : slug.substring(0, MAX_ARTIFACT_SLUG_LENGTH);
    }

    public record PrintPlan(List<PrintTask> tasks) {
        public PrintPlan {
            tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks cannot be null"));
        }
    }
}

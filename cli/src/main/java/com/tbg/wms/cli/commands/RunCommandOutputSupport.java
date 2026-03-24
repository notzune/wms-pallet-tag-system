/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.AdvancedPrintWorkflowService;
import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.cli.gui.WorkflowPrintPlanSupport;
import com.tbg.wms.core.model.Lpn;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds operator-facing CLI summaries for run command workflows.
 *
 * <p>This keeps plan/completion text deterministic and separate from command execution so output
 * regressions can be tested without invoking live DB or printer code.</p>
 */
final class RunCommandOutputSupport {

    String buildShipmentPlanSummary(
            WorkflowPrintPlanSupport.ShipmentPlanSummary plan,
            LabelWorkflowService.PreparedJob prepared,
            List<Lpn> selectedLpns,
            String labelSelectionExpression,
            int maxLabelPreviewRows
    ) {
        Objects.requireNonNull(plan, "plan cannot be null");
        Objects.requireNonNull(prepared, "prepared cannot be null");
        Objects.requireNonNull(selectedLpns, "selectedLpns cannot be null");

        StringBuilder output = new StringBuilder();
        output.append(System.lineSeparator());
        output.append("=== Shipment Plan Summary ===").append(System.lineSeparator());
        output.append("Shipment: ").append(plan.getShipmentId()).append(System.lineSeparator());
        output.append("Total units: ").append(plan.getTotalUnits()).append(System.lineSeparator());
        output.append("Estimated pallets (footprint): ").append(plan.getEstimatedPallets()).append(System.lineSeparator());
        output.append("  Full pallets: ").append(plan.getFullPallets()).append(System.lineSeparator());
        output.append("  Partial pallets: ").append(plan.getPartialPallets()).append(System.lineSeparator());
        if (!plan.getMissingFootprintSkus().isEmpty()) {
            output.append("Missing footprint SKUs: ")
                    .append(String.join(", ", plan.getMissingFootprintSkus()))
                    .append(System.lineSeparator());
        }
        output.append("Labels selected: ").append(plan.getSelectedLabels()).append(" / ").append(plan.getTotalLabels()).append(System.lineSeparator());
        if (labelSelectionExpression != null && !labelSelectionExpression.isBlank()) {
            output.append("Selection: ").append(labelSelectionExpression.trim()).append(System.lineSeparator());
        }
        output.append("Info Tags: ").append(plan.getInfoTagCount()).append(System.lineSeparator());
        output.append(buildShipmentLabelPreview(prepared, selectedLpns, maxLabelPreviewRows));
        output.append("=============================").append(System.lineSeparator());
        output.append(System.lineSeparator());
        return output.toString();
    }

    String buildCarrierMovePlanSummary(WorkflowPrintPlanSupport.CarrierMovePlanSummary plan) {
        Objects.requireNonNull(plan, "plan cannot be null");
        String lineSeparator = System.lineSeparator();
        return lineSeparator
                + "=== Carrier Move Plan Summary ===" + lineSeparator
                + "Carrier Move: " + plan.getCarrierMoveId() + lineSeparator
                + "Stops: " + plan.getTotalStops() + lineSeparator
                + "Total units: " + plan.getTotalUnits() + lineSeparator
                + "Estimated pallets (footprint): " + plan.getEstimatedPallets() + lineSeparator
                + "  Full pallets: " + plan.getFullPallets() + lineSeparator
                + "  Partial pallets: " + plan.getPartialPallets() + lineSeparator
                + "Labels: " + plan.getTotalLabels() + lineSeparator
                + "Info Tags: " + plan.getInfoTagCount() + lineSeparator
                + "=================================" + lineSeparator
                + lineSeparator;
    }

    String buildCompletionMessage(AdvancedPrintWorkflowService.PrintResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        StringBuilder output = new StringBuilder();
        output.append("Success! Generated ")
                .append(result.getLabelsPrinted())
                .append(" label(s) and ")
                .append(result.getInfoTagsPrinted())
                .append(" info tag(s)")
                .append(System.lineSeparator());
        output.append("Output saved to: ").append(result.getOutputDirectory().toAbsolutePath()).append(System.lineSeparator());
        if (result.isPrintToFile()) {
            output.append("(Print-to-file mode: labels were not sent to printer)").append(System.lineSeparator());
        } else {
            output.append("Printed to: ")
                    .append(result.getPrinterId())
                    .append(" (")
                    .append(result.getPrinterEndpoint())
                    .append(")")
                    .append(System.lineSeparator());
        }
        return output.toString();
    }

    String buildShipmentLabelPreview(LabelWorkflowService.PreparedJob prepared, List<Lpn> selectedLpns, int maxLabelPreviewRows) {
        Objects.requireNonNull(prepared, "prepared cannot be null");
        Objects.requireNonNull(selectedLpns, "selectedLpns cannot be null");

        StringBuilder output = new StringBuilder("Label Preview:").append(System.lineSeparator());
        Set<String> selectedIds = new HashSet<>();
        for (Lpn selectedLpn : selectedLpns) {
            if (selectedLpn != null && selectedLpn.getLpnId() != null) {
                selectedIds.add(selectedLpn.getLpnId());
            }
        }
        for (int i = 0; i < prepared.getLpnsForLabels().size() && i < maxLabelPreviewRows; i++) {
            Lpn lpn = prepared.getLpnsForLabels().get(i);
            boolean selected = lpn != null && selectedIds.contains(lpn.getLpnId());
            output.append("  [")
                    .append(selected ? "x" : " ")
                    .append("] ")
                    .append(i + 1)
                    .append(". ")
                    .append(renderLpnPreviewId(lpn))
                    .append(System.lineSeparator());
        }
        if (prepared.getLpnsForLabels().size() > maxLabelPreviewRows) {
            output.append("  ... showing first ").append(maxLabelPreviewRows).append(" labels").append(System.lineSeparator());
        }
        return output.toString();
    }

    private String renderLpnPreviewId(Lpn lpn) {
        if (lpn == null || lpn.getLpnId() == null || lpn.getLpnId().isBlank()) {
            return "UNKNOWN";
        }
        return lpn.getLpnId();
    }
}

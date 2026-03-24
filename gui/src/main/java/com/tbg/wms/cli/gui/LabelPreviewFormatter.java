/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.cli.gui;

import java.util.Objects;

/**
 * Builds text shown by the GUI preview panes and queue preview dialog.
 */
final class LabelPreviewFormatter {
    private static final String SHIPMENT_TABLE_SEPARATOR =
            "----------------------------------------------------------------------------------------------------\n";
    private final PreviewStopDetailsSupport stopDetailsSupport = new PreviewStopDetailsSupport(this);
    private final CarrierMovePreviewSupport carrierMovePreviewSupport = new CarrierMovePreviewSupport();

    static String value(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    String buildShipmentSummaryText(
            LabelWorkflowService.PreparedJob job,
            java.util.List<com.tbg.wms.core.model.Lpn> selectedLpns,
            int infoTagsToGenerate
    ) {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(selectedLpns, "selectedLpns cannot be null");
        WorkflowPrintPlanSupport.ShipmentPlanSummary plan =
                WorkflowPrintPlanSupport.buildShipmentPlan(job, selectedLpns, infoTagsToGenerate);
        StringBuilder summary = new StringBuilder();
        summary.append("Shipment: ").append(job.getShipment().getShipmentId()).append('\n');
        summary.append("Order: ").append(value(job.getShipment().getOrderId())).append('\n');
        summary.append("Ship To: ").append(value(job.getShipment().getShipToName())).append('\n');
        summary.append("Address: ").append(value(job.getShipment().getShipToAddress1())).append(", ")
                .append(value(job.getShipment().getShipToCity())).append(", ")
                .append(value(job.getShipment().getShipToState())).append(" ")
                .append(value(job.getShipment().getShipToZip())).append('\n');
        summary.append("PO: ").append(value(job.getShipment().getCustomerPo())).append('\n');
        summary.append("Location No: ").append(value(job.getShipment().getLocationNumber())).append('\n');
        summary.append("Carrier Move: ").append(value(job.getShipment().getCarrierCode())).append(" ")
                .append(value(job.getShipment().getCarrierMoveId())).append('\n');
        summary.append("Staging Location: ").append(value(job.getStagingLocation())).append('\n');
        summary.append('\n');
        summary.append("Label Plan:\n");
        if (job.getShipment().getLpnCount() > 0) {
            summary.append(" - Actual LPNs: ").append(job.getShipment().getLpnCount()).append('\n');
        }
        summary.append(" - Labels To Generate: ").append(plan.getSelectedLabels())
                .append(" of ").append(plan.getTotalLabels()).append('\n');
        summary.append(" - Info Tags To Generate: ").append(plan.getInfoTagCount()).append('\n');
        summary.append(" - Total Documents To Generate: ").append(plan.getSelectedDocuments()).append('\n');
        summary.append(" - Virtual Labels Used: ").append(job.isUsingVirtualLabels() ? "YES" : "NO").append('\n');
        summary.append(" - Total Units: ").append(plan.getTotalUnits()).append('\n');
        summary.append(" - Estimated Pallets (Footprint): ").append(plan.getEstimatedPallets()).append('\n');
        summary.append(" - Full Pallets (Footprint): ").append(plan.getFullPallets()).append('\n');
        summary.append(" - Partial Pallets (Footprint): ").append(plan.getPartialPallets()).append('\n');
        summary.append(" - Missing Footprint SKUs: ")
                .append(plan.getMissingFootprintSkus().isEmpty()
                        ? "None"
                        : String.join(", ", plan.getMissingFootprintSkus()))
                .append('\n');
        return summary.toString();
    }

    String buildShipmentMathText(
            LabelWorkflowService.PreparedJob job,
            int maxPreviewSkuRowsPerShipment,
            int selectedLabels,
            int infoTagCount
    ) {
        Objects.requireNonNull(job, "job cannot be null");
        StringBuilder math = new StringBuilder();
        math.append("Pallet Math (Full vs Partial)\n");
        math.append(String.format("%-20s %-10s %-14s %-8s %-10s %-10s %s%n",
                "SKU", "Units", "Units/Pallet", "Full", "Partial", "TotalPal", "Description"));
        math.append(SHIPMENT_TABLE_SEPARATOR);
        int skuRows = 0;
        for (LabelWorkflowService.SkuMathRow row : job.getSkuMathRows()) {
            if (skuRows >= maxPreviewSkuRowsPerShipment) {
                break;
            }
            math.append(String.format("%-20s %-10d %-14s %-8d %-10d %-10d %s%n",
                    value(row.getSku()),
                    row.getUnits(),
                    row.getUnitsPerPallet() == null ? "-" : row.getUnitsPerPallet().toString(),
                    row.getFullPallets(),
                    row.getPartialPallets(),
                    row.getEstimatedPallets(),
                    value(row.getDescription())));
            skuRows++;
        }
        if (job.getSkuMathRows().size() > maxPreviewSkuRowsPerShipment) {
            math.append("Preview Notice: Showing first ")
                    .append(maxPreviewSkuRowsPerShipment)
                    .append(" SKU rows.\n");
        }
        int totalFull = job.getPlanResult().getFullPallets();
        int totalPartial = job.getPlanResult().getPartialPallets();
        int totalLabels = job.getPlanResult().getEstimatedPallets();
        math.append(SHIPMENT_TABLE_SEPARATOR);
        math.append(String.format(
                "Totals -> Full: %d | Partial: %d | Labels Needed (Footprint): %d | Actual Labels: %d | Selected Labels: %d | Info Tags: %d | Total Documents: %d%n",
                totalFull,
                totalPartial,
                totalLabels,
                job.getLpnsForLabels().size(),
                selectedLabels,
                infoTagCount,
                selectedLabels + infoTagCount
        ));
        return math.toString();
    }

    String buildCarrierMoveSummary(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            int selectedLabels,
            int infoTagCount
    ) {
        return carrierMovePreviewSupport.buildCarrierMoveSummary(job, selectedLabels, infoTagCount);
    }

    String buildCarrierMoveMathText(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
                                    int maxPreviewStops,
                                    int maxPreviewShipmentsPerStop,
                                    int selectedLabels,
                                    int infoTagCount) {
        return carrierMovePreviewSupport.buildCarrierMoveMathText(
                job,
                maxPreviewStops,
                maxPreviewShipmentsPerStop,
                selectedLabels,
                infoTagCount
        );
    }

    String stopPreviewLabel(AdvancedPrintWorkflowService.PreparedStopGroup stop) {
        return stopDetailsSupport.stopPreviewLabel(stop);
    }

    String buildStopDetailsText(AdvancedPrintWorkflowService.PreparedStopGroup stop,
                                int maxPreviewShipmentsPerStop,
                                int maxPreviewSkuRowsPerShipment) {
        return stopDetailsSupport.buildStopDetailsText(stop, maxPreviewShipmentsPerStop, maxPreviewSkuRowsPerShipment);
    }

    String buildQueuePreview(AdvancedPrintWorkflowService.PreparedQueueJob queueJob) {
        return carrierMovePreviewSupport.buildQueuePreview(queueJob);
    }
}

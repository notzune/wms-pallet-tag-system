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

    private static String value(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    String buildShipmentSummaryText(LabelWorkflowService.PreparedJob job, int infoTagsToGenerate) {
        Objects.requireNonNull(job, "job cannot be null");
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
        summary.append(" - Labels To Generate: ").append(job.getLpnsForLabels().size()).append('\n');
        summary.append(" - Info Tags To Generate: ").append(infoTagsToGenerate).append('\n');
        summary.append(" - Virtual Labels Used: ").append(job.isUsingVirtualLabels() ? "YES" : "NO").append('\n');
        summary.append(" - Total Units: ").append(job.getPlanResult().getTotalUnits()).append('\n');
        summary.append(" - Estimated Pallets (Footprint): ").append(job.getPlanResult().getEstimatedPallets()).append('\n');
        summary.append(" - Full Pallets (Footprint): ").append(job.getPlanResult().getFullPallets()).append('\n');
        summary.append(" - Partial Pallets (Footprint): ").append(job.getPlanResult().getPartialPallets()).append('\n');
        summary.append(" - Missing Footprint SKUs: ")
                .append(job.getPlanResult().getSkusMissingFootprint().isEmpty()
                        ? "None"
                        : String.join(", ", job.getPlanResult().getSkusMissingFootprint()))
                .append('\n');
        return summary.toString();
    }

    String buildShipmentMathText(LabelWorkflowService.PreparedJob job, int maxPreviewSkuRowsPerShipment) {
        Objects.requireNonNull(job, "job cannot be null");
        StringBuilder math = new StringBuilder();
        math.append("Pallet Math (Full vs Partial)\n");
        math.append(String.format("%-20s %-10s %-14s %-8s %-10s %-10s %s%n",
                "SKU", "Units", "Units/Pallet", "Full", "Partial", "TotalPal", "Description"));
        math.append("----------------------------------------------------------------------------------------------------\n");
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
        math.append("----------------------------------------------------------------------------------------------------\n");
        math.append(String.format("Totals -> Full: %d | Partial: %d | Labels Needed (Footprint): %d | Actual Labels: %d%n",
                totalFull, totalPartial, totalLabels, job.getLpnsForLabels().size()));
        return math.toString();
    }

    String buildCarrierMoveSummary(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        StringBuilder summary = new StringBuilder();
        summary.append("Carrier Move ID: ").append(job.getCarrierMoveId()).append('\n');
        summary.append("Total Stops: ").append(job.getTotalStops()).append('\n');
        summary.append("Estimated Labels: ").append(countCarrierMoveLabels(job)).append('\n');
        summary.append("Estimated Info Tags: ").append(job.getTotalStops() + 1).append('\n');
        return summary.toString();
    }

    String buildCarrierMoveMathText(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
                                    int maxPreviewStops,
                                    int maxPreviewShipmentsPerStop) {
        StringBuilder math = new StringBuilder();
        math.append("Carrier Move Pallet Math (Full vs Partial)\n");
        math.append(String.format("%-8s %-16s %-10s %-10s %-10s %-10s %-10s %s%n",
                "Stop", "Shipment", "Units", "Full", "Partial", "TotalPal", "Labels", "Ship To"));
        math.append("----------------------------------------------------------------------------------------------------------------------\n");
        CarrierMoveTotals totals = new CarrierMoveTotals();
        appendCarrierMoveMathRows(math, totals, job, maxPreviewStops, maxPreviewShipmentsPerStop);
        math.append("----------------------------------------------------------------------------------------------------------------------\n");
        math.append(String.format("Totals -> Full: %d | Partial: %d | Labels Needed (Footprint): %d | Actual Labels: %d%n",
                totals.totalFull, totals.totalPartial, totals.totalLabelsNeeded, totals.totalActualLabels));
        return math.toString();
    }

    String stopPreviewLabel(AdvancedPrintWorkflowService.PreparedStopGroup stop) {
        return "Stop " + stop.getStopPosition()
                + (stop.getStopSequence() == null ? "" : " (Seq " + stop.getStopSequence() + ")");
    }

    String buildStopDetailsText(AdvancedPrintWorkflowService.PreparedStopGroup stop,
                                int maxPreviewShipmentsPerStop,
                                int maxPreviewSkuRowsPerShipment) {
        StringBuilder section = new StringBuilder();
        StopTotals totals = new StopTotals();
        int shipmentCount = 0;
        for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
            if (shipmentCount >= maxPreviewShipmentsPerStop) {
                break;
            }
            appendShipmentDetailsInStop(section, totals, shipmentJob, maxPreviewSkuRowsPerShipment);
            shipmentCount++;
        }
        appendStopPreviewNotice(section, stop, maxPreviewShipmentsPerStop);
        appendStopTotals(section, totals);
        return section.toString();
    }

    int countCarrierMoveLabels(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        int total = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                total += shipmentJob.getLpnsForLabels().size();
            }
        }
        return total;
    }

    String buildQueuePreview(AdvancedPrintWorkflowService.PreparedQueueJob queueJob) {
        StringBuilder preview = new StringBuilder();
        int totalLabels = 0;
        int totalInfoTags = 0;
        preview.append("Queue Items: ").append(queueJob.getItems().size()).append('\n');
        for (AdvancedPrintWorkflowService.PreparedQueueItem item : queueJob.getItems()) {
            if (item.getType() == AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE) {
                int labels = countCarrierMoveLabels(item.getCarrierMoveJob());
                int info = item.getCarrierMoveJob().getTotalStops() + 1;
                totalLabels += labels;
                totalInfoTags += info;
                preview.append(" - C:").append(item.getSourceId())
                        .append(" | stops=").append(item.getCarrierMoveJob().getTotalStops())
                        .append(" | labels=").append(labels)
                        .append(" | infoTags=").append(info)
                        .append('\n');
            } else {
                int labels = item.getShipmentJob().getLpnsForLabels().size();
                totalLabels += labels;
                totalInfoTags += 1;
                preview.append(" - S:").append(item.getSourceId())
                        .append(" | labels=").append(labels)
                        .append(" | infoTags=1")
                        .append('\n');
            }
        }
        preview.append('\n')
                .append("Total labels: ").append(totalLabels).append('\n')
                .append("Total info tags: ").append(totalInfoTags).append('\n');
        return preview.toString();
    }

    private void appendCarrierMoveMathRows(StringBuilder math,
                                           CarrierMoveTotals totals,
                                           AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
                                           int maxPreviewStops,
                                           int maxPreviewShipmentsPerStop) {
        int stopCount = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            if (stopCount >= maxPreviewStops) {
                break;
            }
            int shipmentCount = 0;
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                if (shipmentCount >= maxPreviewShipmentsPerStop) {
                    break;
                }
                appendCarrierMoveMathRow(math, totals, stop, shipmentJob);
                shipmentCount++;
            }
            stopCount++;
        }
    }

    private void appendCarrierMoveMathRow(StringBuilder math,
                                          CarrierMoveTotals totals,
                                          AdvancedPrintWorkflowService.PreparedStopGroup stop,
                                          LabelWorkflowService.PreparedJob shipmentJob) {
        int full = shipmentJob.getPlanResult().getFullPallets();
        int partial = shipmentJob.getPlanResult().getPartialPallets();
        int estimated = shipmentJob.getPlanResult().getEstimatedPallets();
        int labels = shipmentJob.getLpnsForLabels().size();
        int units = sumUnits(shipmentJob.getSkuMathRows());
        totals.totalFull += full;
        totals.totalPartial += partial;
        totals.totalLabelsNeeded += estimated;
        totals.totalActualLabels += labels;
        math.append(String.format("%-8d %-16s %-10d %-10d %-10d %-10d %-10d %s%n",
                stop.getStopPosition(),
                value(shipmentJob.getShipmentId()),
                units,
                full,
                partial,
                estimated,
                labels,
                value(shipmentJob.getShipment().getShipToName())));
    }

    private int sumUnits(java.util.List<LabelWorkflowService.SkuMathRow> rows) {
        int total = 0;
        for (LabelWorkflowService.SkuMathRow row : rows) {
            total += row.getUnits();
        }
        return total;
    }

    private void appendShipmentDetailsInStop(StringBuilder section,
                                             StopTotals totals,
                                             LabelWorkflowService.PreparedJob shipmentJob,
                                             int maxPreviewSkuRowsPerShipment) {
        int full = shipmentJob.getPlanResult().getFullPallets();
        int partial = shipmentJob.getPlanResult().getPartialPallets();
        int labelsNeeded = shipmentJob.getPlanResult().getEstimatedPallets();
        int actualLabels = shipmentJob.getLpnsForLabels().size();
        totals.stopFull += full;
        totals.stopPartial += partial;
        totals.stopLabelsNeeded += labelsNeeded;
        totals.stopActualLabels += actualLabels;
        section.append("Shipment Summary: ").append(value(shipmentJob.getShipmentId()))
                .append(" | Labels Needed(Footprint): ").append(labelsNeeded)
                .append(" | Actual Labels: ").append(actualLabels)
                .append(" | Full: ").append(full)
                .append(" | Partial: ").append(partial)
                .append('\n')
                .append("  Ship To: ").append(value(shipmentJob.getShipment().getShipToName()))
                .append('\n')
                .append("----------------------------------------------------------------------------------------------------\n")
                .append(buildShipmentSummaryText(shipmentJob, 0))
                .append('\n')
                .append(buildShipmentMathText(shipmentJob, maxPreviewSkuRowsPerShipment))
                .append('\n')
                .append("====================================================================================================\n")
                .append('\n');
    }

    private void appendStopPreviewNotice(StringBuilder section,
                                         AdvancedPrintWorkflowService.PreparedStopGroup stop,
                                         int maxPreviewShipmentsPerStop) {
        if (stop.getShipmentJobs().size() > maxPreviewShipmentsPerStop) {
            section.append("Preview Notice: Showing first ").append(maxPreviewShipmentsPerStop)
                    .append(" shipments for this stop.\n");
        }
    }

    private void appendStopTotals(StringBuilder section, StopTotals totals) {
        section.append("Stop Totals -> Full: ").append(totals.stopFull)
                .append(" | Partial: ").append(totals.stopPartial)
                .append(" | Labels Needed(Footprint): ").append(totals.stopLabelsNeeded)
                .append(" | Actual Labels: ").append(totals.stopActualLabels)
                .append('\n');
    }

    private static final class CarrierMoveTotals {
        private int totalFull;
        private int totalPartial;
        private int totalLabelsNeeded;
        private int totalActualLabels;
    }

    private static final class StopTotals {
        private int stopFull;
        private int stopPartial;
        private int stopLabelsNeeded;
        private int stopActualLabels;
    }
}

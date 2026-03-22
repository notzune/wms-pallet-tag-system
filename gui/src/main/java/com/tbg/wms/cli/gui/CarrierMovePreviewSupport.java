package com.tbg.wms.cli.gui;

import java.util.Objects;

/**
 * Builds carrier-move and queue preview text so shipment formatting can stay separate.
 */
final class CarrierMovePreviewSupport {
    private static final String CARRIER_TABLE_SEPARATOR =
            "----------------------------------------------------------------------------------------------------------------------\n";

    String buildCarrierMoveSummary(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            int selectedLabels,
            int infoTagCount
    ) {
        WorkflowPrintPlanSupport.CarrierMovePlanSummary plan =
                WorkflowPrintPlanSupport.buildCarrierMovePlan(job, selectedLabels, infoTagCount);
        StringBuilder summary = new StringBuilder();
        summary.append("Carrier Move ID: ").append(plan.getCarrierMoveId()).append('\n');
        summary.append("Total Stops: ").append(plan.getTotalStops()).append('\n');
        summary.append("Labels To Generate: ").append(plan.getSelectedLabels())
                .append(" of ").append(plan.getTotalLabels()).append('\n');
        summary.append("Info Tags To Generate: ").append(plan.getInfoTagCount()).append('\n');
        summary.append("Total Documents To Generate: ").append(plan.getSelectedDocuments()).append('\n');
        return summary.toString();
    }

    String buildCarrierMoveMathText(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            int maxPreviewStops,
            int maxPreviewShipmentsPerStop,
            int selectedLabels,
            int infoTagCount
    ) {
        StringBuilder math = new StringBuilder();
        math.append("Carrier Move Pallet Math (Full vs Partial)\n");
        math.append(String.format("%-8s %-16s %-10s %-10s %-10s %-10s %-10s %s%n",
                "Stop", "Shipment", "Units", "Full", "Partial", "TotalPal", "Labels", "Ship To"));
        math.append(CARRIER_TABLE_SEPARATOR);
        CarrierMoveTotals totals = new CarrierMoveTotals();
        appendCarrierMoveMathRows(math, totals, job, maxPreviewStops, maxPreviewShipmentsPerStop);
        math.append(CARRIER_TABLE_SEPARATOR);
        math.append(String.format(
                "Totals -> Full: %d | Partial: %d | Labels Needed (Footprint): %d | Actual Labels: %d | Selected Labels: %d | Info Tags: %d | Total Documents: %d%n",
                totals.totalFull,
                totals.totalPartial,
                totals.totalLabelsNeeded,
                totals.totalActualLabels,
                selectedLabels,
                infoTagCount,
                selectedLabels + infoTagCount
        ));
        return math.toString();
    }

    String buildQueuePreview(AdvancedPrintWorkflowService.PreparedQueueJob queueJob) {
        Objects.requireNonNull(queueJob, "queueJob cannot be null");
        StringBuilder preview = new StringBuilder();
        int shipmentItems = 0;
        int carrierMoveItems = 0;
        int carrierStops = 0;
        int carrierShipments = 0;
        int totalLabels = 0;
        int totalInfoTags = 0;
        preview.append("Queue Items: ").append(queueJob.getItems().size()).append('\n');
        for (AdvancedPrintWorkflowService.PreparedQueueItem item : queueJob.getItems()) {
            if (item.getType() == AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE) {
                carrierMoveItems++;
                WorkflowPrintPlanSupport.CarrierMovePlanSummary plan =
                        WorkflowPrintPlanSupport.buildCarrierMovePlan(item.getCarrierMoveJob());
                carrierStops += plan.getTotalStops();
                carrierShipments += countCarrierMoveShipments(item.getCarrierMoveJob());
                totalLabels += plan.getTotalLabels();
                totalInfoTags += plan.getInfoTagCount();
                preview.append(" - C:").append(item.getSourceId())
                        .append(" | stops=").append(plan.getTotalStops())
                        .append(" | shipments=").append(countCarrierMoveShipments(item.getCarrierMoveJob()))
                        .append(" | labels=").append(plan.getTotalLabels())
                        .append(" | infoTags=").append(plan.getInfoTagCount())
                        .append('\n');
            } else {
                shipmentItems++;
                WorkflowPrintPlanSupport.ShipmentPlanSummary plan =
                        WorkflowPrintPlanSupport.buildShipmentPlan(
                                item.getShipmentJob(),
                                item.getShipmentJob().getLpnsForLabels(),
                                1
                        );
                totalLabels += plan.getTotalLabels();
                totalInfoTags += plan.getInfoTagCount();
                preview.append(" - S:").append(item.getSourceId())
                        .append(" | labels=").append(plan.getTotalLabels())
                        .append(" | infoTags=").append(plan.getInfoTagCount())
                        .append('\n');
            }
        }
        preview.append('\n')
                .append("Shipments: ").append(shipmentItems).append('\n')
                .append("Carrier Moves: ").append(carrierMoveItems).append('\n')
                .append("Carrier Stops: ").append(carrierStops).append('\n')
                .append("Carrier-Move Shipments: ").append(carrierShipments).append('\n')
                .append("Total Shipments Covered: ").append(shipmentItems + carrierShipments).append('\n')
                .append("Total labels: ").append(totalLabels).append('\n')
                .append("Total info tags: ").append(totalInfoTags).append('\n')
                .append("Total documents: ").append(totalLabels + totalInfoTags).append('\n');
        return preview.toString();
    }

    private int countCarrierMoveShipments(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        int total = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            total += stop.getShipmentJobs().size();
        }
        return total;
    }

    private void appendCarrierMoveMathRows(
            StringBuilder math,
            CarrierMoveTotals totals,
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            int maxPreviewStops,
            int maxPreviewShipmentsPerStop
    ) {
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

    private void appendCarrierMoveMathRow(
            StringBuilder math,
            CarrierMoveTotals totals,
            AdvancedPrintWorkflowService.PreparedStopGroup stop,
            LabelWorkflowService.PreparedJob shipmentJob
    ) {
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
                LabelPreviewFormatter.value(shipmentJob.getShipmentId()),
                units,
                full,
                partial,
                estimated,
                labels,
                LabelPreviewFormatter.value(shipmentJob.getShipment().getShipToName())));
    }

    private int sumUnits(java.util.List<LabelWorkflowService.SkuMathRow> rows) {
        int total = 0;
        for (LabelWorkflowService.SkuMathRow row : rows) {
            total += row.getUnits();
        }
        return total;
    }

    private static final class CarrierMoveTotals {
        private int totalFull;
        private int totalPartial;
        private int totalLabelsNeeded;
        private int totalActualLabels;
    }
}

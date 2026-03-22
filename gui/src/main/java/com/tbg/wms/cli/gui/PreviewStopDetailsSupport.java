package com.tbg.wms.cli.gui;

import java.util.Objects;

/**
 * Builds per-stop preview labels and details for carrier-move preview rendering.
 */
final class PreviewStopDetailsSupport {

    private static final String SHIPMENT_TABLE_SEPARATOR =
            "----------------------------------------------------------------------------------------------------\n";
    private static final String STOP_BLOCK_SEPARATOR =
            "====================================================================================================\n";

    private final LabelPreviewFormatter formatter;

    PreviewStopDetailsSupport(LabelPreviewFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter cannot be null");
    }

    String stopPreviewLabel(AdvancedPrintWorkflowService.PreparedStopGroup stop) {
        Objects.requireNonNull(stop, "stop cannot be null");
        return "Stop " + stop.getStopPosition()
                + (stop.getStopSequence() == null ? "" : " (Seq " + stop.getStopSequence() + ")");
    }

    String buildStopDetailsText(AdvancedPrintWorkflowService.PreparedStopGroup stop,
                                int maxPreviewShipmentsPerStop,
                                int maxPreviewSkuRowsPerShipment) {
        Objects.requireNonNull(stop, "stop cannot be null");
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
        section.append("Shipment Summary: ").append(LabelPreviewFormatter.value(shipmentJob.getShipmentId()))
                .append(" | Labels Needed(Footprint): ").append(labelsNeeded)
                .append(" | Actual Labels: ").append(actualLabels)
                .append(" | Full: ").append(full)
                .append(" | Partial: ").append(partial)
                .append('\n')
                .append("  Ship To: ").append(LabelPreviewFormatter.value(shipmentJob.getShipment().getShipToName()))
                .append('\n')
                .append(SHIPMENT_TABLE_SEPARATOR)
                .append(formatter.buildShipmentSummaryText(shipmentJob, shipmentJob.getLpnsForLabels(), 0))
                .append('\n')
                .append(formatter.buildShipmentMathText(
                        shipmentJob,
                        maxPreviewSkuRowsPerShipment,
                        shipmentJob.getLpnsForLabels().size(),
                        0
                ))
                .append('\n')
                .append(STOP_BLOCK_SEPARATOR)
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

    private static final class StopTotals {
        private int stopFull;
        private int stopPartial;
        private int stopLabelsNeeded;
        private int stopActualLabels;
    }
}

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;

import java.util.List;
import java.util.Objects;

/**
 * Shared plan summaries consumed by CLI and GUI formatting layers.
 */
public final class WorkflowPrintPlanSupport {

    private WorkflowPrintPlanSupport() {
    }

    public static ShipmentPlanSummary buildShipmentPlan(
            LabelWorkflowService.PreparedJob prepared,
            List<Lpn> selectedLpns,
            int infoTagCount
    ) {
        Objects.requireNonNull(prepared, "prepared cannot be null");
        Objects.requireNonNull(selectedLpns, "selectedLpns cannot be null");
        return new ShipmentPlanSummary(
                prepared.getShipmentId(),
                prepared.getPlanResult().getTotalUnits(),
                prepared.getPlanResult().getEstimatedPallets(),
                prepared.getPlanResult().getFullPallets(),
                prepared.getPlanResult().getPartialPallets(),
                prepared.getLpnsForLabels().size(),
                selectedLpns.size(),
                infoTagCount,
                prepared.getPlanResult().getSkusMissingFootprint()
        );
    }

    public static CarrierMovePlanSummary buildCarrierMovePlan(AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepared) {
        return buildCarrierMovePlan(
                prepared,
                countCarrierMoveLabels(prepared),
                prepared.getTotalStops() + 1
        );
    }

    public static CarrierMovePlanSummary buildCarrierMovePlan(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepared,
            int selectedLabels,
            int infoTagCount
    ) {
        Objects.requireNonNull(prepared, "prepared cannot be null");
        int full = 0;
        int partial = 0;
        int totalUnits = 0;
        int labels = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : prepared.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                full += shipmentJob.getPlanResult().getFullPallets();
                partial += shipmentJob.getPlanResult().getPartialPallets();
                totalUnits += shipmentJob.getPlanResult().getTotalUnits();
                labels += shipmentJob.getLpnsForLabels().size();
            }
        }
        return new CarrierMovePlanSummary(
                prepared.getCarrierMoveId(),
                prepared.getTotalStops(),
                totalUnits,
                full + partial,
                full,
                partial,
                labels,
                selectedLabels,
                infoTagCount
        );
    }

    private static int countCarrierMoveLabels(AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepared) {
        int labels = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : prepared.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                labels += shipmentJob.getLpnsForLabels().size();
            }
        }
        return labels;
    }

    public static final class ShipmentPlanSummary {
        private final String shipmentId;
        private final int totalUnits;
        private final int estimatedPallets;
        private final int fullPallets;
        private final int partialPallets;
        private final int totalLabels;
        private final int selectedLabels;
        private final int infoTagCount;
        private final List<String> missingFootprintSkus;

        private ShipmentPlanSummary(
                String shipmentId,
                int totalUnits,
                int estimatedPallets,
                int fullPallets,
                int partialPallets,
                int totalLabels,
                int selectedLabels,
                int infoTagCount,
                List<String> missingFootprintSkus
        ) {
            this.shipmentId = shipmentId;
            this.totalUnits = totalUnits;
            this.estimatedPallets = estimatedPallets;
            this.fullPallets = fullPallets;
            this.partialPallets = partialPallets;
            this.totalLabels = totalLabels;
            this.selectedLabels = selectedLabels;
            this.infoTagCount = infoTagCount;
            this.missingFootprintSkus = List.copyOf(Objects.requireNonNull(missingFootprintSkus, "missingFootprintSkus"));
        }

        public String getShipmentId() {
            return shipmentId;
        }

        public int getTotalUnits() {
            return totalUnits;
        }

        public int getEstimatedPallets() {
            return estimatedPallets;
        }

        public int getFullPallets() {
            return fullPallets;
        }

        public int getPartialPallets() {
            return partialPallets;
        }

        public int getTotalLabels() {
            return totalLabels;
        }

        public int getSelectedLabels() {
            return selectedLabels;
        }

        public int getInfoTagCount() {
            return infoTagCount;
        }

        public int getSelectedDocuments() {
            return selectedLabels + infoTagCount;
        }

        public List<String> getMissingFootprintSkus() {
            return missingFootprintSkus;
        }
    }

    public static final class CarrierMovePlanSummary {
        private final String carrierMoveId;
        private final int totalStops;
        private final int totalUnits;
        private final int estimatedPallets;
        private final int fullPallets;
        private final int partialPallets;
        private final int totalLabels;
        private final int selectedLabels;
        private final int infoTagCount;

        private CarrierMovePlanSummary(
                String carrierMoveId,
                int totalStops,
                int totalUnits,
                int estimatedPallets,
                int fullPallets,
                int partialPallets,
                int totalLabels,
                int selectedLabels,
                int infoTagCount
        ) {
            this.carrierMoveId = carrierMoveId;
            this.totalStops = totalStops;
            this.totalUnits = totalUnits;
            this.estimatedPallets = estimatedPallets;
            this.fullPallets = fullPallets;
            this.partialPallets = partialPallets;
            this.totalLabels = totalLabels;
            this.selectedLabels = selectedLabels;
            this.infoTagCount = infoTagCount;
        }

        public String getCarrierMoveId() {
            return carrierMoveId;
        }

        public int getTotalStops() {
            return totalStops;
        }

        public int getTotalUnits() {
            return totalUnits;
        }

        public int getEstimatedPallets() {
            return estimatedPallets;
        }

        public int getFullPallets() {
            return fullPallets;
        }

        public int getPartialPallets() {
            return partialPallets;
        }

        public int getTotalLabels() {
            return totalLabels;
        }

        public int getSelectedLabels() {
            return selectedLabels;
        }

        public int getInfoTagCount() {
            return infoTagCount;
        }

        public int getSelectedDocuments() {
            return selectedLabels + infoTagCount;
        }
    }
}

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Plans deterministic print tasks from prepared shipment and carrier-move jobs.
 *
 * <p>This helper owns task expansion, subset filtering, info-tag counting, and output artifact
 * naming so the workflow service can stay focused on loading jobs, resolving printers, and
 * executing checkpointed tasks.</p>
 */
final class PrintTaskPlanner {
    private static final PalletLabelPrintTaskSupport PALLET_LABEL_TASKS = new PalletLabelPrintTaskSupport();
    private static final InfoTagPrintTaskSupport INFO_TAG_TASKS = new InfoTagPrintTaskSupport();

    private PrintTaskPlanner() {
    }

    /**
     * Resolves a selected LPN subset against the prepared shipment while preserving the shipment's
     * canonical label order.
     */
    static List<Lpn> filterLpnsForPrint(List<Lpn> availableLpns, List<Lpn> selectedLpns) {
        return PrintTaskSelectionSupport.filterLpnsForPrint(availableLpns, selectedLpns);
    }

    /**
     * Builds the default select-all carrier-move selection set in preview order.
     */
    static List<LabelSelectionRef> collectAllCarrierMoveLabelSelections(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        return PrintTaskSelectionSupport.collectAllCarrierMoveLabelSelections(job);
    }

    /**
     * Shipment jobs emit one info tag only when at least one label remains selected.
     */
    static int countShipmentInfoTags(int selectedLabels, boolean includeInfoTags) {
        return PrintTaskSelectionSupport.countShipmentInfoTags(selectedLabels, includeInfoTags);
    }

    /**
     * Carrier moves emit one stop info tag per selected stop plus one final summary tag.
     */
    static int countCarrierMoveInfoTags(List<LabelSelectionRef> selectedLabels, boolean includeInfoTags) {
        return PrintTaskSelectionSupport.countCarrierMoveInfoTags(selectedLabels, includeInfoTags);
    }

    static List<AdvancedPrintWorkflowService.PrintTask> buildShipmentTasks(ShipmentPrintBatch batch) {
        Objects.requireNonNull(batch, "batch cannot be null");
        List<AdvancedPrintWorkflowService.PrintTask> tasks = new ArrayList<>(PALLET_LABEL_TASKS.buildPalletLabelTasks(batch));

        if (batch.isIncludeShipmentInfoTag()) {
            tasks.add(INFO_TAG_TASKS.buildShipmentInfoTask(batch.getShipmentJob()));
        }
        return tasks;
    }

    static List<AdvancedPrintWorkflowService.PrintTask> buildCarrierMoveTasks(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            List<LabelSelectionRef> selectedLabels,
            boolean includeInfoTags
    ) {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(selectedLabels, "selectedLabels cannot be null");
        Map<String, LinkedHashSet<String>> selectedLpnsByShipment =
                PrintTaskSelectionSupport.indexCarrierMoveSelections(selectedLabels);
        if (selectedLpnsByShipment.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }
        List<CarrierMoveStopBatch> stopBatches = buildCarrierMoveStopBatches(job, selectedLpnsByShipment);
        List<AdvancedPrintWorkflowService.PrintTask> tasks =
                new ArrayList<>(estimateCarrierMoveTaskCount(stopBatches, includeInfoTags));
        for (CarrierMoveStopBatch stopBatch : stopBatches) {
            for (ShipmentPrintBatch shipmentBatch : stopBatch.getShipmentBatches()) {
                tasks.addAll(buildShipmentTasks(shipmentBatch));
            }
            if (includeInfoTags) {
                tasks.add(buildStopInfoTask(job, stopBatch));
            }
        }

        if (includeInfoTags && !stopBatches.isEmpty()) {
            tasks.add(INFO_TAG_TASKS.buildFinalInfoTask(job));
        }
        return tasks;
    }

    private static int estimateCarrierMoveTaskCount(List<CarrierMoveStopBatch> stopBatches, boolean includeInfoTags) {
        int total = includeInfoTags && !stopBatches.isEmpty() ? 1 : 0;
        for (CarrierMoveStopBatch stopBatch : stopBatches) {
            if (includeInfoTags) {
                total += 1;
            }
            for (ShipmentPrintBatch shipmentBatch : stopBatch.getShipmentBatches()) {
                total += shipmentBatch.getLpnsToPrint().size();
                if (shipmentBatch.isIncludeShipmentInfoTag()) {
                    total += 1;
                }
            }
        }
        return total;
    }

    private static List<CarrierMoveStopBatch> buildCarrierMoveStopBatches(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            Map<String, LinkedHashSet<String>> selectedLpnsByShipment
    ) {
        List<CarrierMoveStopBatch> stopBatches = new ArrayList<>(job.getStopGroups().size());
        int totalStops = job.getStopGroups().size();
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            List<ShipmentPrintBatch> shipmentBatches = new ArrayList<>(stop.getShipmentJobs().size());
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                List<Lpn> selectedLpns =
                        PrintTaskSelectionSupport.filterCarrierMoveShipmentLpns(shipmentJob, selectedLpnsByShipment);
                if (selectedLpns.isEmpty()) {
                    continue;
                }
                shipmentBatches.add(ShipmentPrintBatch.forCarrierStop(
                        shipmentJob,
                        selectedLpns,
                        stop.getStopSequence(),
                        stop.getStopPosition()
                ));
            }
            if (!shipmentBatches.isEmpty()) {
                stopBatches.add(new CarrierMoveStopBatch(stop, shipmentBatches, totalStops));
            }
        }
        return stopBatches;
    }

    private static AdvancedPrintWorkflowService.PrintTask buildStopInfoTask(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job,
            CarrierMoveStopBatch stopBatch
    ) {
        List<LabelWorkflowService.PreparedJob> shipmentJobs = new ArrayList<>(stopBatch.getShipmentBatches().size());
        for (ShipmentPrintBatch shipmentBatch : stopBatch.getShipmentBatches()) {
            shipmentJobs.add(shipmentBatch.getShipmentJob());
        }
        return INFO_TAG_TASKS.buildStopInfoTask(
                job.getCarrierMoveId(),
                stopBatch.getStop().getStopPosition(),
                stopBatch.getTotalStops(),
                stopBatch.getStop().getStopSequence(),
                shipmentJobs
        );
    }

    static final class ShipmentPrintBatch {
        private final LabelWorkflowService.PreparedJob shipmentJob;
        private final List<Lpn> lpnsToPrint;
        private final Integer stopSequence;
        private final Integer stopPosition;
        private final boolean includeShipmentInfoTag;

        private ShipmentPrintBatch(
                LabelWorkflowService.PreparedJob shipmentJob,
                List<Lpn> lpnsToPrint,
                Integer stopSequence,
                Integer stopPosition,
                boolean includeShipmentInfoTag
        ) {
            this.shipmentJob = Objects.requireNonNull(shipmentJob, "shipmentJob");
            this.lpnsToPrint = List.copyOf(Objects.requireNonNull(lpnsToPrint, "lpnsToPrint"));
            this.stopSequence = stopSequence;
            this.stopPosition = stopPosition;
            this.includeShipmentInfoTag = includeShipmentInfoTag;
        }

        static ShipmentPrintBatch forShipment(
                LabelWorkflowService.PreparedJob shipmentJob,
                List<Lpn> lpnsToPrint,
                boolean includeInfoTags
        ) {
            return new ShipmentPrintBatch(shipmentJob, lpnsToPrint, null, null, includeInfoTags);
        }

        static ShipmentPrintBatch forCarrierStop(
                LabelWorkflowService.PreparedJob shipmentJob,
                List<Lpn> lpnsToPrint,
                Integer stopSequence,
                int stopPosition
        ) {
            return new ShipmentPrintBatch(shipmentJob, lpnsToPrint, stopSequence, stopPosition, false);
        }

        LabelWorkflowService.PreparedJob getShipmentJob() {
            return shipmentJob;
        }

        List<Lpn> getLpnsToPrint() {
            return lpnsToPrint;
        }

        Integer getStopSequence() {
            return stopSequence;
        }

        Integer getStopPosition() {
            return stopPosition;
        }

        boolean isIncludeShipmentInfoTag() {
            return includeShipmentInfoTag;
        }
    }

    private static final class CarrierMoveStopBatch {
        private final AdvancedPrintWorkflowService.PreparedStopGroup stop;
        private final List<ShipmentPrintBatch> shipmentBatches;
        private final int totalStops;

        private CarrierMoveStopBatch(
                AdvancedPrintWorkflowService.PreparedStopGroup stop,
                List<ShipmentPrintBatch> shipmentBatches,
                int totalStops
        ) {
            this.stop = Objects.requireNonNull(stop, "stop");
            this.shipmentBatches = List.copyOf(Objects.requireNonNull(shipmentBatches, "shipmentBatches"));
            this.totalStops = totalStops;
        }

        AdvancedPrintWorkflowService.PreparedStopGroup getStop() {
            return stop;
        }

        List<ShipmentPrintBatch> getShipmentBatches() {
            return shipmentBatches;
        }

        int getTotalStops() {
            return totalStops;
        }
    }
}

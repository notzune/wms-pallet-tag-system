package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelDataBuilder;
import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.label.LabelType;
import com.tbg.wms.core.labeling.LabelingSupport;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.template.ZplTemplateEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final int MAX_LABELS_PER_JOB = 10_000;
    private static final int MAX_ARTIFACT_SLUG_LENGTH = 64;

    private PrintTaskPlanner() {
    }

    /**
     * Resolves a selected LPN subset against the prepared shipment while preserving the shipment's
     * canonical label order.
     */
    static List<Lpn> filterLpnsForPrint(List<Lpn> availableLpns, List<Lpn> selectedLpns) {
        Objects.requireNonNull(availableLpns, "availableLpns cannot be null");
        Objects.requireNonNull(selectedLpns, "selectedLpns cannot be null");

        if (selectedLpns.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }

        LinkedHashSet<String> selectedIds = new LinkedHashSet<>();
        for (Lpn lpn : selectedLpns) {
            if (lpn != null && lpn.getLpnId() != null) {
                selectedIds.add(lpn.getLpnId());
            }
        }
        if (selectedIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }

        List<Lpn> filtered = new ArrayList<>(selectedIds.size());
        for (Lpn lpn : availableLpns) {
            if (lpn != null && selectedIds.contains(lpn.getLpnId())) {
                filtered.add(lpn);
            }
        }
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("Selected labels are no longer available for printing.");
        }
        return filtered;
    }

    /**
     * Builds the default select-all carrier-move selection set in preview order.
     */
    static List<LabelSelectionRef> collectAllCarrierMoveLabelSelections(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        List<LabelSelectionRef> selected = new ArrayList<>();
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                for (Lpn lpn : shipmentJob.getLpnsForLabels()) {
                    if (lpn != null && lpn.getLpnId() != null) {
                        selected.add(LabelSelectionRef.forCarrierMove(
                                selected.size() + 1,
                                shipmentJob.getShipmentId(),
                                lpn.getLpnId(),
                                stop.getStopPosition()
                        ));
                    }
                }
            }
        }
        return selected;
    }

    /**
     * Shipment jobs emit one info tag only when at least one label remains selected.
     */
    static int countShipmentInfoTags(int selectedLabels, boolean includeInfoTags) {
        return includeInfoTags && selectedLabels > 0 ? 1 : 0;
    }

    /**
     * Carrier moves emit one stop info tag per selected stop plus one final summary tag.
     */
    static int countCarrierMoveInfoTags(List<LabelSelectionRef> selectedLabels, boolean includeInfoTags) {
        Objects.requireNonNull(selectedLabels, "selectedLabels cannot be null");
        if (!includeInfoTags || selectedLabels.isEmpty()) {
            return 0;
        }
        LinkedHashSet<Integer> distinctStops = new LinkedHashSet<>();
        for (LabelSelectionRef selectedLabel : selectedLabels) {
            if (selectedLabel != null && selectedLabel.getStopPosition() != null) {
                distinctStops.add(selectedLabel.getStopPosition());
            }
        }
        return distinctStops.isEmpty() ? 0 : distinctStops.size() + 1;
    }

    static List<AdvancedPrintWorkflowService.PrintTask> buildShipmentTasks(ShipmentPrintBatch batch) {
        Objects.requireNonNull(batch, "batch cannot be null");
        LabelWorkflowService.PreparedJob job = batch.getShipmentJob();
        List<Lpn> lpnsToPrint = batch.getLpnsToPrint();
        LabelDataBuilder builder = new LabelDataBuilder(job.getSkuMapping(), job.getSiteConfig(), job.getFootprintBySku());
        List<AdvancedPrintWorkflowService.PrintTask> tasks = new ArrayList<>();
        Shipment shipmentForLabels = LabelingSupport.buildShipmentForLabeling(job.getShipment(), lpnsToPrint);
        int labelCount = lpnsToPrint.size();
        if (labelCount > MAX_LABELS_PER_JOB) {
            throw new IllegalArgumentException("Label count exceeds max limit: " + MAX_LABELS_PER_JOB);
        }
        String safeShipmentId = ArtifactNameSupport.safeSlug(job.getShipmentId(), "shipment", MAX_ARTIFACT_SLUG_LENGTH);
        for (int i = 0; i < labelCount; i++) {
            Lpn lpn = lpnsToPrint.get(i);
            Map<String, String> data = new LinkedHashMap<>(builder.build(shipmentForLabels, lpn, i, LabelType.WALMART_CANADA_GRID));
            if (batch.getStopSequence() != null) {
                data.put("stopSequence", String.valueOf(batch.getStopSequence()));
            }
            if (job.isUsingVirtualLabels()) {
                data.put("palletSeq", String.valueOf(i + 1));
                data.put("palletTotal", String.valueOf(labelCount));
            }
            String zpl = ZplTemplateEngine.generate(job.getTemplate(), data);
            String safeLpnId = ArtifactNameSupport.safeSlug(lpn.getLpnId(), "lpn", MAX_ARTIFACT_SLUG_LENGTH);
            String fileName = String.format("%s_%s_%d_of_%d.zpl", safeShipmentId, safeLpnId, i + 1, labelCount);
            String payload = job.getShipmentId() + ":" + lpn.getLpnId()
                    + (batch.getStopPosition() == null ? "" : (" stop " + batch.getStopPosition()));
            tasks.add(new AdvancedPrintWorkflowService.PrintTask(
                    AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL,
                    fileName,
                    zpl,
                    payload
            ));
        }

        if (batch.isIncludeShipmentInfoTag()) {
            String infoFile = "info-shipment-" + safeShipmentId + ".zpl";
            String infoZpl = InfoTagZplBuilder.buildShipmentInfoTag(job);
            tasks.add(new AdvancedPrintWorkflowService.PrintTask(
                    AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG,
                    infoFile,
                    infoZpl,
                    "INFO-SHIPMENT " + job.getShipmentId()
            ));
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
        Map<String, LinkedHashSet<String>> selectedLpnsByShipment = indexCarrierMoveSelections(selectedLabels);
        if (selectedLpnsByShipment.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }
        List<CarrierMoveStopBatch> stopBatches = buildCarrierMoveStopBatches(job, selectedLpnsByShipment);
        List<AdvancedPrintWorkflowService.PrintTask> tasks = new ArrayList<>();
        for (CarrierMoveStopBatch stopBatch : stopBatches) {
            for (ShipmentPrintBatch shipmentBatch : stopBatch.getShipmentBatches()) {
                tasks.addAll(buildShipmentTasks(shipmentBatch));
            }
            if (includeInfoTags) {
                tasks.add(buildStopInfoTask(job, stopBatch));
            }
        }

        if (includeInfoTags && !stopBatches.isEmpty()) {
            String finalFile = "info-final-cmid-" +
                    ArtifactNameSupport.safeSlug(job.getCarrierMoveId(), "carrier-move", MAX_ARTIFACT_SLUG_LENGTH) +
                    ".zpl";
            String finalInfo = InfoTagZplBuilder.buildFinalInfoTag(job);
            tasks.add(new AdvancedPrintWorkflowService.PrintTask(
                    AdvancedPrintWorkflowService.TaskKind.FINAL_INFO_TAG,
                    finalFile,
                    finalInfo,
                    "INFO-FINAL " + job.getCarrierMoveId()
            ));
        }
        return tasks;
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
                List<Lpn> selectedLpns = filterCarrierMoveShipmentLpns(shipmentJob, selectedLpnsByShipment);
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
        List<String> shipmentIds = new ArrayList<>(stopBatch.getShipmentBatches().size());
        List<LabelWorkflowService.PreparedJob> shipmentJobs = new ArrayList<>(stopBatch.getShipmentBatches().size());
        for (ShipmentPrintBatch shipmentBatch : stopBatch.getShipmentBatches()) {
            shipmentIds.add(shipmentBatch.getShipmentJob().getShipmentId());
            shipmentJobs.add(shipmentBatch.getShipmentJob());
        }
        String stopInfoFile = String.format(
                "info-stop-%02d-of-%02d.zpl",
                stopBatch.getStop().getStopPosition(),
                stopBatch.getTotalStops()
        );
        String stopInfo = InfoTagZplBuilder.buildStopInfoTag(
                job.getCarrierMoveId(),
                stopBatch.getStop().getStopPosition(),
                stopBatch.getTotalStops(),
                stopBatch.getStop().getStopSequence(),
                shipmentIds,
                shipmentJobs
        );
        return new AdvancedPrintWorkflowService.PrintTask(
                AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG,
                stopInfoFile,
                stopInfo,
                "INFO-STOP " + stopBatch.getStop().getStopPosition()
        );
    }

    private static Map<String, LinkedHashSet<String>> indexCarrierMoveSelections(List<LabelSelectionRef> selectedLabels) {
        Map<String, LinkedHashSet<String>> selectedLpnsByShipment = new LinkedHashMap<>();
        for (LabelSelectionRef selection : selectedLabels) {
            if (selection == null || selection.getShipmentId() == null || selection.getShipmentId().isBlank()
                    || selection.getLpnId() == null || selection.getLpnId().isBlank()) {
                continue;
            }
            selectedLpnsByShipment
                    .computeIfAbsent(selection.getShipmentId(), ignored -> new LinkedHashSet<>())
                    .add(selection.getLpnId());
        }
        return selectedLpnsByShipment;
    }

    private static List<Lpn> filterCarrierMoveShipmentLpns(
            LabelWorkflowService.PreparedJob shipmentJob,
            Map<String, LinkedHashSet<String>> selectedLpnsByShipment
    ) {
        LinkedHashSet<String> selectedIds = selectedLpnsByShipment.get(shipmentJob.getShipmentId());
        if (selectedIds == null || selectedIds.isEmpty()) {
            return List.of();
        }
        List<Lpn> selectedLpns = new ArrayList<>(selectedIds.size());
        for (Lpn lpn : shipmentJob.getLpnsForLabels()) {
            if (lpn != null && selectedIds.contains(lpn.getLpnId())) {
                selectedLpns.add(lpn);
            }
        }
        return selectedLpns;
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

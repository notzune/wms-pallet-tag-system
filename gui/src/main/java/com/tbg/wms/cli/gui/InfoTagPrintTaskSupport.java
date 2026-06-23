package com.tbg.wms.cli.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class InfoTagPrintTaskSupport {
    private static final int MAX_ARTIFACT_SLUG_LENGTH = 64;

    AdvancedPrintWorkflowService.PrintTask buildShipmentInfoTask(LabelWorkflowService.PreparedJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        String safeShipmentId = ArtifactNameSupport.safeSlug(job.getShipmentId(), "shipment", MAX_ARTIFACT_SLUG_LENGTH);
        return new AdvancedPrintWorkflowService.PrintTask(
                AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG,
                "info-shipment-" + safeShipmentId + ".zpl",
                InfoTagZplBuilder.buildShipmentInfoTag(job),
                "INFO-SHIPMENT " + job.getShipmentId()
        );
    }

    AdvancedPrintWorkflowService.PrintTask buildStopInfoTask(
            String carrierMoveId,
            int stopPosition,
            int totalStops,
            Integer stopSequence,
            List<LabelWorkflowService.PreparedJob> shipmentJobs
    ) {
        Objects.requireNonNull(carrierMoveId, "carrierMoveId cannot be null");
        Objects.requireNonNull(shipmentJobs, "shipmentJobs cannot be null");
        List<String> shipmentIds = new ArrayList<>(shipmentJobs.size());
        for (LabelWorkflowService.PreparedJob shipmentJob : shipmentJobs) {
            shipmentIds.add(shipmentJob.getShipmentId());
        }
        return new AdvancedPrintWorkflowService.PrintTask(
                AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG,
                String.format("info-stop-%02d-of-%02d.zpl", stopPosition, totalStops),
                InfoTagZplBuilder.buildStopInfoTag(
                        carrierMoveId,
                        stopPosition,
                        totalStops,
                        stopSequence,
                        shipmentIds,
                        shipmentJobs
                ),
                "INFO-STOP " + stopPosition
        );
    }

    AdvancedPrintWorkflowService.PrintTask buildFinalInfoTask(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        String safeCarrierMoveId = ArtifactNameSupport.safeSlug(job.getCarrierMoveId(), "carrier-move", MAX_ARTIFACT_SLUG_LENGTH);
        return new AdvancedPrintWorkflowService.PrintTask(
                AdvancedPrintWorkflowService.TaskKind.FINAL_INFO_TAG,
                "info-final-cmid-" + safeCarrierMoveId + ".zpl",
                InfoTagZplBuilder.buildFinalInfoTag(job),
                "INFO-FINAL " + job.getCarrierMoveId()
        );
    }
}

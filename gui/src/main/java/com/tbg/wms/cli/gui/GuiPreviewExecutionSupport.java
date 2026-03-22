/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import java.util.Objects;

/**
 * Shared preview validation and dispatch policy for the GUI shell.
 */
final class GuiPreviewExecutionSupport {

    PreviewRequest prepareRequest(String inputId, boolean carrierMoveMode) {
        String normalized = inputId == null ? "" : inputId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(carrierMoveMode ? "Enter a Carrier Move ID." : "Enter a Shipment ID.");
        }
        return new PreviewRequest(normalized, carrierMoveMode);
    }

    PreparedPreview execute(PreviewRequest request, PreviewLoader loader) throws Exception {
        Objects.requireNonNull(request, "request cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        Object prepared = request.carrierMoveMode()
                ? loader.prepareCarrierMoveJob(request.inputId())
                : loader.prepareShipmentJob(request.inputId());
        if (prepared instanceof AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob) {
            return PreparedPreview.forCarrierMove(carrierMoveJob);
        }
        return PreparedPreview.forShipment((LabelWorkflowService.PreparedJob) prepared);
    }

    SuccessOutcome buildSuccessOutcome() {
        return new SuccessOutcome("Preview ready. Verify details, then click Confirm Print.");
    }

    FailureOutcome buildFailureOutcome(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        return new FailureOutcome("Preview failed.", GuiExceptionMessageSupport.rootMessage(throwable));
    }

    record PreviewRequest(String inputId, boolean carrierMoveMode) {
    }

    record PreparedPreview(
            LabelWorkflowService.PreparedJob shipmentJob,
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob
    ) {
        static PreparedPreview forShipment(LabelWorkflowService.PreparedJob shipmentJob) {
            return new PreparedPreview(Objects.requireNonNull(shipmentJob, "shipmentJob cannot be null"), null);
        }

        static PreparedPreview forCarrierMove(AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob) {
            return new PreparedPreview(null, Objects.requireNonNull(carrierMoveJob, "carrierMoveJob cannot be null"));
        }

        boolean isCarrierMove() {
            return carrierMoveJob != null;
        }
    }

    record SuccessOutcome(String statusMessage) {
    }

    record FailureOutcome(String statusMessage, String errorMessage) {
    }

    interface PreviewLoader {
        LabelWorkflowService.PreparedJob prepareShipmentJob(String shipmentId) throws Exception;

        AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMoveJob(String carrierMoveId) throws Exception;
    }
}

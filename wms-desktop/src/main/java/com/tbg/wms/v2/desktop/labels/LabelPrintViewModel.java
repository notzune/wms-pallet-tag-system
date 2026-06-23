package com.tbg.wms.v2.desktop.labels;

import java.nio.file.Path;
import java.util.Objects;

public final class LabelPrintViewModel {
    private final LabelPrintWorkflow workflow;
    private Mode mode = Mode.SHIPMENT;
    private String sourceId = "";
    private LabelPreview preview;
    private LabelPrintResult lastPrintResult;
    private String status = "Enter a shipment or carrier move to preview.";

    public LabelPrintViewModel(LabelPrintWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    public void previewShipment(String shipmentId) {
        sourceId = normalize(shipmentId, "Shipment ID is required.");
        mode = Mode.SHIPMENT;
        preview = workflow.previewShipment(sourceId);
        status = "Ready to print " + preview.taskCount() + " shipment artifacts.";
    }

    public void previewCarrierMove(String carrierMoveId) {
        sourceId = normalize(carrierMoveId, "Carrier move ID is required.");
        mode = Mode.CARRIER_MOVE;
        preview = workflow.previewCarrierMove(sourceId);
        status = "Ready to print " + preview.taskCount() + " carrier move artifacts.";
    }

    public void printToFile(Path outputDir) {
        if (preview == null) {
            throw new IllegalStateException("Preview before printing.");
        }
        lastPrintResult = workflow.printToFile(preview, outputDir);
        status = "Printed " + lastPrintResult.artifactsWritten() + " artifacts to "
                + lastPrintResult.outputDir() + ".";
    }

    public Mode mode() {
        return mode;
    }

    public String sourceId() {
        return sourceId;
    }

    public LabelPreview preview() {
        return preview;
    }

    public boolean canPrint() {
        return preview != null && preview.taskCount() > 0;
    }

    public LabelPrintResult lastPrintResult() {
        return lastPrintResult;
    }

    public String status() {
        return status;
    }

    private static String normalize(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public enum Mode {
        SHIPMENT,
        CARRIER_MOVE
    }
}

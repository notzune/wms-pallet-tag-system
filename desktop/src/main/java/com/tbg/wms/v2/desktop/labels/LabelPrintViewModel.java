package com.tbg.wms.v2.desktop.labels;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Coordinates shipment and carrier-move label preview and print state for the desktop shell.
 */
public final class LabelPrintViewModel {
    private final LabelPrintWorkflow workflow;
    private Mode mode = Mode.SHIPMENT;
    private String sourceId = "";
    private LabelPreview preview;
    private LabelPrintResult lastPrintResult;
    private String status = "Enter a shipment or carrier move to preview.";

    /**
     * Creates a label print view model.
     *
     * @param workflow label print workflow adapter
     */
    public LabelPrintViewModel(LabelPrintWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    /**
     * Previews labels for a shipment.
     *
     * @param shipmentId shipment identifier to preview
     */
    public void previewShipment(String shipmentId) {
        sourceId = normalize(shipmentId, "Shipment ID is required.");
        mode = Mode.SHIPMENT;
        preview = workflow.previewShipment(sourceId);
        status = "Ready to print " + preview.taskCount() + " shipment artifacts.";
    }

    /**
     * Previews labels for a carrier move.
     *
     * @param carrierMoveId carrier-move identifier to preview
     */
    public void previewCarrierMove(String carrierMoveId) {
        sourceId = normalize(carrierMoveId, "Carrier move ID is required.");
        mode = Mode.CARRIER_MOVE;
        preview = workflow.previewCarrierMove(sourceId);
        status = "Ready to print " + preview.taskCount() + " carrier move artifacts.";
    }

    /**
     * Writes the current preview to files.
     *
     * @param outputDir directory where artifacts should be written
     */
    public void printToFile(Path outputDir) {
        if (preview == null) {
            throw new IllegalStateException("Preview before printing.");
        }
        lastPrintResult = workflow.printToFile(preview, outputDir);
        status = "Printed " + lastPrintResult.artifactsWritten() + " artifacts to "
                + lastPrintResult.outputDir() + ".";
    }

    /**
     * Returns the current label workflow mode.
     *
     * @return selected mode
     */
    public Mode mode() {
        return mode;
    }

    /**
     * Returns the currently previewed source identifier.
     *
     * @return shipment or carrier-move identifier
     */
    public String sourceId() {
        return sourceId;
    }

    /**
     * Previews the requested workflow data.
     *
     * @return latest label preview, or {@code null} before preview generation
     */
    public LabelPreview preview() {
        return preview;
    }

    /**
     * Indicates whether the current preview has printable tasks.
     *
     * @return {@code true} when printing is currently available
     */
    public boolean canPrint() {
        return preview != null && preview.taskCount() > 0;
    }

    /**
     * Returns the most recent print-to-file result.
     *
     * @return last print result, or {@code null} before printing
     */
    public LabelPrintResult lastPrintResult() {
        return lastPrintResult;
    }

    /**
     * Returns the current label workflow status.
     *
     * @return current status message
     */
    public String status() {
        return status;
    }

    private static String normalize(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * Enumerates supported mode values for WMS 2.0 workflows.
     */
    public enum Mode {
        SHIPMENT,
        CARRIER_MOVE
    }
}

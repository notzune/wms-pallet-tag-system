package com.tbg.wms.v2.desktop.barcode;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Coordinates barcode label preview and dry-run generation state for the desktop shell.
 */
public final class BarcodeViewModel {
    private final Workflow workflow;
    private Path lastArtifact;
    private String status = "Enter barcode data.";

    /**
     * Creates a Barcode View Model instance.
     *
     * @param workflow the workflow.
     */
    public BarcodeViewModel(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    /**
     * Generates a dry-run barcode label artifact and updates the status message.
     *
     * @param data barcode payload to encode
     * @param outputDir directory where the artifact should be written
     */
    public void generateDryRun(String data, Path outputDir) {
        lastArtifact = workflow.generateDryRun(data, outputDir);
        status = "Wrote barcode label " + lastArtifact.getFileName() + ".";
    }

    /**
     * Returns the most recent dry-run artifact path.
     *
     * @return last generated artifact, or {@code null} before generation
     */
    public Path lastArtifact() {
        return lastArtifact;
    }

    /**
     * Returns the user-facing barcode workflow status.
     *
     * @return current status message
     */
    public String status() {
        return status;
    }

    /**
     * Defines the contract for workflow behavior in WMS 2.0 workflows.
     */
    public interface Workflow {
        /**
         * Generates a barcode dry-run artifact.
         *
         * @param data barcode payload to encode
         * @param outputDir directory where the artifact should be written
         * @return generated artifact path
         */
        Path generateDryRun(String data, Path outputDir);
    }
}

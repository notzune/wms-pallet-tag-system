package com.tbg.wms.v2.desktop.rail;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Manages rail label preview and template-generation state for the desktop shell.
 */
public final class RailLabelsViewModel {
    private final Workflow workflow;
    private RailPreview preview;
    private String status = "Enter a train to preview.";

    /**
     * Creates a Rail Labels View Model instance.
     *
     * @param workflow the workflow.
     */
    public RailLabelsViewModel(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    /**
     * Previews train.
     *
     * @param trainId the train ID.
     */
    public void previewTrain(String trainId) {
        preview = workflow.previewTrain(trainId);
        status = "Prepared " + preview.railcarCount() + " railcars.";
    }

    /**
     * Generates a rail label alignment template and updates status text.
     *
     * @param outputDir directory where the template should be written
     */
    public void generateTemplate(Path outputDir) {
        workflow.generateTemplate(outputDir);
        status = "Wrote rail alignment template.";
    }

    /**
     * Returns the current rail preview.
     *
     * @return latest preview, or {@code null} before preview generation
     */
    public RailPreview preview() {
        return preview;
    }

    /**
     * Returns the current rail workflow status.
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
         * Previews train.
         *
         * @param trainId the train id.
         * @return the preview.
         */
        RailPreview previewTrain(String trainId);

        /**
         * Generates a rail label alignment template.
         *
         * @param outputDir directory where the template should be written
         * @return generated template path
         */
        Path generateTemplate(Path outputDir);
    }

    /**
     * Carries rail preview data across WMS 2.0 module boundaries.
     *
     * @param trainId the train id.
     * @param railcarCount the railcar count.
     * @param warnings the warnings.
     */
    public record RailPreview(String trainId, int railcarCount, List<String> warnings) {
        public RailPreview {
            trainId = trainId == null ? "" : trainId.trim();
            warnings = List.copyOf(warnings == null ? List.of() : warnings);
        }
    }
}

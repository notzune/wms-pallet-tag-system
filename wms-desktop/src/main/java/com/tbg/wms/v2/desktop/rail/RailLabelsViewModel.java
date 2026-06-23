package com.tbg.wms.v2.desktop.rail;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class RailLabelsViewModel {
    private final Workflow workflow;
    private RailPreview preview;
    private String status = "Enter a train to preview.";

    public RailLabelsViewModel(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    public void previewTrain(String trainId) {
        preview = workflow.previewTrain(trainId);
        status = "Prepared " + preview.railcarCount() + " railcars.";
    }

    public void generateTemplate(Path outputDir) {
        workflow.generateTemplate(outputDir);
        status = "Wrote rail alignment template.";
    }

    public RailPreview preview() {
        return preview;
    }

    public String status() {
        return status;
    }

    public interface Workflow {
        RailPreview previewTrain(String trainId);

        Path generateTemplate(Path outputDir);
    }

    public record RailPreview(String trainId, int railcarCount, List<String> warnings) {
        public RailPreview {
            trainId = trainId == null ? "" : trainId.trim();
            warnings = List.copyOf(warnings == null ? List.of() : warnings);
        }
    }
}

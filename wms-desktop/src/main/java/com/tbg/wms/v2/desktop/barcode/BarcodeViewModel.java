package com.tbg.wms.v2.desktop.barcode;

import java.nio.file.Path;
import java.util.Objects;

public final class BarcodeViewModel {
    private final Workflow workflow;
    private Path lastArtifact;
    private String status = "Enter barcode data.";

    public BarcodeViewModel(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    public void generateDryRun(String data, Path outputDir) {
        lastArtifact = workflow.generateDryRun(data, outputDir);
        status = "Wrote barcode label " + lastArtifact.getFileName() + ".";
    }

    public Path lastArtifact() {
        return lastArtifact;
    }

    public String status() {
        return status;
    }

    public interface Workflow {
        Path generateDryRun(String data, Path outputDir);
    }
}

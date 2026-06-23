package com.tbg.wms.v2.desktop.queue;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class QueuePrintViewModel {
    private final Workflow workflow;
    private QueuePreview preview;
    private String status = "Enter queue items to preview.";

    public QueuePrintViewModel(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    public void preview(String input) {
        preview = workflow.preview(input);
        status = "Prepared " + preview.items().size() + " queue items.";
    }

    public void printToFile(Path outputDir) {
        if (preview == null) {
            throw new IllegalStateException("Preview queue before printing.");
        }
        QueuePrintResult result = workflow.printToFile(preview, outputDir);
        status = "Printed queue: " + result.artifactsWritten() + " artifacts.";
    }

    public QueuePreview preview() {
        return preview;
    }

    public String status() {
        return status;
    }

    public interface Workflow {
        QueuePreview preview(String input);

        QueuePrintResult printToFile(QueuePreview preview, Path outputDir);
    }

    public record QueuePreview(List<QueuePreviewItem> items) {
        public QueuePreview {
            items = List.copyOf(items == null ? List.of() : items);
        }
    }

    public record QueuePreviewItem(String type, String sourceId) {
    }

    public record QueuePrintResult(int artifactsWritten, Path outputDir) {
    }
}

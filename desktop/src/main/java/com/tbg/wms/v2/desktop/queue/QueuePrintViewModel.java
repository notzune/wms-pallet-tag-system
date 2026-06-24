package com.tbg.wms.v2.desktop.queue;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates queue preview and print-to-file state for the desktop shell.
 */
public final class QueuePrintViewModel {
    private final Workflow workflow;
    private QueuePreview preview;
    private String status = "Enter queue items to preview.";

    /**
     * Creates a queue print view model.
     *
     * @param workflow queue workflow adapter
     */
    public QueuePrintViewModel(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    /**
     * Builds a queue preview from operator input.
     *
     * @param input raw queue input text
     */
    public void preview(String input) {
        preview = workflow.preview(input);
        status = "Prepared " + preview.items().size() + " queue items.";
    }

    /**
     * Writes the current queue preview to files.
     *
     * @param outputDir directory where artifacts should be written
     */
    public void printToFile(Path outputDir) {
        if (preview == null) {
            throw new IllegalStateException("Preview queue before printing.");
        }
        QueuePrintResult result = workflow.printToFile(preview, outputDir);
        status = "Printed queue: " + result.artifactsWritten() + " artifacts.";
    }

    /**
     * Returns the current queue preview.
     *
     * @return latest preview, or {@code null} before preview generation
     */
    public QueuePreview preview() {
        return preview;
    }

    /**
     * Returns the current queue workflow status.
     *
     * @return current status message
     */
    public String status() {
        return status;
    }

    /**
     * Defines the queue workflow operations used by the view model.
     */
    public interface Workflow {
        /**
         * Builds a queue preview from raw input.
         *
         * @param input raw queue input text
         * @return queue preview
         */
        QueuePreview preview(String input);

        /**
         * Writes a queue preview to files.
         *
         * @param preview queue preview to print
         * @param outputDir directory where artifacts should be written
         * @return print-to-file result
         */
        QueuePrintResult printToFile(QueuePreview preview, Path outputDir);
    }

    /**
     * Carries queue preview rows for display.
     *
     * @param items preview rows in execution order
     */
    public record QueuePreview(List<QueuePreviewItem> items) {
        public QueuePreview {
            items = List.copyOf(items == null ? List.of() : items);
        }
    }

    /**
     * Carries one queue preview row.
     *
     * @param type queue item type
     * @param sourceId shipment or carrier-move identifier
     */
    public record QueuePreviewItem(String type, String sourceId) {
    }

    /**
     * Carries queue print-to-file totals.
     *
     * @param artifactsWritten number of artifacts written
     * @param outputDir directory that received the artifacts
     */
    public record QueuePrintResult(int artifactsWritten, Path outputDir) {
    }
}

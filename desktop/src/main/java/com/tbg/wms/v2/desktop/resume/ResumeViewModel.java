package com.tbg.wms.v2.desktop.resume;

import java.util.List;
import java.util.Objects;

/**
 * Manages resumable print job state for the desktop shell.
 */
public final class ResumeViewModel {
    private final Workflow workflow;
    private List<ResumeCandidateItem> candidates = List.of();
    private String status = "Scan for incomplete jobs.";

    /**
     * Creates a Resume View Model instance.
     *
     * @param workflow the workflow.
     */
    public ResumeViewModel(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    /**
     * Refreshes the list of resumable jobs and updates status text.
     */
    public void refresh() {
        candidates = workflow.candidates();
        status = "Found " + candidates.size() + " resumable jobs.";
    }

    /**
     * Resumes the selected checkpoint and records the resume status.
     *
     * @param checkpointId checkpoint identifier selected by the user
     */
    public void resume(String checkpointId) {
        ResumeResult result = workflow.resume(checkpointId);
        status = "Resumed " + result.checkpointId() + " at task " + result.nextTaskIndex() + ".";
    }

    /**
     * Returns the currently loaded resume candidates.
     *
     * @return immutable or workflow-provided candidate list
     */
    public List<ResumeCandidateItem> candidates() {
        return candidates;
    }

    /**
     * Returns the current resume workflow status.
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
         * Lists resume candidates.
         *
         * @return the matching values.
         */
        List<ResumeCandidateItem> candidates();

        /**
         * Resumes a checkpoint.
         *
         * @param checkpointId checkpoint identifier to resume
         * @return resume result describing the next task
         */
        ResumeResult resume(String checkpointId);
    }

    /**
     * Carries resume candidate item data across WMS 2.0 module boundaries.
     *
     * @param checkpointId the checkpoint id.
     * @param sourceId the source id.
     * @param nextTaskIndex the next task index.
     * @param totalTasks the total tasks.
     */
    public record ResumeCandidateItem(String checkpointId, String sourceId, int nextTaskIndex, int totalTasks) {
    }

    /**
     * Carries resume result data across WMS 2.0 module boundaries.
     *
     * @param checkpointId the checkpoint id.
     * @param nextTaskIndex the next task index.
     */
    public record ResumeResult(String checkpointId, int nextTaskIndex) {
    }
}

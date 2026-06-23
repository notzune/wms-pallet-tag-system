package com.tbg.wms.v2.desktop.resume;

import java.util.List;
import java.util.Objects;

public final class ResumeViewModel {
    private final Workflow workflow;
    private List<ResumeCandidateItem> candidates = List.of();
    private String status = "Scan for incomplete jobs.";

    public ResumeViewModel(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow");
    }

    public void refresh() {
        candidates = workflow.candidates();
        status = "Found " + candidates.size() + " resumable jobs.";
    }

    public void resume(String checkpointId) {
        ResumeResult result = workflow.resume(checkpointId);
        status = "Resumed " + result.checkpointId() + " at task " + result.nextTaskIndex() + ".";
    }

    public List<ResumeCandidateItem> candidates() {
        return candidates;
    }

    public String status() {
        return status;
    }

    public interface Workflow {
        List<ResumeCandidateItem> candidates();

        ResumeResult resume(String checkpointId);
    }

    public record ResumeCandidateItem(String checkpointId, String sourceId, int nextTaskIndex, int totalTasks) {
    }

    public record ResumeResult(String checkpointId, int nextTaskIndex) {
    }
}

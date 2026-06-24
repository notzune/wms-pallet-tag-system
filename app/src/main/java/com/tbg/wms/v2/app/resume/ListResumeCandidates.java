package com.tbg.wms.v2.app.resume;

import com.tbg.wms.v2.app.ports.CheckpointStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Provides list resume candidates behavior for WMS 2.0 workflows.
 */
public final class ListResumeCandidates {
    private final CheckpointStore checkpointStore;

    /**
     * Creates a resume-candidate listing use case.
     *
     * @param checkpointStore the checkpoint store.
     */
    public ListResumeCandidates(CheckpointStore checkpointStore) {
        this.checkpointStore = Objects.requireNonNull(checkpointStore, "checkpointStore");
    }

    /**
     * Lists .
     *
     * @return the resume candidates.
     */
    public List<ResumeCandidate> list() {
        List<CheckpointStore.PrintCheckpoint> checkpoints = new ArrayList<>(checkpointStore.findIncomplete());
        Collections.reverse(checkpoints);
        return checkpoints.stream()
                .map(checkpoint -> new ResumeCandidate(
                        checkpoint.id(),
                        checkpoint.sourceId(),
                        checkpoint.nextTaskIndex(),
                        checkpoint.totalTasks(),
                        checkpoint.lastError()
                ))
                .toList();
    }
}

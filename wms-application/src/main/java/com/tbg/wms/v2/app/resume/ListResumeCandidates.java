package com.tbg.wms.v2.app.resume;

import com.tbg.wms.v2.app.ports.CheckpointStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Lists incomplete print checkpoints for operator resume prompts.
 */
public final class ListResumeCandidates {
    private final CheckpointStore checkpointStore;

    public ListResumeCandidates(CheckpointStore checkpointStore) {
        this.checkpointStore = Objects.requireNonNull(checkpointStore, "checkpointStore");
    }

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

package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.ports.ArtifactStore;
import com.tbg.wms.v2.domain.print.PrintTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides output dir artifact store behavior for WMS 2.0 workflows.
 */
final class OutputDirArtifactStore implements ArtifactStore {
    private final Path root;

    OutputDirArtifactStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * Writes the supplied artifact data to storage.
     *
     * @param task the task.
     * @return written artifact path
     */
    @Override
    public Path write(PrintTask task) {
        Path target = root.resolve(task.artifactName()).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Artifact path escapes output root: " + task.artifactName());
        }
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, task.subject());
            return target;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write artifact: " + target, ex);
        }
    }
}

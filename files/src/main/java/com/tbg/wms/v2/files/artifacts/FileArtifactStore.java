package com.tbg.wms.v2.files.artifacts;

import com.tbg.wms.v2.app.ports.ArtifactStore;
import com.tbg.wms.v2.domain.print.PrintTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides file artifact store behavior for WMS 2.0 workflows.
 */
public final class FileArtifactStore implements ArtifactStore {
    private final Path root;

    /**
     * Creates a file-backed artifact store.
     *
     * @param root the root.
     */
    public FileArtifactStore(Path root) {
        this.root = Objects.requireNonNull(root, "root cannot be null").toAbsolutePath().normalize();
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

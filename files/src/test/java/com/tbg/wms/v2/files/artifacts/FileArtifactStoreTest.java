package com.tbg.wms.v2.files.artifacts;

import com.tbg.wms.v2.domain.print.PrintTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileArtifactStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void write_storesSubjectAtSafeArtifactPath() throws Exception {
        FileArtifactStore store = new FileArtifactStore(tempDir);
        PrintTask task = new PrintTask(PrintTask.Kind.PALLET_LABEL, "labels/first.zpl", "^XA^XZ");

        Path path = store.write(task);

        assertEquals(tempDir.resolve("labels").resolve("first.zpl"), path);
        assertEquals("^XA^XZ", Files.readString(path));
    }

    @Test
    void write_rejectsArtifactTraversalOutsideRoot() {
        FileArtifactStore store = new FileArtifactStore(tempDir);
        PrintTask task = new PrintTask(PrintTask.Kind.PALLET_LABEL, "../escape.zpl", "^XA^XZ");

        assertThrows(IllegalArgumentException.class, () -> store.write(task));
    }
}

package com.tbg.wms.v2.smoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmokeManifestWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writerEmitsPowerShellReadableManifest() throws Exception {
        SmokeManifestWriter writer = new SmokeManifestWriter();

        writer.write(SmokeManifest.defaultManifest(), tempDir);

        String json = Files.readString(tempDir.resolve("smoke-manifest.json"));
        assertTrue(json.contains("\"version\":2"));
        assertTrue(json.contains("\"name\":\"config\""));
        assertTrue(json.contains("\"name\":\"packaged-config-precedence\""));
    }
}

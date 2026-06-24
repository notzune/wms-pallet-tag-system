package com.tbg.wms.v2.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides smoke manifest writer behavior for WMS 2.0 workflows.
 */
public final class SmokeManifestWriter {
    /**
     * Writes the supplied artifact data to storage.
     *
     * @param manifest the manifest.
     * @param outputDirectory the output directory.
     * @throws IOException if the operation cannot complete.
     */
    public void write(SmokeManifest manifest, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("smoke-manifest.json"), manifest.toJson());
    }

    /**
     * Runs the command-line entry point.
     *
     * @param args the args.
     * @throws IOException if the operation cannot complete.
     */
    public static void main(String[] args) throws IOException {
        Path outputDirectory = args.length == 0
                ? Path.of("smoke", "target", "generated-smoke")
                : Path.of(args[0]);
        new SmokeManifestWriter().write(SmokeManifest.defaultManifest(), outputDirectory);
    }
}

package com.tbg.wms.v2.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SmokeManifestWriter {
    public void write(SmokeManifest manifest, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("smoke-manifest.json"), manifest.toJson());
    }

    public static void main(String[] args) throws IOException {
        Path outputDirectory = args.length == 0
                ? Path.of("wms-smoke", "target", "generated-smoke")
                : Path.of(args[0]);
        new SmokeManifestWriter().write(SmokeManifest.defaultManifest(), outputDirectory);
    }
}

package com.tbg.wms.v2.files.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileRuntimeConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void load_appliesEnvironmentOverExplicitFileOverPerUserOverAppRootOverDotEnvOverDefaults() throws Exception {
        Path explicit = write(tempDir.resolve("explicit.env"), "A=explicit\nB=explicit\n");
        Path localAppData = Files.createDirectories(tempDir.resolve("local-app-data"));
        Path perUserDir = Files.createDirectories(localAppData.resolve("Tropicana").resolve("WMS-Pallet-Tag-System"));
        write(perUserDir.resolve("wms-tags.env"), "A=per-user\nB=per-user\nC=per-user\n");
        Path appRoot = Files.createDirectories(tempDir.resolve("app-root"));
        write(appRoot.resolve("wms-tags.env"), "A=app-root\nB=app-root\nC=app-root\nD=app-root\n");
        Path workingDir = Files.createDirectories(tempDir.resolve("working"));
        write(workingDir.resolve(".env"), "A=dotenv\nB=dotenv\nC=dotenv\nD=dotenv\nE=dotenv\n");

        FileRuntimeConfig config = FileRuntimeConfig.load(new FileRuntimeConfig.Request(
                Map.of(
                        "WMS_CONFIG_FILE", explicit.toString(),
                        "LOCALAPPDATA", localAppData.toString(),
                        "A", "env"
                ),
                workingDir,
                appRoot,
                Map.of(
                        "A", "default",
                        "B", "default",
                        "C", "default",
                        "D", "default",
                        "E", "default",
                        "F", "default"
                )
        ));

        assertEquals("env", config.get("A"));
        assertEquals("explicit", config.get("B"));
        assertEquals("per-user", config.get("C"));
        assertEquals("app-root", config.get("D"));
        assertEquals("dotenv", config.get("E"));
        assertEquals("default", config.get("F"));
        assertEquals(explicit.toAbsolutePath().normalize(), config.loadedConfigFiles().get(0));
    }

    @Test
    void parser_supportsExportQuotesAndInlineComments() throws Exception {
        Path file = write(tempDir.resolve("quoted.env"), """
                # ignored
                export DB_USER="RPTADM"
                DB_PASS='secret#not-comment'
                DB_HOST=host # comment
                BROKEN
                """);

        FileRuntimeConfig config = FileRuntimeConfig.load(new FileRuntimeConfig.Request(
                Map.of("WMS_CONFIG_FILE", file.toString()),
                tempDir,
                tempDir,
                Map.of()
        ));

        assertEquals("RPTADM", config.get("DB_USER"));
        assertEquals("secret#not-comment", config.get("DB_PASS"));
        assertEquals("host", config.get("DB_HOST"));
    }

    private static Path write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}

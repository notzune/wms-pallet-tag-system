/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.cli.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Filesystem persistence for GUI print-job checkpoints.
 */
final class JobCheckpointStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Path checkpointDirectory;

    JobCheckpointStore() {
        this(Paths.get("out", "gui-jobs"));
    }

    JobCheckpointStore(Path checkpointDirectory) {
        this.checkpointDirectory = checkpointDirectory.toAbsolutePath();
    }

    AdvancedPrintWorkflowService.JobCheckpoint read(String id) throws Exception {
        Path file = checkpointDirectory.resolve(id + ".json");
        if (!Files.exists(file)) {
            return null;
        }
        return MAPPER.readValue(file.toFile(), AdvancedPrintWorkflowService.JobCheckpoint.class);
    }

    void write(AdvancedPrintWorkflowService.JobCheckpoint checkpoint) throws Exception {
        Files.createDirectories(checkpointDirectory);
        Path file = checkpointDirectory.resolve(checkpoint.id + ".json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), checkpoint);
    }

    List<Path> listCheckpointFiles(int maxFiles) throws Exception {
        if (!Files.exists(checkpointDirectory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(checkpointDirectory)) {
            Iterator<Path> iterator = stream
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .limit(maxFiles)
                    .iterator();
            List<Path> files = new java.util.ArrayList<>();
            while (iterator.hasNext()) {
                files.add(iterator.next());
            }
            return files;
        }
    }
}

package com.tbg.wms.v2.files.checkpoints;

import com.tbg.wms.v2.app.ports.CheckpointStore;
import com.tbg.wms.v2.domain.print.PrintTask;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class FileCheckpointStore implements CheckpointStore {
    private final Path root;

    public FileCheckpointStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void save(PrintCheckpoint checkpoint) {
        Path target = checkpointPath(checkpoint.id());
        try {
            Files.createDirectories(root);
            Files.writeString(target, encode(checkpoint), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save checkpoint: " + checkpoint.id(), ex);
        }
    }

    @Override
    public Optional<PrintCheckpoint> findById(String checkpointId) {
        Path path = checkpointPath(checkpointId);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(read(path));
    }

    @Override
    public List<PrintCheckpoint> findIncomplete() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(root)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".checkpoint"))
                    .sorted(Comparator.comparingLong(this::lastModified).reversed())
                    .map(this::read)
                    .filter(checkpoint -> checkpoint.nextTaskIndex() < checkpoint.totalTasks())
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list checkpoints: " + root, ex);
        }
    }

    private Path checkpointPath(String checkpointId) {
        String safe = checkpointId.replaceAll("[^A-Za-z0-9._-]+", "-");
        Path target = root.resolve(safe + ".checkpoint").normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Checkpoint path escapes root: " + checkpointId);
        }
        return target;
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to inspect checkpoint: " + path, ex);
        }
    }

    private PrintCheckpoint read(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return decode(lines);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read checkpoint: " + path, ex);
        }
    }

    private static String encode(PrintCheckpoint checkpoint) {
        StringBuilder builder = new StringBuilder();
        append(builder, "id", checkpoint.id());
        append(builder, "sourceId", checkpoint.sourceId());
        append(builder, "nextTaskIndex", Integer.toString(checkpoint.nextTaskIndex()));
        append(builder, "totalTasks", Integer.toString(checkpoint.totalTasks()));
        append(builder, "lastError", checkpoint.lastError() == null ? "" : checkpoint.lastError());
        append(builder, "printToFile", Boolean.toString(checkpoint.printToFile()));
        append(builder, "printerId", checkpoint.printerId() == null ? "" : checkpoint.printerId());
        for (PrintTask task : checkpoint.tasks()) {
            append(builder, "task", task.kind().name() + "|" + escape(task.artifactName()) + "|" + escape(task.subject()));
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, String key, String value) {
        builder.append(key).append('=').append(escape(value)).append('\n');
    }

    private static PrintCheckpoint decode(List<String> lines) {
        String id = "";
        String sourceId = "";
        int nextTaskIndex = 0;
        int totalTasks = 0;
        String lastError = null;
        boolean printToFile = false;
        String printerId = null;
        List<PrintTask> tasks = new ArrayList<>();

        for (String line : lines) {
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator);
            String value = unescape(line.substring(separator + 1));
            switch (key) {
                case "id" -> id = value;
                case "sourceId" -> sourceId = value;
                case "nextTaskIndex" -> nextTaskIndex = Integer.parseInt(value);
                case "totalTasks" -> totalTasks = Integer.parseInt(value);
                case "lastError" -> lastError = value.isBlank() ? null : value;
                case "printToFile" -> printToFile = Boolean.parseBoolean(value);
                case "printerId" -> printerId = value.isBlank() ? null : value;
                case "task" -> tasks.add(decodeTask(value));
                default -> {
                }
            }
        }
        return new PrintCheckpoint(id, sourceId, nextTaskIndex, totalTasks, lastError, printToFile, printerId, tasks);
    }

    private static PrintTask decodeTask(String value) {
        String[] parts = splitTask(value);
        return new PrintTask(PrintTask.Kind.valueOf(parts[0]), parts[1], parts[2]);
    }

    private static String[] splitTask(String value) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '|') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        parts.add(current.toString());
        return parts.toArray(String[]::new);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("|", "\\p");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!escaped) {
                if (ch == '\\') {
                    escaped = true;
                } else {
                    result.append(ch);
                }
                continue;
            }
            if (ch == 'n') {
                result.append('\n');
            } else if (ch == 'p') {
                result.append('|');
            } else {
                result.append(ch);
            }
            escaped = false;
        }
        if (escaped) {
            result.append('\\');
        }
        return result.toString();
    }
}

package com.tbg.wms.v2.files.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads file-backed runtime configuration values for WMS 2.0 adapters.
 *
 * @param values the values.
 * @param loadedConfigFiles the loaded Config Files.
 */
public record FileRuntimeConfig(Map<String, String> values, List<Path> loadedConfigFiles) {
    private static final String DEFAULT_FILE_NAME = "wms-tags.env";

    public FileRuntimeConfig {
        values = Map.copyOf(values == null ? Map.of() : values);
        loadedConfigFiles = List.copyOf(loadedConfigFiles == null ? List.of() : loadedConfigFiles);
    }

    /**
     * Returns the .
     *
     * @param key the key.
     * @return configured value, when present
     */
    public String get(String key) {
        return values.get(key);
    }

    /**
     * Loads the current state from the backing store.
     *
     * @param request the request.
     * @return loaded runtime configuration
     */
    public static FileRuntimeConfig load(Request request) {
        Objects.requireNonNull(request, "request cannot be null");
        LinkedHashMap<String, String> values = new LinkedHashMap<>(request.defaults());
        List<Path> loaded = new ArrayList<>();

        for (Path candidate : discoveryOrder(request)) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                values.putAll(readEnvFile(candidate));
                loaded.add(0, candidate.toAbsolutePath().normalize());
            }
        }
        values.putAll(environmentOverrides(request.environment()));
        return new FileRuntimeConfig(values, loaded);
    }

    private static List<Path> discoveryOrder(Request request) {
        List<Path> paths = new ArrayList<>();
        Path dotEnv = request.workingDirectory().resolve(".env");
        Path appRootConfig = request.appRoot().resolve(DEFAULT_FILE_NAME);
        Path perUser = perUserConfig(request.environment());
        Path explicit = explicitConfig(request.environment());

        paths.add(dotEnv);
        paths.add(appRootConfig);
        paths.add(perUser);
        paths.add(explicit);
        return paths;
    }

    private static Path explicitConfig(Map<String, String> environment) {
        String value = environment.get("WMS_CONFIG_FILE");
        return value == null || value.isBlank() ? null : Path.of(value.trim());
    }

    private static Path perUserConfig(Map<String, String> environment) {
        String localAppData = environment.get("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            return null;
        }
        return Path.of(localAppData.trim())
                .resolve("Tropicana")
                .resolve("WMS-Pallet-Tag-System")
                .resolve(DEFAULT_FILE_NAME);
    }

    private static Map<String, String> environmentOverrides(Map<String, String> environment) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            if (!"WMS_CONFIG_FILE".equals(entry.getKey()) && !"LOCALAPPDATA".equals(entry.getKey())) {
                values.put(entry.getKey(), entry.getValue());
            }
        }
        return values;
    }

    private static Map<String, String> readEnvFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            LinkedHashMap<String, String> values = new LinkedHashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(values, line);
            }
            return values;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read config file: " + path, ex);
        }
    }

    private static void parseLine(Map<String, String> values, String rawLine) {
        if (rawLine == null) {
            return;
        }
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }
        int separator = line.indexOf('=');
        if (separator <= 0) {
            return;
        }
        String key = line.substring(0, separator).trim();
        String value = stripQuotes(stripInlineComment(line.substring(separator + 1)).trim());
        if (!key.isBlank()) {
            values.put(key, value);
        }
    }

    private static String stripInlineComment(String value) {
        boolean singleQuote = false;
        boolean doubleQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
            } else if (ch == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
            } else if (ch == '#' && !singleQuote && !doubleQuote) {
                return value.substring(0, i);
            }
        }
        return value;
    }

    private static String stripQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Carries request data across WMS 2.0 module boundaries.
     *
     * @param environment the environment.
     * @param workingDirectory the working directory.
     * @param appRoot the app root.
     * @param defaults the defaults.
     */
    public record Request(
            Map<String, String> environment,
            Path workingDirectory,
            Path appRoot,
            Map<String, String> defaults
    ) {
        public Request {
            environment = Map.copyOf(environment == null ? Map.of() : environment);
            workingDirectory = workingDirectory == null ? Path.of(".") : workingDirectory;
            appRoot = appRoot == null ? workingDirectory : appRoot;
            defaults = Map.copyOf(defaults == null ? Map.of() : defaults);
        }
    }
}

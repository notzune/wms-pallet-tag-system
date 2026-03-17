package com.tbg.wms.core.update;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Objects;

/**
 * Minimal version normalization/comparison for release tags like {@code v1.7.1}.
 */
public final class VersionSupport {
    private VersionSupport() {
        // Utility class.
    }

    public static String normalize(String version) {
        String raw = Objects.requireNonNullElse(version, "").trim();
        if (raw.isEmpty()) {
            return "";
        }
        if (raw.startsWith("v") || raw.startsWith("V")) {
            raw = raw.substring(1).trim();
        }
        return raw;
    }

    public static String resolvePackageVersion(Class<?> anchorType) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Package pkg = anchorType.getPackage();
        String implementationVersion = pkg == null ? null : pkg.getImplementationVersion();
        return implementationVersion == null ? "" : implementationVersion.trim();
    }

    public static String readFirstNonBlankResourceLine(Class<?> anchorType, String... resourcePaths) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Objects.requireNonNull(resourcePaths, "resourcePaths cannot be null");
        for (String resourcePath : resourcePaths) {
            if (resourcePath == null || resourcePath.isBlank()) {
                continue;
            }
            try (InputStream in = anchorType.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        return line.trim();
                    }
                }
            } catch (Exception ignored) {
                // Fall through to the next candidate.
            }
        }
        return "";
    }

    public static String readFirstNonBlankProperty(Class<?> anchorType, String propertyName, String... resourcePaths) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Objects.requireNonNull(propertyName, "propertyName cannot be null");
        Objects.requireNonNull(resourcePaths, "resourcePaths cannot be null");
        for (String resourcePath : resourcePaths) {
            if (resourcePath == null || resourcePath.isBlank()) {
                continue;
            }
            try (InputStream in = anchorType.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    continue;
                }
                Properties properties = new Properties();
                properties.load(in);
                String value = properties.getProperty(propertyName, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            } catch (Exception ignored) {
                // Fall through to the next candidate.
            }
        }
        return "";
    }

    public static int compare(String leftVersion, String rightVersion) {
        String left = normalize(leftVersion);
        String right = normalize(rightVersion);
        if (left.equals(right)) {
            return 0;
        }

        String[] leftSegments = left.split("[.-]");
        String[] rightSegments = right.split("[.-]");
        int maxSegments = Math.max(leftSegments.length, rightSegments.length);
        for (int index = 0; index < maxSegments; index++) {
            String leftSegment = index < leftSegments.length ? leftSegments[index] : "0";
            String rightSegment = index < rightSegments.length ? rightSegments[index] : "0";
            int segmentCompare = compareSegment(leftSegment, rightSegment);
            if (segmentCompare != 0) {
                return segmentCompare;
            }
        }
        return 0;
    }

    private static int compareSegment(String leftSegment, String rightSegment) {
        boolean leftNumeric = leftSegment.chars().allMatch(Character::isDigit);
        boolean rightNumeric = rightSegment.chars().allMatch(Character::isDigit);
        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(leftSegment), Integer.parseInt(rightSegment));
        }
        if (leftNumeric) {
            return 1;
        }
        if (rightNumeric) {
            return -1;
        }
        return leftSegment.compareToIgnoreCase(rightSegment);
    }
}

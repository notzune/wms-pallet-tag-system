package com.tbg.wms.core.update;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Objects;

/**
 * Minimal version normalization/comparison for release identifiers such as {@code v1.7.1}
 * and {@code v1.7.1-rc1}.
 *
 * <p>The helper intentionally stays small because it is used by runtime update checks and
 * release-facing metadata paths that only need predictable tag normalization and ordering, not
 * full semantic-version feature coverage.</p>
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

    public static String resolveRuntimeVersion(
            Class<?> anchorType,
            String systemPropertyName,
            String propertyName,
            String... resourcePaths
    ) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        String version = resolvePackageVersion(anchorType);
        if (!version.isBlank()) {
            return version;
        }
        if (systemPropertyName != null && !systemPropertyName.isBlank()) {
            version = System.getProperty(systemPropertyName, "").trim();
            if (!version.isBlank()) {
                return version;
            }
        }
        if (propertyName != null && !propertyName.isBlank()) {
            version = readFirstNonBlankProperty(anchorType, propertyName, resourcePaths);
            if (!version.isBlank()) {
                return version;
            }
        }
        return readFirstNonBlankResourceLine(anchorType, resourcePaths);
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
        ParsedVersion left = ParsedVersion.parse(leftVersion);
        ParsedVersion right = ParsedVersion.parse(rightVersion);
        return left.compareTo(right);
    }

    public static boolean isPrerelease(String version) {
        String normalized = normalize(version);
        return normalized.contains("-");
    }

    public static int compareReleaseLine(String leftVersion, String rightVersion) {
        int[] leftLine = releaseLine(normalize(leftVersion));
        int[] rightLine = releaseLine(normalize(rightVersion));
        int majorCompare = Integer.compare(leftLine[0], rightLine[0]);
        if (majorCompare != 0) {
            return majorCompare;
        }
        return Integer.compare(leftLine[1], rightLine[1]);
    }

    private static int[] releaseLine(String version) {
        if (version == null || version.isBlank()) {
            return new int[]{0, 0};
        }
        String[] segments = version.split("[.-]");
        return new int[]{
                parseLeadingNumericSegment(segments, 0),
                parseLeadingNumericSegment(segments, 1)
        };
    }

    private static int parseLeadingNumericSegment(String[] segments, int index) {
        if (segments == null || index >= segments.length) {
            return 0;
        }
        String segment = segments[index];
        return segment.chars().allMatch(Character::isDigit) ? Integer.parseInt(segment) : 0;
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

    private record ParsedVersion(int major, int minor, int patch, List<QualifierToken> qualifiers)
            implements Comparable<ParsedVersion> {
        private static ParsedVersion parse(String version) {
            String normalized = normalize(version);
            if (normalized.isBlank()) {
                return new ParsedVersion(0, 0, 0, List.of());
            }

            String numericPart = normalized;
            String qualifierPart = "";
            int qualifierSeparator = normalized.indexOf('-');
            if (qualifierSeparator >= 0) {
                numericPart = normalized.substring(0, qualifierSeparator);
                qualifierPart = normalized.substring(qualifierSeparator + 1);
            }

            String[] numericSegments = numericPart.split("\\.");
            int major = parseNumericSegment(numericSegments, 0);
            int minor = parseNumericSegment(numericSegments, 1);
            int patch = parseNumericSegment(numericSegments, 2);
            return new ParsedVersion(major, minor, patch, parseQualifierTokens(qualifierPart));
        }

        private static int parseNumericSegment(String[] segments, int index) {
            if (segments == null || index >= segments.length) {
                return 0;
            }
            String segment = segments[index] == null ? "" : segments[index].trim();
            return segment.chars().allMatch(Character::isDigit) ? Integer.parseInt(segment) : 0;
        }

        private static List<QualifierToken> parseQualifierTokens(String qualifierPart) {
            if (qualifierPart == null || qualifierPart.isBlank()) {
                return List.of();
            }
            List<QualifierToken> tokens = new ArrayList<>();
            String[] groups = qualifierPart.split("[.-]");
            for (String group : groups) {
                if (group == null || group.isBlank()) {
                    continue;
                }
                StringBuilder current = new StringBuilder();
                Boolean currentNumeric = null;
                for (int i = 0; i < group.length(); i++) {
                    char ch = group.charAt(i);
                    boolean numeric = Character.isDigit(ch);
                    if (currentNumeric != null && currentNumeric != numeric) {
                        tokens.add(QualifierToken.of(current.toString(), currentNumeric));
                        current.setLength(0);
                    }
                    current.append(ch);
                    currentNumeric = numeric;
                }
                if (current.length() > 0 && currentNumeric != null) {
                    tokens.add(QualifierToken.of(current.toString(), currentNumeric));
                }
            }
            return List.copyOf(tokens);
        }

        @Override
        public int compareTo(ParsedVersion other) {
            int majorCompare = Integer.compare(major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            int minorCompare = Integer.compare(minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }
            int patchCompare = Integer.compare(patch, other.patch);
            if (patchCompare != 0) {
                return patchCompare;
            }
            if (qualifiers.isEmpty() && other.qualifiers.isEmpty()) {
                return 0;
            }
            if (qualifiers.isEmpty()) {
                return 1;
            }
            if (other.qualifiers.isEmpty()) {
                return -1;
            }
            int maxTokens = Math.max(qualifiers.size(), other.qualifiers.size());
            for (int i = 0; i < maxTokens; i++) {
                if (i >= qualifiers.size()) {
                    return -1;
                }
                if (i >= other.qualifiers.size()) {
                    return 1;
                }
                int tokenCompare = qualifiers.get(i).compareTo(other.qualifiers.get(i));
                if (tokenCompare != 0) {
                    return tokenCompare;
                }
            }
            return 0;
        }
    }

    private record QualifierToken(String value, boolean numeric) implements Comparable<QualifierToken> {
        private static QualifierToken of(String value, boolean numeric) {
            return new QualifierToken(value, numeric);
        }

        @Override
        public int compareTo(QualifierToken other) {
            if (numeric && other.numeric) {
                return Integer.compare(Integer.parseInt(value), Integer.parseInt(other.value));
            }
            if (numeric) {
                return -1;
            }
            if (other.numeric) {
                return 1;
            }
            return value.compareToIgnoreCase(other.value);
        }
    }
}

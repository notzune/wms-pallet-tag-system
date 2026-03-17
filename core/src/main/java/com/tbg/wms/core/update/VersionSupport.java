package com.tbg.wms.core.update;

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

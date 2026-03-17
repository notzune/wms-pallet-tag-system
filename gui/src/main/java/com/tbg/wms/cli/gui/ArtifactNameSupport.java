package com.tbg.wms.cli.gui;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Builds stable filesystem-safe artifact names from workflow identifiers.
 *
 * <p>The GUI generates ZPL artifacts from shipment IDs, LPNs, and ad hoc barcode payloads.
 * Centralizing slug rules keeps artifact naming predictable across workflow types and avoids
 * path-invalid characters leaking into operator output folders.</p>
 */
final class ArtifactNameSupport {
    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9]+");

    private ArtifactNameSupport() {
    }

    static String safeSlug(String value, String fallback, int maxLength) {
        Objects.requireNonNull(fallback, "fallback cannot be null");
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be > 0");
        }
        String normalizedFallback = fallback.trim().isEmpty() ? "id" : fallback.trim().toLowerCase(Locale.ROOT);
        if (value == null) {
            return truncate(normalizedFallback, maxLength);
        }
        String slug = NON_ALNUM_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        while (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        if (slug.isEmpty()) {
            slug = normalizedFallback;
        }
        return truncate(slug, maxLength);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}

/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.db;

/**
 * Shared heuristics for determining whether resolved item text is human-readable.
 *
 * <p><strong>Why this helper exists:</strong> description resolution in
 * {@link OracleDbQueryRepository} probes multiple sources (PRTDSC, PRTMST, and
 * fallback order text). The "is this text actually label-safe?" rule must remain
 * consistent across every probe branch.</p>
 *
 * <p><strong>Why it is necessary:</strong> keeping the rule centralized prevents
 * branch drift and accidental behavior changes when new description sources are
 * added. It also keeps repository query code focused on SQL/data access concerns
 * (SRP) while this class owns text-quality policy.</p>
 */
final class DescriptionTextHeuristics {

    private DescriptionTextHeuristics() {
    }

    /**
     * Returns {@code true} when the candidate value contains at least one letter.
     *
     * <p>Current policy intentionally rejects blank and numeric-only strings to
     * avoid emitting non-descriptive label text.</p>
     */
    static boolean isHumanReadable(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}

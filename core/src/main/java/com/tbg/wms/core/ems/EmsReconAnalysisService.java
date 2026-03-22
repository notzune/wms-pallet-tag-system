/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.core.ems;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Classifies EMS reconciliation rows into actionable mismatch findings.
 *
 * <p>This service owns the report-only reasoning layer for EMS reconciliation so categorization,
 * persistence hints, and suggested operator actions can evolve without leaking spreadsheet logic
 * into CLI command code.</p>
 */
public final class EmsReconAnalysisService {

    public EmsReconAnalysisReport analyze(String sourceFile, List<EmsReconRow> rows, EndpointProbeResult qaProbe) {
        Objects.requireNonNull(sourceFile, "sourceFile cannot be null");
        Objects.requireNonNull(rows, "rows cannot be null");

        Map<String, Integer> latestPassByKey = rows.stream().collect(Collectors.toMap(
                EmsReconRow::stableKey,
                EmsReconRow::getPassIndex,
                Math::max,
                LinkedHashMap::new
        ));

        List<EmsReconFinding> findings = new ArrayList<>();
        for (EmsReconRow row : rows) {
            EmsMismatchCategory category = classify(row);
            boolean persists = latestPassByKey.getOrDefault(row.stableKey(), row.getPassIndex()) > row.getPassIndex();
            findings.add(new EmsReconFinding(
                    row.getPassLabel(),
                    row.getPassIndex(),
                    row.getSheetRowNumber(),
                    row.getContainerId(),
                    row.getCurrentLocationId(),
                    row.getSku(),
                    row.getAsrsLocNum(),
                    row.getLodnum(),
                    row.getMaxPrtnum(),
                    category,
                    persists,
                    explanation(row, category, persists),
                    recommendation(category, persists),
                    suggestedFixCommand(row, category)
            ));
        }

        findings.sort(Comparator.comparingInt(EmsReconFinding::getPassIndex)
                .thenComparingInt(EmsReconFinding::getSheetRowNumber));

        List<EmsReconPassSummary> passes = rows.stream()
                .collect(Collectors.groupingBy(EmsReconRow::getPassIndex, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> new EmsReconPassSummary(
                        entry.getValue().get(0).getPassLabel(),
                        entry.getKey(),
                        entry.getValue().size()))
                .collect(Collectors.toList());

        List<String> notes = buildNotes(rows, findings, qaProbe);
        return new EmsReconAnalysisReport(LocalDateTime.now(), sourceFile, findings.size(), passes, findings, notes, qaProbe);
    }

    private EmsMismatchCategory classify(EmsReconRow row) {
        if (row.getSku().toUpperCase(Locale.ROOT).contains("MIXED")) {
            return EmsMismatchCategory.MIXED_SKU;
        }
        if (row.hasEmsData() && !row.hasWmsData()) {
            return EmsMismatchCategory.EMS_ONLY;
        }
        if (!row.hasEmsData() && row.hasWmsData()) {
            return EmsMismatchCategory.WMS_ONLY;
        }
        if (!normalizedLocation(row.getCurrentLocationId()).isEmpty()
                && !normalizedLocation(row.getAsrsLocNum()).isEmpty()
                && !normalizedLocation(row.getCurrentLocationId()).equals(normalizedLocation(row.getAsrsLocNum()))) {
            return EmsMismatchCategory.LOCATION_DRIFT;
        }
        if (!row.getContainerId().isEmpty() && (!row.getLodnum().isEmpty() || !row.getMaxPrtnum().isEmpty())) {
            return EmsMismatchCategory.REINDUCT_CANDIDATE;
        }
        return EmsMismatchCategory.UNKNOWN;
    }

    private String explanation(EmsReconRow row, EmsMismatchCategory category, boolean persists) {
        String persistenceText = persists
                ? "The mismatch also appears in a later pass, so it is likely still unresolved."
                : "The mismatch does not appear in a later pass, so it may have self-corrected or been manually fixed.";
        switch (category) {
            case EMS_ONLY:
                return "EMS shows container " + value(row.getContainerId())
                        + " at " + value(row.getCurrentLocationId())
                        + " but the report has no matching WMS inventory reference. " + persistenceText;
            case WMS_ONLY:
                return "WMS shows load " + value(row.getLodnum())
                        + " / pallet " + value(row.getMaxPrtnum())
                        + " without a matching EMS inventory row. " + persistenceText;
            case LOCATION_DRIFT:
                return "EMS location " + value(row.getCurrentLocationId())
                        + " does not match WMS ASRS location " + value(row.getAsrsLocNum())
                        + ". This is consistent with a missed scan or post-reconnect location drift. " + persistenceText;
            case MIXED_SKU:
                return "Container " + value(row.getContainerId())
                        + " is flagged as MIXED in EMS, which usually needs manual validation before any automated reinduct or correction. "
                        + persistenceText;
            case REINDUCT_CANDIDATE:
                return "Both EMS and WMS identifiers are present for container " + value(row.getContainerId())
                        + ", which makes this a good candidate for a controlled reinduct or synchronization command. "
                        + persistenceText;
            default:
                return "The row does not match a stronger pattern yet. More WMS/EMS history is required before automating a correction. "
                        + persistenceText;
        }
    }

    private String recommendation(EmsMismatchCategory category, boolean persists) {
        switch (category) {
            case EMS_ONLY:
                return persists
                        ? "Check WMS history for missing putaway or rejected host transaction, then rebuild or reinduct the container."
                        : "Confirm the container is absent from the latest WMS view before taking action.";
            case WMS_ONLY:
                return "Verify whether EMS missed the inbound/outbound confirmation, then re-present the pallet only if the latest PLC path confirms physical presence.";
            case LOCATION_DRIFT:
                return "Compare EMS move timestamps, PLC scan history, and WMS transaction history around the disconnect window before issuing a reinduct.";
            case MIXED_SKU:
                return "Hold for manual review. Mixed pallets should not be auto-corrected without confirming the physical pallet and expected inventory.";
            case REINDUCT_CANDIDATE:
                return "Prepare a QA reinduct command using the container or load identifier, but only execute after pre-checking current WMS status.";
            default:
                return "Escalate for manual investigation and collect EMS host messaging plus WMS transaction history for this identifier.";
        }
    }

    private String suggestedFixCommand(EmsReconRow row, EmsMismatchCategory category) {
        if (category == EmsMismatchCategory.MIXED_SKU || category == EmsMismatchCategory.UNKNOWN) {
            return "MANUAL_REVIEW_REQUIRED";
        }
        String key = !row.getContainerId().isEmpty() ? row.getContainerId()
                : !row.getLodnum().isEmpty() ? row.getLodnum()
                : row.getMaxPrtnum();
        if (key.isEmpty()) {
            return "IDENTIFIER_REQUIRED";
        }
        return "REINDUCT " + key;
    }

    private List<String> buildNotes(List<EmsReconRow> rows, List<EmsReconFinding> findings, EndpointProbeResult qaProbe) {
        List<String> notes = new ArrayList<>();
        long persistentCount = findings.stream().filter(EmsReconFinding::isPersistsIntoLaterPasses).count();
        notes.add("Parsed " + rows.size() + " mismatch rows across " + rows.stream().map(EmsReconRow::getPassIndex).distinct().count() + " pass(es).");
        notes.add("Persistent mismatches across passes: " + persistentCount + ".");
        if (qaProbe != null) {
            if (qaProbe.isReachable()) {
                notes.add("QA endpoint " + qaProbe.getHost() + ":" + qaProbe.getPort()
                        + " is reachable; banner preview: " + qaProbe.getBannerPreview() + ".");
            } else {
                notes.add("QA endpoint " + qaProbe.getHost() + ":" + qaProbe.getPort()
                        + " was not reachable from this workstation: " + qaProbe.getError() + ".");
            }
        }
        notes.add("The current implementation classifies report evidence only. It does not query EMS or WMS databases yet.");
        return notes;
    }

    private String normalizedLocation(String location) {
        return location == null ? "" : location.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String value(String input) {
        return input == null || input.isBlank() ? "<missing>" : input;
    }
}

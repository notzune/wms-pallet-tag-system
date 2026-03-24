/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.db;

import com.tbg.wms.core.model.NormalizationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves human-readable shipment item descriptions from Oracle description tables.
 *
 * <p>This helper centralizes the description fallback order and per-load cache so shipment
 * footprint hydration does not duplicate text-selection logic across repository methods.</p>
 */
final class ShipmentDescriptionSupport {
    private final Map<DescriptionLookupKey, String> descriptionCache = new HashMap<>();

    String resolveItemDescription(Connection conn,
                                  String sku,
                                  String prtClientId,
                                  String whId,
                                  String fallbackDescription,
                                  List<String> descriptionColumns) {
        DescriptionLookupKey key = new DescriptionLookupKey(
                NormalizationService.normalizeSku(sku),
                NormalizationService.normalizeString(prtClientId),
                NormalizationService.normalizeString(whId),
                NormalizationService.normalizeString(fallbackDescription)
        );
        if (descriptionCache.containsKey(key)) {
            return descriptionCache.get(key);
        }

        String prtdscDescription = fetchDescriptionFromPrtdsc(conn, sku, prtClientId, whId);
        String resolved = DescriptionTextHeuristics.isHumanReadable(prtdscDescription)
                ? prtdscDescription
                : chooseBestDescription(
                null,
                fetchDescriptionFromPrtmst(conn, sku, prtClientId, descriptionColumns),
                fallbackDescription
        );
        descriptionCache.put(key, resolved);
        return resolved;
    }

    void clearCache() {
        descriptionCache.clear();
    }

    String chooseBestDescription(String prtdscDescription, String prtmstDescription, String fallbackDescription) {
        if (DescriptionTextHeuristics.isHumanReadable(prtdscDescription)) {
            return prtdscDescription;
        }
        if (DescriptionTextHeuristics.isHumanReadable(prtmstDescription)) {
            return prtmstDescription;
        }
        if (DescriptionTextHeuristics.isHumanReadable(fallbackDescription)) {
            return fallbackDescription;
        }
        return null;
    }

    private String fetchDescriptionFromPrtdsc(Connection conn, String sku, String prtClientId, String whId) {
        if (sku == null || sku.isBlank()) {
            return null;
        }

        List<String> clientCandidates = new ArrayList<>();
        if (prtClientId != null && !prtClientId.isBlank()) {
            clientCandidates.add(prtClientId);
        }
        clientCandidates.add("----");

        List<String> whCandidates = new ArrayList<>();
        if (whId != null && !whId.isBlank()) {
            whCandidates.add(whId);
        }
        whCandidates.add("----");

        String sql = "SELECT SHORT_DSC, LNGDSC FROM WMSP.PRTDSC "
                + "WHERE COLNAM = 'prtnum|prt_client_id|wh_id_tmpl' AND COLVAL = ? "
                + "FETCH FIRST 1 ROWS ONLY";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String skuCandidate : SkuCandidateBuilder.buildCandidates(sku)) {
                for (String clientCandidate : clientCandidates) {
                    for (String whCandidate : whCandidates) {
                        String colVal = skuCandidate + "|" + clientCandidate + "|" + whCandidate;
                        stmt.setString(1, colVal);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String shortDsc = NormalizationService.normalizeString(rs.getString("SHORT_DSC"));
                                if (DescriptionTextHeuristics.isHumanReadable(shortDsc)) {
                                    return shortDsc;
                                }
                                String longDsc = NormalizationService.normalizeString(rs.getString("LNGDSC"));
                                if (DescriptionTextHeuristics.isHumanReadable(longDsc)) {
                                    return longDsc;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            return null;
        }
        return null;
    }

    private String fetchDescriptionFromPrtmst(Connection conn,
                                              String sku,
                                              String prtClientId,
                                              List<String> descriptionColumns) {
        if (sku == null || sku.isBlank() || descriptionColumns == null || descriptionColumns.isEmpty()) {
            return null;
        }

        String selectCols = String.join(", ", descriptionColumns);
        boolean hasClientId = prtClientId != null && !prtClientId.isBlank();
        String sql = hasClientId
                ? "SELECT " + selectCols + " FROM WMSP.PRTMST WHERE PRTNUM = ? AND PRT_CLIENT_ID = ? FETCH FIRST 3 ROWS ONLY"
                : "SELECT " + selectCols + " FROM WMSP.PRTMST WHERE PRTNUM = ? FETCH FIRST 3 ROWS ONLY";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String skuCandidate : SkuCandidateBuilder.buildCandidates(sku)) {
                stmt.setString(1, skuCandidate);
                if (hasClientId) {
                    stmt.setString(2, prtClientId);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        for (String column : descriptionColumns) {
                            String value = NormalizationService.normalizeString(rs.getString(column));
                            if (DescriptionTextHeuristics.isHumanReadable(value)) {
                                return value;
                            }
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            return null;
        }
        return null;
    }

    private record DescriptionLookupKey(
            String sku,
            String prtClientId,
            String whId,
            String fallbackDescription
    ) {
    }
}

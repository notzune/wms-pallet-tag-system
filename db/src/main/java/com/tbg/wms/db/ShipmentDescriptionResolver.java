/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.2
 */
package com.tbg.wms.db;

import com.tbg.wms.core.model.NormalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves shipment SKU descriptions using a request-scoped cache so repeated fallback lookups do
 * not amplify database traffic inside a single shipment load.
 */
final class ShipmentDescriptionResolver {
    private static final Logger log = LoggerFactory.getLogger(ShipmentDescriptionResolver.class);

    private final Connection connection;
    private final List<String> descriptionColumns;
    private final Map<LookupKey, String> cache = new HashMap<>();

    ShipmentDescriptionResolver(
            Connection connection,
            List<String> descriptionColumns,
            PrtmstDescriptionColumnResolver columnResolver
    ) {
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        Objects.requireNonNull(columnResolver, "columnResolver cannot be null");
        this.descriptionColumns = descriptionColumns == null || descriptionColumns.isEmpty()
                ? columnResolver.getColumns(connection)
                : List.copyOf(descriptionColumns);
    }

    String resolveDescription(String sku, String prtClientId, String whId, String fallbackDescription) {
        LookupKey key = new LookupKey(
                NormalizationService.normalizeSku(sku),
                NormalizationService.normalizeString(prtClientId),
                NormalizationService.normalizeString(whId),
                NormalizationService.normalizeString(fallbackDescription)
        );
        return cache.computeIfAbsent(key, this::resolveUncached);
    }

    private String resolveUncached(LookupKey key) {
        String prtdscDescription = fetchDescriptionFromPrtdsc(key.sku(), key.prtClientId(), key.whId());
        if (DescriptionTextHeuristics.isHumanReadable(prtdscDescription)) {
            return prtdscDescription;
        }

        String prtmstDescription = fetchDescriptionFromPrtmst(key.sku(), key.prtClientId());
        if (DescriptionTextHeuristics.isHumanReadable(prtmstDescription)) {
            return prtmstDescription;
        }
        return DescriptionTextHeuristics.isHumanReadable(key.fallbackDescription()) ? key.fallbackDescription() : null;
    }

    private String fetchDescriptionFromPrtdsc(String sku, String prtClientId, String whId) {
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

        String sql = "SELECT SHORT_DSC, LNGDSC FROM WMSP.PRTDSC " +
                "WHERE COLNAM = 'prtnum|prt_client_id|wh_id_tmpl' AND COLVAL = ? " +
                "FETCH FIRST 1 ROWS ONLY";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        } catch (SQLException e) {
            log.debug("Could not query PRTDSC for SKU {}: {}", sku, e.getMessage());
        }
        return null;
    }

    private String fetchDescriptionFromPrtmst(String sku, String prtClientId) {
        if (sku == null || sku.isBlank() || descriptionColumns.isEmpty()) {
            return null;
        }

        String selectCols = String.join(", ", descriptionColumns);
        boolean hasClientId = prtClientId != null && !prtClientId.isBlank();
        String sql = hasClientId
                ? "SELECT " + selectCols + " FROM WMSP.PRTMST WHERE PRTNUM = ? AND PRT_CLIENT_ID = ? FETCH FIRST 3 ROWS ONLY"
                : "SELECT " + selectCols + " FROM WMSP.PRTMST WHERE PRTNUM = ? FETCH FIRST 3 ROWS ONLY";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
        } catch (SQLException e) {
            log.debug("Could not resolve PRTMST description for SKU {}: {}", sku, e.getMessage());
        }
        return null;
    }

    private record LookupKey(String sku, String prtClientId, String whId, String fallbackDescription) {
    }
}

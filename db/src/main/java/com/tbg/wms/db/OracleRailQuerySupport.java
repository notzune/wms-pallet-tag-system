/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.db;

import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.model.NormalizationService;
import com.tbg.wms.core.rail.RailFootprintCandidate;
import com.tbg.wms.core.rail.RailStopRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Rail-specific Oracle query helpers.
 */
final class OracleRailQuerySupport {

    private static final int RAIL_FOOTPRINT_BATCH_SIZE = 900;
    private final DataSource dataSource;
    private final RailFootprintCandidateSupport footprintCandidateSupport;

    OracleRailQuerySupport(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.footprintCandidateSupport = new RailFootprintCandidateSupport();
    }

    List<RailStopRecord> findRailStopsByTrainId(String normalizedTrainId) {
        String sql = "SELECT " +
                "  TO_CHAR(SYSDATE, 'MM-DD-YY') AS RUN_DATE, " +
                "  SUBSTR(t.VC_TRAIN_NUM, 3, 4) AS TRAIN_NBR, " +
                "  DECODE(ri.SUPNUM, '1011', 'FP', '1000', 'BR', '3230', 'MW', '3322', 'DF') AS DCS_WHSE, " +
                "  ri.INVNUM AS LOAD_NBR, " +
                "  t.TRLR_NUM AS VEHICLE_ID, " +
                "  t.VC_CAR_SEQ AS SEQ, " +
                "  ap.ALT_PRTNUM AS SHORT_CODE, " +
                "  SUM(NVL(rl.EXPQTY, 0)) AS TOTAL_CASES " +
                "FROM WMSP.TRLR t " +
                "LEFT JOIN WMSP.RCVTRK rt ON rt.TRLR_ID = t.TRLR_ID " +
                "LEFT JOIN WMSP.RCVINV ri ON ri.TRKNUM = rt.TRKNUM " +
                "LEFT JOIN WMSP.RCVLIN rl ON rl.TRKNUM = rt.TRKNUM " +
                "LEFT JOIN WMSP.ALT_PRTMST ap ON ap.PRTNUM = rl.PRTNUM " +
                "  AND ap.ALT_PRT_TYP = 'UPC' " +
                "WHERE t.VC_TRAIN_NUM = ? " +
                "  AND ri.INVNUM IS NOT NULL " +
                "  AND ap.ALT_PRTNUM IS NOT NULL " +
                "  AND NVL(rl.EXPQTY, 0) > 0 " +
                "GROUP BY SUBSTR(t.VC_TRAIN_NUM, 3, 4), " +
                "  DECODE(ri.SUPNUM, '1011', 'FP', '1000', 'BR', '3230', 'MW', '3322', 'DF'), " +
                "  ri.INVNUM, t.TRLR_NUM, t.VC_CAR_SEQ, ap.ALT_PRTNUM " +
                "ORDER BY t.VC_CAR_SEQ ASC, ri.INVNUM ASC, ap.ALT_PRTNUM ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizedTrainId);
            try (ResultSet rs = stmt.executeQuery()) {
                Map<RailStopGroupKey, MutableRailStop> grouped = new LinkedHashMap<>();
                while (rs.next()) {
                    String date = NormalizationService.normalizeString(rs.getString("RUN_DATE"));
                    String sequence = integerToString(rs, "SEQ");
                    String trainNumber = NormalizationService.normalizeString(rs.getString("TRAIN_NBR"));
                    String warehouse = NormalizationService.normalizeString(rs.getString("DCS_WHSE"));
                    String loadNumber = NormalizationService.normalizeString(rs.getString("LOAD_NBR"));
                    String vehicleId = NormalizationService.normalizeString(rs.getString("VEHICLE_ID"));
                    String shortCode = NormalizationService.normalizeString(rs.getString("SHORT_CODE"));
                    int cases = rs.getInt("TOTAL_CASES");

                    if (loadNumber.isBlank() || shortCode.isBlank() || cases <= 0) {
                        continue;
                    }

                    RailStopGroupKey key = new RailStopGroupKey(sequence, loadNumber, vehicleId, warehouse, trainNumber);
                    MutableRailStop stop = grouped.computeIfAbsent(key,
                            ignored -> new MutableRailStop(date, sequence, trainNumber, vehicleId, warehouse, loadNumber));
                    stop.items.add(new RailStopRecord.ItemQuantity(shortCode, cases));
                }

                List<RailStopRecord> records = new ArrayList<>(grouped.size());
                for (MutableRailStop stop : grouped.values()) {
                    stop.items.sort(Comparator.comparing(RailStopRecord.ItemQuantity::getItemNumber));
                    records.add(new RailStopRecord(
                            stop.date,
                            stop.sequence,
                            stop.trainNumber,
                            stop.vehicleId,
                            stop.warehouse,
                            stop.loadNumber,
                            stop.items
                    ));
                }
                return records;
            }
        } catch (SQLException e) {
            throw new WmsDbConnectivityException(
                    "Failed to retrieve rail rows for train " + normalizedTrainId + ": " + e.getMessage(),
                    e,
                    "Verify SELECT access to WMSP.TRLR, WMSP.RCVTRK, WMSP.RCVINV, WMSP.RCVLIN, and WMSP.ALT_PRTMST"
            );
        }
    }

    Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
        Objects.requireNonNull(shortCodes, "shortCodes cannot be null");

        List<String> normalized = normalizeShortCodes(shortCodes);
        if (normalized.isEmpty()) {
            return Map.of();
        }

        Map<String, List<RailFootprintCandidate>> byShortCode = new LinkedHashMap<>();
        String baseSql =
                "SELECT " +
                        "  ap.ALT_PRTNUM AS SHORT_CODE, " +
                        "  ap.PRTNUM AS ITEM_NBR, " +
                        "  p.PRTFAM AS PRTFAM, " +
                        "  p.UC_PARS_FLG AS UC_PARS_FLG, " +
                        "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.UNTQTY END) AS UNITS_PER_PALLET " +
                        "FROM WMSP.ALT_PRTMST ap " +
                        "LEFT JOIN WMSP.PRTMST p ON p.PRTNUM = ap.PRTNUM " +
                        "  AND p.PRT_CLIENT_ID = ap.PRT_CLIENT_ID " +
                        "LEFT JOIN WMSP.PRTFTP pf ON pf.PRTNUM = ap.PRTNUM " +
                        "  AND pf.PRT_CLIENT_ID = ap.PRT_CLIENT_ID " +
                        "  AND pf.DEFFTP_FLG = 1 " +
                        "LEFT JOIN WMSP.PRTFTP_DTL d ON d.PRTNUM = pf.PRTNUM " +
                        "  AND d.PRT_CLIENT_ID = pf.PRT_CLIENT_ID " +
                        "  AND d.WH_ID = pf.WH_ID " +
                        "  AND d.FTPCOD = pf.FTPCOD " +
                        "WHERE ap.ALT_PRT_TYP = 'UPC' " +
                        "  AND ap.ALT_PRTNUM IN (%s) " +
                        "GROUP BY ap.ALT_PRTNUM, ap.PRTNUM, p.PRTFAM, p.UC_PARS_FLG";

        try (Connection conn = dataSource.getConnection()) {
            for (int start = 0; start < normalized.size(); start += RAIL_FOOTPRINT_BATCH_SIZE) {
                int end = Math.min(start + RAIL_FOOTPRINT_BATCH_SIZE, normalized.size());
                List<String> batch = normalized.subList(start, end);
                String placeholders = sqlPlaceholders(batch.size());
                try (PreparedStatement stmt = conn.prepareStatement(String.format(baseSql, placeholders))) {
                    for (int i = 0; i < batch.size(); i++) {
                        stmt.setString(i + 1, batch.get(i));
                    }
                    collectFootprintCandidates(stmt, byShortCode);
                }
            }
        } catch (SQLException e) {
            throw new WmsDbConnectivityException(
                    "Failed to retrieve rail footprint candidates: " + e.getMessage(),
                    e,
                    "Verify SELECT access to WMSP.ALT_PRTMST, WMSP.PRTMST, WMSP.PRTFTP, and WMSP.PRTFTP_DTL"
            );
        }

        footprintCandidateSupport.sortCandidates(byShortCode);
        return byShortCode;
    }

    private static List<String> normalizeShortCodes(List<String> shortCodes) {
        Set<String> normalizedSet = new LinkedHashSet<>();
        for (String code : shortCodes) {
            String value = NormalizationService.normalizeString(code);
            if (!value.isBlank()) {
                normalizedSet.add(value);
            }
        }
        return new ArrayList<>(normalizedSet);
    }

    private void collectFootprintCandidates(
            PreparedStatement stmt,
            Map<String, List<RailFootprintCandidate>> byShortCode
    ) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            footprintCandidateSupport.collectCandidates(rs, byShortCode);
        }
    }

    private static String sqlPlaceholders(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
        StringBuilder sb = new StringBuilder(count * 2);
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        return sb.toString();
    }

    private static String integerToString(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        if (rs.wasNull()) {
            return "";
        }
        return Integer.toString(value);
    }

    private static final class MutableRailStop {
        private final String date;
        private final String sequence;
        private final String trainNumber;
        private final String vehicleId;
        private final String warehouse;
        private final String loadNumber;
        private final List<RailStopRecord.ItemQuantity> items = new ArrayList<>();

        private MutableRailStop(
                String date,
                String sequence,
                String trainNumber,
                String vehicleId,
                String warehouse,
                String loadNumber
        ) {
            this.date = date == null ? "" : date.trim();
            this.sequence = sequence == null ? "" : sequence.trim();
            this.trainNumber = trainNumber == null ? "" : trainNumber.trim();
            this.vehicleId = vehicleId == null ? "" : vehicleId.trim();
            this.warehouse = warehouse == null ? "" : warehouse.trim();
            this.loadNumber = loadNumber == null ? "" : loadNumber.trim();
        }
    }

    private static final class RailStopGroupKey {
        private final String sequence;
        private final String loadNumber;
        private final String vehicleId;
        private final String warehouse;
        private final String trainNumber;

        private RailStopGroupKey(
                String sequence,
                String loadNumber,
                String vehicleId,
                String warehouse,
                String trainNumber
        ) {
            this.sequence = sequence;
            this.loadNumber = loadNumber;
            this.vehicleId = vehicleId;
            this.warehouse = warehouse;
            this.trainNumber = trainNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RailStopGroupKey)) {
                return false;
            }
            RailStopGroupKey that = (RailStopGroupKey) o;
            return Objects.equals(sequence, that.sequence)
                    && Objects.equals(loadNumber, that.loadNumber)
                    && Objects.equals(vehicleId, that.vehicleId)
                    && Objects.equals(warehouse, that.warehouse)
                    && Objects.equals(trainNumber, that.trainNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sequence, loadNumber, vehicleId, warehouse, trainNumber);
        }
    }
}

/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.db;

import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.model.*;
import com.tbg.wms.core.rail.RailFootprintCandidate;
import com.tbg.wms.core.rail.RailStopRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Oracle implementation of DbQueryRepository.
 * <p>
 * This class executes Oracle WMS queries against the WMSP schema to retrieve
 * shipment data with all related information (orders, lines, pallets, lots, etc.).
 * <p>
 * Uses prepared statements to prevent SQL injection and proper resource
 * management with try-with-resources.
 * <p>
 * Database: Oracle WMS at site-specific location (e.g., TBG3002 at 10.19.68.61:1521/WMSP)
 * Schema: WMSP (schema name is prepended to all table references)
 * User: RPTADM (read-only reporting user)
 */
public final class OracleDbQueryRepository implements DbQueryRepository {

    private static final Logger log = LoggerFactory.getLogger(OracleDbQueryRepository.class);

    private final DataSource dataSource;
    private final PrtmstDescriptionColumnResolver prtmstColumnResolver;
    private final OracleShipmentQuerySupport shipmentQuerySupport;

    /**
     * Creates a new OracleDbQueryRepository.
     *
     * @param dataSource the connection pool providing database connections
     * @throws IllegalArgumentException if dataSource is null
     */
    public OracleDbQueryRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.prtmstColumnResolver = new PrtmstDescriptionColumnResolver();
        this.shipmentQuerySupport = new OracleShipmentQuerySupport(dataSource, prtmstColumnResolver);
    }

    private static String normalizeRailFamilyCode(String prtfam, Integer parsFlag) {
        String family = NormalizationService.normalizeToUppercase(prtfam);
        // UC_PARS_FLG=1 is an explicit WMS override for CAN handling.
        if (parsFlag != null && parsFlag == 1) {
            return "CAN";
        }
        if (family.contains("CAN")) {
            return "CAN";
        }
        if (family.contains("KEV")) {
            return "KEV";
        }
        if (family.contains("DOM")) {
            return "DOM";
        }
        if (family.isBlank()) {
            return "DOM";
        }
        if (family.length() > 3) {
            return family.substring(0, 3);
        }
        return family;
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

    private static String requireNormalizedId(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return NormalizationService.normalizeString(value);
    }

    private static String requireNormalizedUppercaseId(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return NormalizationService.normalizeToUppercase(value);
    }

    @Override
    public Shipment findShipmentWithLpnsAndLineItems(String shipmentId) {
        String normalizedId = requireNormalizedId(shipmentId, "shipmentId");
        log.info("Retrieving shipment with LPNs and line items: {}", normalizedId);

        try {
            // Step 1: Fetch shipment header with address and order info
            Shipment shipment = shipmentQuerySupport.loadShipment(normalizedId);
            if (shipment == null) {
                log.warn("Shipment not found: {}", normalizedId);
                return null;
            }
            log.debug("Found {} LPNs for shipment {}", shipment.getLpnCount(), normalizedId);
            return shipment;
        } catch (SQLException e) {
            log.error("Database error retrieving shipment {}: {}", normalizedId, e.getSQLState());
            throw new WmsDbConnectivityException(
                    "Failed to retrieve shipment: " + e.getMessage(),
                    e,
                    "Check database connectivity, verify shipment ID exists in WMSP.SHIPMENT table"
            );
        }
    }

    @Override
    public boolean shipmentExists(String shipmentId) {
        String normalizedId = requireNormalizedId(shipmentId, "shipmentId");
        String sql = "SELECT 1 FROM WMSP.SHIPMENT WHERE SHIP_ID = ? AND ROWNUM = 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizedId);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                log.debug("Shipment {} exists: {}", normalizedId, exists);
                return exists;
            }
        } catch (SQLException e) {
            log.error("Database error checking shipment existence for {}: {}", normalizedId, e.getSQLState());
            throw new WmsDbConnectivityException(
                    "Failed to check shipment existence: " + e.getMessage(),
                    e,
                    "Verify database connection and RPTADM user has SELECT on WMSP.SHIPMENT"
            );
        }
    }

    @Override
    public String getStagingLocation(String shipmentId) {
        String normalizedId = requireNormalizedId(shipmentId, "shipmentId");
        String sql = "SELECT DISTINCT s.DSTLOC " +
                "FROM WMSP.SHIPMENT s " +
                "WHERE s.SHIP_ID = ? AND ROWNUM <= 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizedId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String location = rs.getString("DSTLOC");
                    String normalized = NormalizationService.normalizeOptionalStagingLocation(location);
                    log.debug("Staging location for shipment {}: {}", normalizedId, normalized);
                    return normalized;
                }
            }
            log.warn("No staging location found for shipment: {}", normalizedId);
            return null;
        } catch (SQLException e) {
            log.error("Database error retrieving staging location for {}: {}", normalizedId, e.getSQLState());
            throw new WmsDbConnectivityException(
                    "Failed to retrieve staging location: " + e.getMessage(),
                    e,
                    "Verify database connection and shipment exists in WMSP.SHIPMENT"
            );
        }
    }

    @Override
    public List<ShipmentSkuFootprint> findShipmentSkuFootprints(String shipmentId) {
        String normalizedId = requireNormalizedId(shipmentId, "shipmentId");
        try {
            List<ShipmentSkuFootprint> rows = shipmentQuerySupport.loadShipmentSkuFootprints(normalizedId);
            log.debug("Loaded {} footprint rows for shipment {}", rows.size(), normalizedId);
            return rows;
        } catch (SQLException e) {
            log.error("Database error retrieving footprint data for {}: {}", normalizedId, e.getSQLState());
            throw new WmsDbConnectivityException(
                    "Failed to retrieve footprint data: " + e.getMessage(),
                    e,
                    "Verify SELECT access to WMSP.PRTFTP and WMSP.PRTFTP_DTL for RPTADM user"
            );
        }
    }

    @Override
    public List<CarrierMoveStopRef> findCarrierMoveStops(String carrierMoveId) {
        String normalizedCarrierMoveId = requireNormalizedId(carrierMoveId, "carrierMoveId");
        List<CarrierMoveStopRef> rows = new ArrayList<>();
        String sql = "SELECT " +
                "  st.CAR_MOVE_ID, st.STOP_ID, st.STOP_SEQ, st.TMS_STOP_SEQ, " +
                "  s.SHIP_ID, s.SHPSTS, s.ADDDTE " +
                "FROM WMSP.STOP st " +
                "INNER JOIN WMSP.SHIPMENT s ON s.STOP_ID = st.STOP_ID " +
                "WHERE st.CAR_MOVE_ID = ? " +
                "ORDER BY st.STOP_SEQ ASC, s.SHIP_ID ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizedCarrierMoveId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer stopSeq = rs.getInt("STOP_SEQ");
                    if (rs.wasNull()) {
                        stopSeq = null;
                    }

                    Integer tmsStopSeq = rs.getInt("TMS_STOP_SEQ");
                    if (rs.wasNull()) {
                        tmsStopSeq = null;
                    }

                    LocalDateTime created = nullableLocalDateTime(rs, "ADDDTE");

                    rows.add(new CarrierMoveStopRef(
                            NormalizationService.normalizeString(rs.getString("CAR_MOVE_ID")),
                            NormalizationService.normalizeString(rs.getString("STOP_ID")),
                            stopSeq,
                            tmsStopSeq,
                            NormalizationService.normalizeString(rs.getString("SHIP_ID")),
                            NormalizationService.normalizeToUppercase(rs.getString("SHPSTS")),
                            created
                    ));
                }
            }
            return rows;
        } catch (SQLException e) {
            log.error("Database error resolving carrier move {}: {}", normalizedCarrierMoveId, e.getSQLState());
            throw new WmsDbConnectivityException(
                    "Failed to resolve carrier move shipments: " + e.getMessage(),
                    e,
                    "Verify SELECT access to WMSP.STOP and WMSP.SHIPMENT for RPTADM user"
            );
        }
    }

    @Override
    public List<RailStopRecord> findRailStopsByTrainId(String trainId) {
        String normalizedTrainId = requireNormalizedUppercaseId(trainId, "trainId");

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

    @Override
    public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
        Objects.requireNonNull(shortCodes, "shortCodes cannot be null");

        Set<String> normalizedSet = new LinkedHashSet<>();
        for (String code : shortCodes) {
            String value = NormalizationService.normalizeString(code);
            if (!value.isBlank()) {
                normalizedSet.add(value);
            }
        }
        List<String> normalized = new ArrayList<>(normalizedSet);
        if (normalized.isEmpty()) {
            return Map.of();
        }

        Map<String, List<RailFootprintCandidate>> byShortCode = new LinkedHashMap<>();
        final int batchSize = 900;
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
            for (int start = 0; start < normalized.size(); start += batchSize) {
                int end = Math.min(start + batchSize, normalized.size());
                List<String> batch = normalized.subList(start, end);
                String placeholders = sqlPlaceholders(batch.size());
                try (PreparedStatement stmt = conn.prepareStatement(String.format(baseSql, placeholders))) {
                    for (int i = 0; i < batch.size(); i++) {
                        stmt.setString(i + 1, batch.get(i));
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String shortCode = NormalizationService.normalizeString(rs.getString("SHORT_CODE"));
                            String itemNumber = NormalizationService.normalizeSku(rs.getString("ITEM_NBR"));
                            String familyCode = normalizeRailFamilyCode(
                                    rs.getString("PRTFAM"),
                                    nullableInt(rs, "UC_PARS_FLG")
                            );
                            Integer upp = nullableInt(rs, "UNITS_PER_PALLET");
                            int casesPerPallet = upp == null ? 0 : upp;
                            RailFootprintCandidate candidate = new RailFootprintCandidate(
                                    shortCode,
                                    itemNumber,
                                    familyCode,
                                    casesPerPallet
                            );
                            if (!candidate.isValid()) {
                                continue;
                            }
                            byShortCode.computeIfAbsent(shortCode, ignored -> new ArrayList<>()).add(candidate);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new WmsDbConnectivityException(
                    "Failed to retrieve rail footprint candidates: " + e.getMessage(),
                    e,
                    "Verify SELECT access to WMSP.ALT_PRTMST, WMSP.PRTMST, WMSP.PRTFTP, and WMSP.PRTFTP_DTL"
            );
        }

        for (List<RailFootprintCandidate> list : byShortCode.values()) {
            list.sort(Comparator.comparing(RailFootprintCandidate::getItemNumber));
        }
        return byShortCode;
    }

    @Override
    public void close() {
        // DataSource is managed by the connection pool
        // No explicit close needed here; connection pool handles lifecycle
        log.debug("OracleDbQueryRepository closed");
    }

    private LocalDateTime nullableLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static final class MutableRailStop {
        private final String date;
        private final String sequence;
        private final String trainNumber;
        private final String vehicleId;
        private final String warehouse;
        private final String loadNumber;
        private final List<RailStopRecord.ItemQuantity> items = new ArrayList<>();

        private MutableRailStop(String date,
                                String sequence,
                                String trainNumber,
                                String vehicleId,
                                String warehouse,
                                String loadNumber) {
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

        private RailStopGroupKey(String sequence,
                                 String loadNumber,
                                 String vehicleId,
                                 String warehouse,
                                 String trainNumber) {
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

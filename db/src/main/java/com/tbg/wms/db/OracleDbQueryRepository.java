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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern DC_NUMBER_PATTERN = Pattern.compile("(?i)\\bDC\\s*#?\\s*(\\d{3,6})\\b");
    private static final String DEFAULT_LINE_ITEM_UOM = "EA";
    private static final String SHIPMENT_LINE_ITEMS_SQL = "SELECT " +
            "pwd.SHIP_CTNNUM AS LODNUM, " +
            "sl.SHIP_LINE_ID, sl.ORDNUM, sl.ORDLIN, sl.ORDSLN, sl.CONS_BATCH, " +
            "COALESCE(" +
            "NULLIF(sl.SHPQTY, 0), " +
            "NULLIF(sl.STGQTY, 0), " +
            "NULLIF(sl.PCKQTY, 0), " +
            "NULLIF(sl.INPQTY, 0), " +
            "NULLIF(sl.TOT_PLN_QTY, 0), " +
            "NULLIF(ol.ORDQTY, 0), " +
            "0) AS EFFECTIVE_QTY, " +
            "ol.PRTNUM, ol.CSTPRT, ol.ORDQTY, ol.SALES_ORDNUM, ol.UNTPAK, " +
            "CAST(NULL AS VARCHAR2(1)) AS LNGDSC, ol.CSTPRT AS SRTDSC, 0 AS NETWGT " +
            "FROM WMSP.PCKWRK_DTL pwd " +
            "INNER JOIN WMSP.SHIPMENT_LINE sl ON pwd.SHIP_LINE_ID = sl.SHIP_LINE_ID " +
            "INNER JOIN WMSP.ORD_LINE ol ON sl.ORDNUM = ol.ORDNUM " +
            "  AND sl.ORDLIN = ol.ORDLIN AND sl.ORDSLN = ol.ORDSLN AND sl.CLIENT_ID = ol.CLIENT_ID " +
            "WHERE pwd.SHIP_ID = ? " +
            "ORDER BY pwd.SHIP_CTNNUM, sl.ORDLIN, sl.ORDSLN";

    private final DataSource dataSource;
    private final PrtmstDescriptionColumnResolver prtmstColumnResolver;

    /**
     * Creates a new OracleDbQueryRepository.
     *
     * @param dataSource the connection pool providing database connections
     * @throws IllegalArgumentException if dataSource is null
     */
    public OracleDbQueryRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.prtmstColumnResolver = new PrtmstDescriptionColumnResolver();
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
            Shipment shipment = fetchShipmentHeader(normalizedId);
            if (shipment == null) {
                log.warn("Shipment not found: {}", normalizedId);
                return null;
            }

            // Step 2: Fetch all LPNs for this shipment with their line items
            List<Lpn> lpns = fetchLpnsWithLineItems(normalizedId);
            log.debug("Found {} LPNs for shipment {}", lpns.size(), normalizedId);

            // Step 3: Create new shipment with loaded LPNs
            return new Shipment(
                    shipment.getShipmentId(),
                    shipment.getExternalId(),
                    shipment.getOrderId(),
                    shipment.getWarehouseId(),
                    shipment.getShipToName(),
                    shipment.getShipToAddress1(),
                    shipment.getShipToAddress2(),
                    shipment.getShipToAddress3(),
                    shipment.getShipToCity(),
                    shipment.getShipToState(),
                    shipment.getShipToZip(),
                    shipment.getShipToCountry(),
                    shipment.getShipToPhone(),
                    shipment.getCarrierCode(),
                    shipment.getServiceLevel(),
                    shipment.getDocumentNumber(),
                    shipment.getTrackingNumber(),
                    shipment.getDestinationLocation(),
                    shipment.getCustomerPo(),
                    shipment.getLocationNumber(),
                    shipment.getDepartmentNumber(),
                    shipment.getStopId(),
                    shipment.getStopSequence(),
                    shipment.getCarrierMoveId(),
                    shipment.getProNumber(),
                    shipment.getBolNumber(),
                    shipment.getStatus(),
                    shipment.getShipDate(),
                    shipment.getDeliveryDate(),
                    shipment.getCreatedDate(),
                    lpns
            );
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
        List<ShipmentSkuFootprint> rows = new ArrayList<>();

        String sql = "WITH sku_units AS (" +
                "  SELECT " +
                "    ol.PRTNUM AS PRTNUM, " +
                "    MAX(ol.PRT_CLIENT_ID) AS PRT_CLIENT_ID, " +
                "    MAX(s.WH_ID) AS WH_ID, " +
                "    MAX(ol.CSTPRT) AS ITEM_DESCRIPTION, " +
                "    SUM(COALESCE(" +
                "      NULLIF(sl.SHPQTY, 0), " +
                "      NULLIF(sl.STGQTY, 0), " +
                "      NULLIF(sl.PCKQTY, 0), " +
                "      NULLIF(sl.INPQTY, 0), " +
                "      NULLIF(sl.TOT_PLN_QTY, 0), " +
                "      NULLIF(ol.ORDQTY, 0), " +
                "      0)) AS TOTAL_UNITS " +
                "  FROM WMSP.SHIPMENT_LINE sl " +
                "  INNER JOIN WMSP.SHIPMENT s ON s.SHIP_ID = sl.SHIP_ID " +
                "  INNER JOIN WMSP.ORD_LINE ol ON sl.ORDNUM = ol.ORDNUM " +
                "    AND sl.ORDLIN = ol.ORDLIN AND sl.ORDSLN = ol.ORDSLN AND sl.CLIENT_ID = ol.CLIENT_ID " +
                "  WHERE sl.SHIP_ID = ? " +
                "  GROUP BY ol.PRTNUM" +
                ") " +
                "SELECT " +
                "  su.PRTNUM, " +
                "  su.PRT_CLIENT_ID, " +
                "  su.WH_ID, " +
                "  su.ITEM_DESCRIPTION, " +
                "  su.TOTAL_UNITS, " +
                "  MAX(CASE WHEN d.CAS_FLG = 1 THEN d.UNTQTY END) AS UNITS_PER_CASE, " +
                "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.UNTQTY END) AS UNITS_PER_PALLET, " +
                "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.LEN END) AS PALLET_LEN, " +
                "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.WID END) AS PALLET_WID, " +
                "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.HGT END) AS PALLET_HGT " +
                "FROM sku_units su " +
                "LEFT JOIN WMSP.PRTFTP pf ON pf.PRTNUM = su.PRTNUM " +
                "  AND pf.PRT_CLIENT_ID = su.PRT_CLIENT_ID " +
                "  AND pf.WH_ID = su.WH_ID " +
                "  AND pf.DEFFTP_FLG = 1 " +
                "LEFT JOIN WMSP.PRTFTP_DTL d ON d.PRTNUM = pf.PRTNUM " +
                "  AND d.PRT_CLIENT_ID = pf.PRT_CLIENT_ID " +
                "  AND d.WH_ID = pf.WH_ID " +
                "  AND d.FTPCOD = pf.FTPCOD " +
                "GROUP BY su.PRTNUM, su.PRT_CLIENT_ID, su.WH_ID, su.ITEM_DESCRIPTION, su.TOTAL_UNITS";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            List<String> descriptionColumns = prtmstColumnResolver.getColumns(conn);
            ShipmentDescriptionResolver descriptionResolver =
                    new ShipmentDescriptionResolver(conn, descriptionColumns, prtmstColumnResolver);
            stmt.setString(1, normalizedId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sku = NormalizationService.normalizeSku(rs.getString("PRTNUM"));
                    String fallbackDescription = NormalizationService.normalizeString(rs.getString("ITEM_DESCRIPTION"));
                    String itemDescription = descriptionResolver.resolveDescription(
                            sku,
                            NormalizationService.normalizeString(rs.getString("PRT_CLIENT_ID")),
                            NormalizationService.normalizeString(rs.getString("WH_ID")),
                            fallbackDescription
                    );

                    rows.add(new ShipmentSkuFootprint(
                            sku,
                            itemDescription,
                            rs.getInt("TOTAL_UNITS"),
                            nullableInt(rs, "UNITS_PER_CASE"),
                            nullableInt(rs, "UNITS_PER_PALLET"),
                            nullableDouble(rs, "PALLET_LEN"),
                            nullableDouble(rs, "PALLET_WID"),
                            nullableDouble(rs, "PALLET_HGT")
                    ));
                }
            }

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

    /**
     * Fetches the shipment header information with all related address, order, and carrier details.
     * <p>
     * This is the master query that retrieves ALL fields from:
     * - WMSP.SHIPMENT (header)
     * - WMSP.ADRMST (ship-to address)
     * - WMSP.ORD (order-level fields: CPONUM, DEST_NUM, DEPTNO)
     * - WMSP.STOP (stop sequence)
     * - WMSP.CAR_MOVE (carrier move details: PRO, BOL at move level)
     *
     * @param shipmentId the normalized shipment ID
     * @return Shipment with comprehensive header data, or null if not found
     * @throws SQLException if database operation fails
     */
    private Shipment fetchShipmentHeader(String shipmentId) throws SQLException {
        String sql = "SELECT " +
                "s.SHIP_ID, s.HOST_EXT_ID, s.WH_ID, s.SHPSTS, " +
                "s.CARCOD, s.SRVLVL, s.DOC_NUM, s.TRACK_NUM, s.STOP_ID, s.DSTLOC, " +
                "s.EARLY_SHPDTE, s.LATE_DLVDTE, s.ADDDTE, s.TMS_MOVE_ID, " +
                "a.ADRNAM, a.ADRLN1, a.ADRLN2, a.ADRLN3, " +
                "a.ADRCTY, a.ADRSTC, a.ADRPSZ, a.CTRY_NAME, a.PHNNUM, a.ATTN_NAME, a.HOST_EXT_ID AS ADR_HOST_EXT_ID, " +
                "(SELECT MAX(o2.CPONUM) FROM WMSP.SHIPMENT_LINE sl2 " +
                "  INNER JOIN WMSP.ORD o2 ON sl2.ORDNUM = o2.ORDNUM AND sl2.CLIENT_ID = o2.CLIENT_ID " +
                "  WHERE sl2.SHIP_ID = s.SHIP_ID AND o2.CPONUM IS NOT NULL) AS CPONUM, " +
                "(SELECT MAX(o2.DEST_NUM) FROM WMSP.SHIPMENT_LINE sl2 " +
                "  INNER JOIN WMSP.ORD o2 ON sl2.ORDNUM = o2.ORDNUM AND sl2.CLIENT_ID = o2.CLIENT_ID " +
                "  WHERE sl2.SHIP_ID = s.SHIP_ID AND o2.DEST_NUM IS NOT NULL) AS DEST_NUM, " +
                "(SELECT MAX(o2.VC_DEST_ID) FROM WMSP.SHIPMENT_LINE sl2 " +
                "  INNER JOIN WMSP.ORD o2 ON sl2.ORDNUM = o2.ORDNUM AND sl2.CLIENT_ID = o2.CLIENT_ID " +
                "  WHERE sl2.SHIP_ID = s.SHIP_ID AND o2.VC_DEST_ID IS NOT NULL) AS VC_DEST_ID, " +
                "(SELECT MAX(o2.DEPTNO) FROM WMSP.SHIPMENT_LINE sl2 " +
                "  INNER JOIN WMSP.ORD o2 ON sl2.ORDNUM = o2.ORDNUM AND sl2.CLIENT_ID = o2.CLIENT_ID " +
                "  WHERE sl2.SHIP_ID = s.SHIP_ID AND o2.DEPTNO IS NOT NULL) AS DEPTNO, " +
                "st.STOP_SEQ, " +
                "cm.DOC_NUM as CARRIER_BOL, cm.TRACK_NUM as CARRIER_PRO " +
                "FROM WMSP.SHIPMENT s " +
                "INNER JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID " +
                "LEFT JOIN WMSP.STOP st ON s.STOP_ID = st.STOP_ID " +
                "LEFT JOIN WMSP.CAR_MOVE cm ON s.TMS_MOVE_ID = cm.CAR_MOVE_ID " +
                "WHERE s.SHIP_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Extract all fields with null-safe handling
                    LocalDateTime shipDate = nullableLocalDateTime(rs, "EARLY_SHPDTE");
                    LocalDateTime deliveryDate = nullableLocalDateTime(rs, "LATE_DLVDTE");
                    LocalDateTime createdDate = nullableLocalDateTime(rs, "ADDDTE");
                    if (createdDate == null) {
                        createdDate = LocalDateTime.now();
                    }

                    Integer stopSeq = rs.getInt("STOP_SEQ");
                    if (rs.wasNull()) {
                        stopSeq = null;
                    }

                    String destinationNumber = resolveLocationNumber(
                            rs.getString("DEST_NUM"),
                            rs.getString("VC_DEST_ID"),
                            rs.getString("ADRNAM"),
                            rs.getString("ADR_HOST_EXT_ID")
                    );

                    return new Shipment(
                            NormalizationService.normalizeString(rs.getString("SHIP_ID")),
                            NormalizationService.normalizeString(rs.getString("HOST_EXT_ID")),
                            // Derive orderId from SHIPMENT_LINE (done in real call if needed)
                            extractFirstOrderNumber(conn, shipmentId),
                            NormalizationService.normalizeString(rs.getString("WH_ID")),
                            NormalizationService.normalizeString(rs.getString("ADRNAM")),
                            NormalizationService.normalizeString(rs.getString("ADRLN1")),
                            NormalizationService.normalizeString(rs.getString("ADRLN2")),
                            NormalizationService.normalizeString(rs.getString("ADRLN3")),
                            NormalizationService.normalizeString(rs.getString("ADRCTY")),
                            NormalizationService.normalizeToUppercase(rs.getString("ADRSTC")),
                            NormalizationService.normalizeString(rs.getString("ADRPSZ")),
                            NormalizationService.normalizeString(rs.getString("CTRY_NAME")),
                            NormalizationService.normalizeString(rs.getString("PHNNUM")),
                            NormalizationService.normalizeCarrierCode(rs.getString("CARCOD")),
                            NormalizationService.normalizeToUppercase(rs.getString("SRVLVL")),
                            NormalizationService.normalizeString(rs.getString("DOC_NUM")),
                            NormalizationService.normalizeString(rs.getString("TRACK_NUM")),
                            NormalizationService.normalizeOptionalStagingLocation(rs.getString("DSTLOC")),
                            NormalizationService.normalizeString(rs.getString("CPONUM")),
                            destinationNumber,
                            NormalizationService.normalizeString(rs.getString("DEPTNO")),
                            NormalizationService.normalizeString(rs.getString("STOP_ID")),
                            stopSeq,
                            NormalizationService.normalizeString(rs.getString("TMS_MOVE_ID")),
                            NormalizationService.normalizeString(rs.getString("CARRIER_PRO")),
                            NormalizationService.normalizeString(rs.getString("CARRIER_BOL")),
                            NormalizationService.normalizeToUppercase(rs.getString("SHPSTS")),
                            shipDate,
                            deliveryDate,
                            createdDate,
                            new ArrayList<>()
                    );
                }
            }
            return null;
        }
    }

    /**
     * Fetches all LPNs for a shipment with their line items.
     * <p>
     * Uses the PCKWRK_DTL → INVDTL → INVSUB → INVLOD chain to get complete pallet data
     * including lot tracking and dates.
     *
     * @param shipmentId the normalized shipment ID
     * @return list of LPNs with populated line items
     * @throws SQLException if database operation fails
     */
    private List<Lpn> fetchLpnsWithLineItems(String shipmentId) throws SQLException {
        String lpnSql = "SELECT DISTINCT " +
                "il.LODNUM, il.LODUCC, il.STOLOC, il.LODWGT, " +
                "id.LOTNUM, id.SUP_LOTNUM, id.MANDTE, id.EXPIRE_DTE " +
                "FROM WMSP.PCKWRK_DTL pwd " +
                "INNER JOIN WMSP.INVDTL id ON pwd.DTLNUM = id.DTLNUM " +
                "INNER JOIN WMSP.INVSUB isub ON id.SUBNUM = isub.SUBNUM " +
                "INNER JOIN WMSP.INVLOD il ON isub.LODNUM = il.LODNUM " +
                "WHERE pwd.SHIP_ID = ? " +
                "ORDER BY il.LODNUM";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(lpnSql)) {
            Map<String, List<LineItem>> lineItemsByLpn = fetchLineItemsByLpn(conn, shipmentId);
            LinkedHashMap<String, MutableLpnRow> lpnsById = new LinkedHashMap<>();

            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String normalizedLpnId = NormalizationService.normalizeString(rs.getString("LODNUM"));
                    if (normalizedLpnId.isBlank()) {
                        continue;
                    }
                    MutableLpnRow row = lpnsById.computeIfAbsent(
                            normalizedLpnId,
                            ignored -> new MutableLpnRow(
                                    normalizedLpnId,
                                    NormalizationService.normalizeString(shipmentId),
                                    lineItemsByLpn.getOrDefault(normalizedLpnId, List.of())
                            )
                    );
                    row.merge(rs, this);
                }
            }
            List<Lpn> lpns = new ArrayList<>(lpnsById.size());
            for (MutableLpnRow row : lpnsById.values()) {
                lpns.add(row.toLpn());
            }
            return lpns;
        }
    }

    /**
     * Fetches all line items for a shipment in one query, grouped by pallet (LPN).
     *
     * <p>This avoids one-query-per-LPN access patterns on large multi-pallet shipments.</p>
     *
     * @param conn       active database connection
     * @param shipmentId shipment identifier
     * @return map keyed by normalized LPN ID
     * @throws SQLException if database operation fails
     */
    private Map<String, List<LineItem>> fetchLineItemsByLpn(Connection conn, String shipmentId) throws SQLException {
        Map<String, List<LineItem>> lineItemsByLpn = new HashMap<>();

        try (PreparedStatement stmt = conn.prepareStatement(SHIPMENT_LINE_ITEMS_SQL)) {
            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String lpnId = NormalizationService.normalizeString(rs.getString("LODNUM"));
                    LineItem item = new LineItem(
                            NormalizationService.normalizeString(rs.getString("ORDLIN")),
                            NormalizationService.normalizeString(rs.getString("ORDSLN")),
                            NormalizationService.normalizeSku(rs.getString("PRTNUM")),
                            NormalizationService.normalizeString(rs.getString("SRTDSC")),
                            NormalizationService.normalizeString(rs.getString("CSTPRT")),
                            NormalizationService.normalizeString(rs.getString("ORDNUM")),
                            NormalizationService.normalizeString(rs.getString("CONS_BATCH")),
                            NormalizationService.normalizeString(rs.getString("SALES_ORDNUM")),
                            rs.getInt("EFFECTIVE_QTY"),
                            rs.getInt("UNTPAK"),
                            // WMS shipment-line joins used here do not expose a stable unit-of-measure column;
                            // preserve legacy behavior with explicit EA fallback.
                            DEFAULT_LINE_ITEM_UOM,
                            rs.getDouble("NETWGT"),
                            null, // walmartItemNumber - will be looked up via SkuMappingService
                            null, // gtinBarcode - could be fetched from ALT_PRTMST
                            null  // upcCode - could be fetched from ALT_PRTMST
                    );
                    lineItemsByLpn.computeIfAbsent(lpnId, ignored -> new ArrayList<>()).add(item);
                }
            }
        }

        return lineItemsByLpn;
    }

    /**
     * Helper: Extract the first order number from a shipment's lines.
     * This is used to populate the orderId field.
     *
     * @param conn       active database connection
     * @param shipmentId the shipment ID
     * @return first order number, or null
     * @throws SQLException if database operation fails
     */
    private String extractFirstOrderNumber(Connection conn, String shipmentId) throws SQLException {
        String sql = "SELECT sl.ORDNUM FROM WMSP.SHIPMENT_LINE sl WHERE sl.SHIP_ID = ? AND ROWNUM <= 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return NormalizationService.normalizeString(rs.getString("ORDNUM"));
                }
            }
        }
        return null;
    }

    private String resolveLocationNumber(String destNum,
                                         String vcDestId,
                                         String shipToName,
                                         String adrHostExtId) {
        // Prefer explicit order-level destination first; downstream fallbacks are heuristic.
        String destination = NormalizationService.normalizeString(destNum);
        if (!destination.isBlank()) {
            return destination;
        }

        String vcDestination = NormalizationService.normalizeString(vcDestId);
        if (!vcDestination.isBlank()) {
            return vcDestination;
        }

        String dcFromName = extractDcNumber(shipToName);
        if (dcFromName != null) {
            return dcFromName;
        }

        String addressHost = NormalizationService.normalizeString(adrHostExtId);
        return addressHost.isBlank() ? null : addressHost;
    }

    private String extractDcNumber(String shipToName) {
        if (shipToName == null || shipToName.isBlank()) {
            return null;
        }
        Matcher matcher = DC_NUMBER_PATTERN.matcher(shipToName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime nullableLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDate nullableLocalDate(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    /**
     * Keeps the first meaningful value while allowing later rows to backfill blanks produced by
     * mixed-detail inventory joins.
     */
    private static String preferNonBlank(String currentValue, String candidateValue) {
        return currentValue == null || currentValue.isBlank() ? candidateValue : currentValue;
    }

    private static final class MutableLpnRow {
        private final String lpnId;
        private final String shipmentId;
        private final List<LineItem> lineItems;
        private String sscc;
        private double weight;
        private String stagingLocation;
        private String warehouseLot;
        private String customerLot;
        private LocalDate manufactureDate;
        private LocalDate bestByDate;

        private MutableLpnRow(String lpnId, String shipmentId, List<LineItem> lineItems) {
            this.lpnId = lpnId;
            this.shipmentId = shipmentId;
            this.lineItems = List.copyOf(lineItems);
        }

        private void merge(ResultSet rs, OracleDbQueryRepository repository) throws SQLException {
            sscc = preferNonBlank(sscc, NormalizationService.normalizeBarcode(rs.getString("LODUCC")));
            if (weight == 0.0d) {
                weight = rs.getDouble("LODWGT");
            }
            stagingLocation = preferNonBlank(stagingLocation,
                    NormalizationService.normalizeOptionalStagingLocation(rs.getString("STOLOC")));
            warehouseLot = preferNonBlank(warehouseLot, NormalizationService.normalizeString(rs.getString("LOTNUM")));
            customerLot = preferNonBlank(customerLot, NormalizationService.normalizeString(rs.getString("SUP_LOTNUM")));
            if (manufactureDate == null) {
                manufactureDate = repository.nullableLocalDate(rs, "MANDTE");
            }
            if (bestByDate == null) {
                bestByDate = repository.nullableLocalDate(rs, "EXPIRE_DTE");
            }
        }

        private Lpn toLpn() {
            return new Lpn(
                    lpnId,
                    shipmentId,
                    sscc,
                    0,
                    0,
                    weight,
                    stagingLocation,
                    warehouseLot,
                    customerLot,
                    manufactureDate,
                    bestByDate,
                    lineItems
            );
        }
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

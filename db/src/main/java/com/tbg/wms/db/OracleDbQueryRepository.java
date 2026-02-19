/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.db;

import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.NormalizationService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oracle implementation of DbQueryRepository.
 *
 * This class executes Oracle WMS queries against the WMSP schema to retrieve
 * shipment data with all related information (orders, lines, pallets, lots, etc.).
 *
 * Uses prepared statements to prevent SQL injection and proper resource
 * management with try-with-resources.
 *
 * Database: Oracle WMS at site-specific location (e.g., TBG3002 at 10.19.68.61:1521/WMSP)
 * Schema: WMSP (schema name is prepended to all table references)
 * User: RPTADM (read-only reporting user)
 */
public final class OracleDbQueryRepository implements DbQueryRepository {

    private static final Logger log = LoggerFactory.getLogger(OracleDbQueryRepository.class);
    private static final Pattern DC_NUMBER_PATTERN = Pattern.compile("(?i)\\bDC\\s*#?\\s*(\\d{3,6})\\b");

    private final DataSource dataSource;

    /**
     * Creates a new OracleDbQueryRepository.
     *
     * @param dataSource the connection pool providing database connections
     * @throws IllegalArgumentException if dataSource is null
     */
    public OracleDbQueryRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
    }

    @Override
    public Shipment findShipmentWithLpnsAndLineItems(String shipmentId) {
        Objects.requireNonNull(shipmentId, "shipmentId cannot be null");
        if (shipmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("shipmentId cannot be empty");
        }

        String normalizedId = NormalizationService.normalizeString(shipmentId);
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
        Objects.requireNonNull(shipmentId, "shipmentId cannot be null");
        if (shipmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("shipmentId cannot be empty");
        }

        String normalizedId = NormalizationService.normalizeString(shipmentId);
        String sql = "SELECT COUNT(*) as shipment_count FROM WMSP.SHIPMENT WHERE SHIP_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizedId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("shipment_count");
                    boolean exists = count > 0;
                    log.debug("Shipment {} exists: {}", normalizedId, exists);
                    return exists;
                }
            }
            return false;
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
        Objects.requireNonNull(shipmentId, "shipmentId cannot be null");
        if (shipmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("shipmentId cannot be empty");
        }

        String normalizedId = NormalizationService.normalizeString(shipmentId);
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
        Objects.requireNonNull(shipmentId, "shipmentId cannot be null");
        if (shipmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("shipmentId cannot be empty");
        }

        String normalizedId = NormalizationService.normalizeString(shipmentId);
        List<ShipmentSkuFootprint> rows = new ArrayList<>();

        String sql = "SELECT " +
                "ol.PRTNUM AS PRTNUM, " +
                "MAX(ol.PRT_CLIENT_ID) AS PRT_CLIENT_ID, " +
                "MAX(s.WH_ID) AS WH_ID, " +
                "MAX(ol.CSTPRT) AS ITEM_DESCRIPTION, " +
                "SUM(COALESCE(" +
                "NULLIF(sl.SHPQTY, 0), " +
                "NULLIF(sl.STGQTY, 0), " +
                "NULLIF(sl.PCKQTY, 0), " +
                "NULLIF(sl.INPQTY, 0), " +
                "NULLIF(sl.TOT_PLN_QTY, 0), " +
                "NULLIF(ol.ORDQTY, 0), " +
                "0)) AS TOTAL_UNITS, " +
                "MAX(CASE WHEN d.CAS_FLG = 1 THEN d.UNTQTY END) AS UNITS_PER_CASE, " +
                "MAX(CASE WHEN d.PAL_FLG = 1 THEN d.UNTQTY END) AS UNITS_PER_PALLET, " +
                "MAX(CASE WHEN d.PAL_FLG = 1 THEN d.LEN END) AS PALLET_LEN, " +
                "MAX(CASE WHEN d.PAL_FLG = 1 THEN d.WID END) AS PALLET_WID, " +
                "MAX(CASE WHEN d.PAL_FLG = 1 THEN d.HGT END) AS PALLET_HGT " +
                "FROM WMSP.SHIPMENT_LINE sl " +
                "INNER JOIN WMSP.SHIPMENT s ON s.SHIP_ID = sl.SHIP_ID " +
                "INNER JOIN WMSP.ORD_LINE ol ON sl.ORDNUM = ol.ORDNUM " +
                "  AND sl.ORDLIN = ol.ORDLIN AND sl.ORDSLN = ol.ORDSLN AND sl.CLIENT_ID = ol.CLIENT_ID " +
                "LEFT JOIN WMSP.PRTFTP pf ON pf.PRTNUM = ol.PRTNUM " +
                "  AND pf.PRT_CLIENT_ID = ol.PRT_CLIENT_ID " +
                "  AND pf.WH_ID = s.WH_ID " +
                "  AND pf.DEFFTP_FLG = 1 " +
                "LEFT JOIN WMSP.PRTFTP_DTL d ON d.PRTNUM = pf.PRTNUM " +
                "  AND d.PRT_CLIENT_ID = pf.PRT_CLIENT_ID " +
                "  AND d.WH_ID = pf.WH_ID " +
                "  AND d.FTPCOD = pf.FTPCOD " +
                "WHERE sl.SHIP_ID = ? " +
                "GROUP BY ol.PRTNUM";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            List<String> descriptionColumns = resolvePrtmstDescriptionColumns(conn);
            stmt.setString(1, normalizedId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sku = NormalizationService.normalizeSku(rs.getString("PRTNUM"));
                    String fallbackDescription = NormalizationService.normalizeString(rs.getString("ITEM_DESCRIPTION"));
                    String itemDescription = resolveItemDescription(
                            conn,
                            sku,
                            NormalizationService.normalizeString(rs.getString("PRT_CLIENT_ID")),
                            NormalizationService.normalizeString(rs.getString("WH_ID")),
                            fallbackDescription,
                            descriptionColumns
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
    public void close() {
        // DataSource is managed by the connection pool
        // No explicit close needed here; connection pool handles lifecycle
        log.debug("OracleDbQueryRepository closed");
    }

    /**
     * Fetches the shipment header information with all related address, order, and carrier details.
     *
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
                    LocalDateTime shipDate = rs.getTimestamp("EARLY_SHPDTE") != null ?
                            rs.getTimestamp("EARLY_SHPDTE").toLocalDateTime() : null;
                    LocalDateTime deliveryDate = rs.getTimestamp("LATE_DLVDTE") != null ?
                            rs.getTimestamp("LATE_DLVDTE").toLocalDateTime() : null;
                    LocalDateTime createdDate = rs.getTimestamp("ADDDTE") != null ?
                            rs.getTimestamp("ADDDTE").toLocalDateTime() : LocalDateTime.now();

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
     *
     * Uses the PCKWRK_DTL → INVDTL → INVSUB → INVLOD chain to get complete pallet data
     * including lot tracking and dates.
     *
     * @param shipmentId the normalized shipment ID
     * @return list of LPNs with populated line items
     * @throws SQLException if database operation fails
     */
    private List<Lpn> fetchLpnsWithLineItems(String shipmentId) throws SQLException {
        List<Lpn> lpns = new ArrayList<>();

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

            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String lpnId = rs.getString("LODNUM");
                    List<LineItem> lineItems = fetchLineItemsForLpn(conn, shipmentId, lpnId);

                    LocalDate mfgDate = rs.getDate("MANDTE") != null ?
                            rs.getDate("MANDTE").toLocalDate() : null;
                    LocalDate expiryDate = rs.getDate("EXPIRE_DTE") != null ?
                            rs.getDate("EXPIRE_DTE").toLocalDate() : null;

                    Lpn lpn = new Lpn(
                            NormalizationService.normalizeString(lpnId),
                            NormalizationService.normalizeString(shipmentId),
                            NormalizationService.normalizeBarcode(rs.getString("LODUCC")),
                            0, // case count - will be calculated from line items
                            0, // unit count - will be calculated from line items
                            rs.getDouble("LODWGT"),
                            NormalizationService.normalizeOptionalStagingLocation(rs.getString("STOLOC")),
                            NormalizationService.normalizeString(rs.getString("LOTNUM")),
                            NormalizationService.normalizeString(rs.getString("SUP_LOTNUM")),
                            mfgDate,
                            expiryDate,
                            lineItems
                    );
                    lpns.add(lpn);
                }
            }
        }

        return lpns;
    }

    /**
     * Fetches all line items for a specific pallet (LPN) in a shipment.
     *
     * Retrieves detailed product, order, and SKU information for each line.
     *
     * @param conn active database connection
     * @param shipmentId the shipment ID
     * @param lpnId the LPN ID
     * @return list of line items for the LPN
     * @throws SQLException if database operation fails
     */
    private List<LineItem> fetchLineItemsForLpn(Connection conn, String shipmentId, String lpnId) throws SQLException {
        List<LineItem> lineItems = new ArrayList<>();

        String itemSql = "SELECT " +
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
                "WHERE pwd.SHIP_ID = ? AND pwd.SHIP_CTNNUM = ? " +
                "ORDER BY sl.ORDLIN, sl.ORDSLN";

        try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
            stmt.setString(1, shipmentId);
            stmt.setString(2, lpnId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
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
                            "EA", // unit of measure - TODO: get from database if available
                            rs.getDouble("NETWGT"),
                            null, // walmartItemNumber - will be looked up via SkuMappingService
                            null, // gtinBarcode - could be fetched from ALT_PRTMST
                            null  // upcCode - could be fetched from ALT_PRTMST
                    );
                    lineItems.add(item);
                }
            }
        }

        return lineItems;
    }

    /**
     * Helper: Extract the first order number from a shipment's lines.
     * This is used to populate the orderId field.
     *
     * @param conn active database connection
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

    private List<String> resolvePrtmstDescriptionColumns(Connection conn) {
        final String sql = "SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = 'WMSP' AND TABLE_NAME = 'PRTMST'";
        Set<String> columns = new LinkedHashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                if (column != null) {
                    columns.add(column.toUpperCase());
                }
            }
        } catch (SQLException e) {
            log.warn("Could not inspect PRTMST columns via ALL_TAB_COLUMNS: {}", e.getMessage());
            return List.of();
        }

        List<String> preferredOrder = List.of("SHORT_DSC", "LNGDSC", "PRT_DISP", "PRT_DISPTN");
        List<String> available = new ArrayList<>();
        for (String candidate : preferredOrder) {
            if (columns.contains(candidate)) {
                available.add(candidate);
            }
        }
        if (!available.isEmpty()) {
            return available;
        }

        // Fallback for restricted dictionary visibility: probe columns directly.
        for (String candidate : preferredOrder) {
            if (canSelectPrtmstColumn(conn, candidate)) {
                available.add(candidate);
            }
        }
        return available;
    }

    private boolean canSelectPrtmstColumn(Connection conn, String column) {
        String sql = "SELECT " + column + " FROM WMSP.PRTMST WHERE ROWNUM = 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() || !rs.isBeforeFirst();
        } catch (SQLException e) {
            return false;
        }
    }

    private String resolveItemDescription(Connection conn,
                                          String sku,
                                          String prtClientId,
                                          String whId,
                                          String fallbackDescription,
                                          List<String> descriptionColumns) {
        String prtdscDescription = fetchDescriptionFromPrtdsc(conn, sku, prtClientId, whId);
        if (isHumanReadableDescription(prtdscDescription)) {
            return prtdscDescription;
        }

        String prtmstDescription = fetchDescriptionFromPrtmst(conn, sku, prtClientId, descriptionColumns);
        if (isHumanReadableDescription(prtmstDescription)) {
            return prtmstDescription;
        }
        if (isHumanReadableDescription(fallbackDescription)) {
            return fallbackDescription;
        }
        return null;
    }

    private String fetchDescriptionFromPrtdsc(Connection conn,
                                              String sku,
                                              String prtClientId,
                                              String whId) {
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

        for (String skuCandidate : buildSkuCandidates(sku)) {
            for (String clientCandidate : clientCandidates) {
                for (String whCandidate : whCandidates) {
                    String colVal = skuCandidate + "|" + clientCandidate + "|" + whCandidate;
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, colVal);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String shortDsc = NormalizationService.normalizeString(rs.getString("SHORT_DSC"));
                                if (isHumanReadableDescription(shortDsc)) {
                                    return shortDsc;
                                }
                                String longDsc = NormalizationService.normalizeString(rs.getString("LNGDSC"));
                                if (isHumanReadableDescription(longDsc)) {
                                    return longDsc;
                                }
                            }
                        }
                    } catch (SQLException e) {
                        log.debug("Could not query PRTDSC for SKU {}: {}", sku, e.getMessage());
                    }
                }
            }
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
        for (String skuCandidate : buildSkuCandidates(sku)) {
            String sql;
            if (prtClientId == null || prtClientId.isBlank()) {
                sql = "SELECT " + selectCols + " FROM WMSP.PRTMST WHERE PRTNUM = ? FETCH FIRST 3 ROWS ONLY";
            } else {
                sql = "SELECT " + selectCols + " FROM WMSP.PRTMST WHERE PRTNUM = ? AND PRT_CLIENT_ID = ? FETCH FIRST 3 ROWS ONLY";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, skuCandidate);
                if (prtClientId != null && !prtClientId.isBlank()) {
                    stmt.setString(2, prtClientId);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        for (String column : descriptionColumns) {
                            String value = NormalizationService.normalizeString(rs.getString(column));
                            if (isHumanReadableDescription(value)) {
                                return value;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                log.debug("Could not resolve PRTMST description for SKU {} candidate {}: {}", sku, skuCandidate, e.getMessage());
            }
        }
        return null;
    }

    private List<String> buildSkuCandidates(String sku) {
        List<String> candidates = new ArrayList<>();
        if (sku == null || sku.isBlank()) {
            return candidates;
        }
        candidates.add(sku);

        if (sku.startsWith("100") && sku.length() > 3) {
            candidates.add(sku.substring(3));
        }

        String noLeadingZeros = sku.replaceFirst("^0+(?!$)", "");
        if (!noLeadingZeros.equals(sku)) {
            candidates.add(noLeadingZeros);
        }

        return candidates;
    }

    private boolean isHumanReadableDescription(String value) {
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

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}

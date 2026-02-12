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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
                    String normalized = NormalizationService.normalizeStagingLocation(location);
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
                "a.ADRCTY, a.ADRSTC, a.ADRPSZ, a.CTRY_NAME, a.PHNNUM, a.ATTN_NAME, " +
                "o.CPONUM, o.DEST_NUM, o.DEPTNO, " +
                "st.STOP_SEQ, " +
                "cm.DOC_NUM as CARRIER_BOL, cm.TRACK_NUM as CARRIER_PRO " +
                "FROM WMSP.SHIPMENT s " +
                "INNER JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID " +
                "LEFT JOIN WMSP.ORD o ON EXISTS (SELECT 1 FROM WMSP.SHIPMENT_LINE sl WHERE sl.SHIP_ID = s.SHIP_ID AND sl.ORDNUM = o.ORDNUM AND sl.CLIENT_ID = o.CLIENT_ID) " +
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
                            NormalizationService.normalizeStagingLocation(rs.getString("DSTLOC")),
                            NormalizationService.normalizeString(rs.getString("CPONUM")),
                            NormalizationService.normalizeString(rs.getString("DEST_NUM")),
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
                            NormalizationService.normalizeStagingLocation(rs.getString("STOLOC")),
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
                "sl.SHPQTY, " +
                "ol.PRTNUM, ol.CSTPRT, ol.ORDQTY, ol.SALES_ORDNUM, ol.UNTPAK, " +
                "p.LNGDSC, p.SRTDSC, p.NETWGT " +
                "FROM WMSP.PCKWRK_DTL pwd " +
                "INNER JOIN WMSP.SHIPMENT_LINE sl ON pwd.SHIP_LINE_ID = sl.SHIP_LINE_ID " +
                "INNER JOIN WMSP.ORD_LINE ol ON sl.ORDNUM = ol.ORDNUM " +
                "  AND sl.ORDLIN = ol.ORDLIN AND sl.ORDSLN = ol.ORDSLN AND sl.CLIENT_ID = ol.CLIENT_ID " +
                "LEFT JOIN WMSP.PRTMST p ON ol.PRTNUM = p.PRTNUM AND ol.PRT_CLIENT_ID = p.PRT_CLIENT_ID " +
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
                            rs.getInt("SHPQTY"),
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
}


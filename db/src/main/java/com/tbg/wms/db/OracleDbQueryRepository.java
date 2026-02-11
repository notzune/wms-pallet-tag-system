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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Oracle implementation of DbQueryRepository.
 *
 * This class executes actual Oracle WMS queries to retrieve shipment data.
 * Uses prepared statements to prevent SQL injection and proper resource
 * management with try-with-resources.
 *
 * Note: Actual table/column names are placeholders pending schema mapping.
 * Update SQL queries once WMS schema is confirmed.
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

        try {
            // Step 1: Fetch shipment header
            Shipment shipment = fetchShipmentHeader(normalizedId);
            if (shipment == null) {
                log.warn("Shipment not found: {}", normalizedId);
                return null;
            }

            // Step 2: Fetch all LPNs for this shipment with their line items
            List<Lpn> lpns = fetchLpnsWithLineItems(normalizedId);

            // Step 3: Create new shipment with loaded LPNs
            return new Shipment(
                    shipment.getShipmentId(),
                    shipment.getOrderId(),
                    shipment.getShipToName(),
                    shipment.getShipToAddress(),
                    shipment.getShipToCity(),
                    shipment.getShipToState(),
                    shipment.getShipToZip(),
                    shipment.getCarrierCode(),
                    shipment.getServiceCode(),
                    shipment.getCreatedDate(),
                    lpns
            );
        } catch (SQLException e) {
            log.error("Database error retrieving shipment {}: {}", normalizedId, e.getSQLState());
            throw new WmsDbConnectivityException(
                    "Failed to retrieve shipment: " + e.getMessage(),
                    e,
                    "Check database connectivity and verify shipment ID exists in WMS"
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
        String sql = "SELECT COUNT(*) as lpn_count FROM wms_lpns WHERE shipment_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizedId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int lpnCount = rs.getInt("lpn_count");
                    return lpnCount > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            log.error("Database error checking shipment existence for {}: {}", normalizedId, e.getSQLState());
            throw new WmsDbConnectivityException(
                    "Failed to check shipment existence: " + e.getMessage(),
                    e,
                    "Verify database connection and shipment ID format"
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
        String sql = "SELECT DISTINCT staging_location FROM wms_lpns WHERE shipment_id = ? FETCH FIRST 1 ROW ONLY";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, normalizedId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String location = rs.getString("staging_location");
                    return NormalizationService.normalizeStagingLocation(location);
                }
            }
            return null;
        } catch (SQLException e) {
            log.error("Database error retrieving staging location for {}: {}", normalizedId, e.getSQLState());
            throw new WmsDbConnectivityException(
                    "Failed to retrieve staging location: " + e.getMessage(),
                    e,
                    "Verify database connection and shipment exists"
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
     * Fetches the shipment header information.
     *
     * @param shipmentId the normalized shipment ID
     * @return Shipment with header data only (no LPNs), or null if not found
     * @throws SQLException if database operation fails
     */
    private Shipment fetchShipmentHeader(String shipmentId) throws SQLException {
        String sql = "SELECT " +
                "shipment_id, order_id, ship_to_name, ship_to_address, " +
                "ship_to_city, ship_to_state, ship_to_zip, " +
                "carrier_code, service_code, created_date " +
                "FROM wms_shipments WHERE shipment_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime createdDate = rs.getTimestamp("created_date") != null ?
                            rs.getTimestamp("created_date").toLocalDateTime() :
                            LocalDateTime.now();

                    return new Shipment(
                            NormalizationService.normalizeString(rs.getString("shipment_id")),
                            NormalizationService.normalizeString(rs.getString("order_id")),
                            NormalizationService.normalizeString(rs.getString("ship_to_name")),
                            NormalizationService.normalizeString(rs.getString("ship_to_address")),
                            NormalizationService.normalizeString(rs.getString("ship_to_city")),
                            NormalizationService.normalizeToUppercase(rs.getString("ship_to_state")),
                            NormalizationService.normalizeString(rs.getString("ship_to_zip")),
                            NormalizationService.normalizeCarrierCode(rs.getString("carrier_code")),
                            NormalizationService.normalizeToUppercase(rs.getString("service_code")),
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
     * @param shipmentId the normalized shipment ID
     * @return list of LPNs with populated line items
     * @throws SQLException if database operation fails
     */
    private List<Lpn> fetchLpnsWithLineItems(String shipmentId) throws SQLException {
        List<Lpn> lpns = new ArrayList<>();

        String lpnSql = "SELECT " +
                "lpn_id, shipment_id, sscc, case_count, unit_count, weight, staging_location " +
                "FROM wms_lpns WHERE shipment_id = ? ORDER BY lpn_id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(lpnSql)) {

            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String lpnId = rs.getString("lpn_id");
                    List<LineItem> lineItems = fetchLineItemsForLpn(conn, lpnId);

                    Lpn lpn = new Lpn(
                            NormalizationService.normalizeString(lpnId),
                            NormalizationService.normalizeString(rs.getString("shipment_id")),
                            NormalizationService.normalizeBarcode(rs.getString("sscc")),
                            NormalizationService.normalizeToInt(rs.getString("case_count"), 0),
                            NormalizationService.normalizeToInt(rs.getString("unit_count"), 0),
                            NormalizationService.normalizeToDouble(rs.getString("weight"), 0.0),
                            NormalizationService.normalizeStagingLocation(rs.getString("staging_location")),
                            lineItems
                    );
                    lpns.add(lpn);
                }
            }
        }

        return lpns;
    }

    /**
     * Fetches all line items for a specific LPN.
     *
     * @param conn active database connection
     * @param lpnId the LPN ID
     * @return list of line items for the LPN
     * @throws SQLException if database operation fails
     */
    private List<LineItem> fetchLineItemsForLpn(Connection conn, String lpnId) throws SQLException {
        List<LineItem> lineItems = new ArrayList<>();

        String itemSql = "SELECT " +
                "line_number, sku, description, quantity, weight, uom " +
                "FROM wms_line_items WHERE lpn_id = ? ORDER BY line_number";

        try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
            stmt.setString(1, lpnId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LineItem item = new LineItem(
                            NormalizationService.normalizeString(rs.getString("line_number")),
                            NormalizationService.normalizeSku(rs.getString("sku")),
                            NormalizationService.normalizeString(rs.getString("description")),
                            NormalizationService.normalizeToInt(rs.getString("quantity"), 0),
                            NormalizationService.normalizeToDouble(rs.getString("weight"), 0.0),
                            NormalizationService.normalizeToUppercase(rs.getString("uom"))
                    );
                    lineItems.add(item);
                }
            }
        }

        return lineItems;
    }
}


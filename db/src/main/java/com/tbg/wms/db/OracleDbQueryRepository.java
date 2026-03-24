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
    private final OracleRailQuerySupport railQuerySupport;

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
        this.railQuerySupport = new OracleRailQuerySupport(dataSource);
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
        return railQuerySupport.findRailStopsByTrainId(normalizedTrainId);
    }

    @Override
    public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
        return railQuerySupport.findRailFootprintsByShortCode(shortCodes);
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
}

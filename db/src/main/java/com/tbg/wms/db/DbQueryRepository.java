/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.db;

import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.model.Shipment;

import java.util.List;

/**
 * Repository interface for querying WMS data from Oracle database.
 *
 * This interface provides clean boundaries for database access and allows
 * for different implementations (real Oracle queries, test mocks, etc.).
 *
 * All queries throw WmsDbException subtypes for proper error handling.
 */
public interface DbQueryRepository {

    /**
     * Retrieves a complete shipment with all LPNs and line items.
     *
     * This is a read-only operation that gathers:
     * - Shipment header information (destination, carrier, dates)
     * - All LPNs (pallets) tied to this shipment
     * - All line items within each LPN
     * - Staging location for routing decisions
     *
     * @param shipmentId the shipment identifier
     * @return the complete shipment with all associated data
     * @throws com.tbg.wms.core.exception.WmsDbConnectivityException if database connection fails
     * @throws com.tbg.wms.core.exception.WmsDbException if query execution fails
     * @throws IllegalArgumentException if shipmentId is null or empty
     */
    Shipment findShipmentWithLpnsAndLineItems(String shipmentId);

    /**
     * Validates that a shipment exists and has at least one LPN.
     *
     * Lightweight check to verify shipment is printable without
     * fetching all associated data.
     *
     * @param shipmentId the shipment identifier
     * @return true if shipment exists and has at least one LPN, false otherwise
     * @throws com.tbg.wms.core.exception.WmsDbConnectivityException if database connection fails
     * @throws com.tbg.wms.core.exception.WmsDbException if query execution fails
     * @throws IllegalArgumentException if shipmentId is null or empty
     */
    boolean shipmentExists(String shipmentId);

    /**
     * Returns the staging location for a shipment.
     *
     * Used for printer routing decisions. All LPNs in a shipment
     * should be in the same staging location.
     *
     * @param shipmentId the shipment identifier
     * @return the staging location (e.g., "ROSSI", "OFFICE"), or null if not found
     * @throws com.tbg.wms.core.exception.WmsDbConnectivityException if database connection fails
     * @throws com.tbg.wms.core.exception.WmsDbException if query execution fails
     * @throws IllegalArgumentException if shipmentId is null or empty
     */
    String getStagingLocation(String shipmentId);

    /**
     * Retrieves shipment-level SKU totals with footprint maintenance data.
     *
     * <p>Data is sourced from shipment lines plus PRTFTP / PRTFTP_DTL to provide
     * units-per-case, units-per-pallet, and pallet dimensions for planning.</p>
     *
     * @param shipmentId the shipment identifier
     * @return one row per SKU in the shipment
     */
    List<ShipmentSkuFootprint> findShipmentSkuFootprints(String shipmentId);

    /**
     * Closes the repository and releases any resources.
     *
     * Implementors must ensure underlying connections are closed properly.
     */
    void close();
}


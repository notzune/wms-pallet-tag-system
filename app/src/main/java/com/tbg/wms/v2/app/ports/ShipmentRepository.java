package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;

/**
 * Loads prepared shipment label data for application use cases.
 */
public interface ShipmentRepository {
    /**
     * Finds prepared shipment labels by shipment identifier.
     *
     * @param shipmentId the shipment id.
     * @return the matching value, when present.
     */
    PreparedShipmentLabels findByShipmentId(String shipmentId);
}

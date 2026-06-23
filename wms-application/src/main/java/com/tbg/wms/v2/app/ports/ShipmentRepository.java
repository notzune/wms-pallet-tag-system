package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;

/**
 * Loads prepared shipment label data for application use cases.
 */
public interface ShipmentRepository {
    PreparedShipmentLabels findByShipmentId(String shipmentId);
}

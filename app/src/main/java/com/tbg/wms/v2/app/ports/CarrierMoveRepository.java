package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;

/**
 * Loads prepared carrier-move label data for application use cases.
 */
public interface CarrierMoveRepository {
    /**
     * Finds prepared carrier-move labels by carrier-move identifier.
     *
     * @param carrierMoveId the carrier move id.
     * @return the matching value, when present.
     */
    CarrierMoveLabels findByCarrierMoveId(String carrierMoveId);
}

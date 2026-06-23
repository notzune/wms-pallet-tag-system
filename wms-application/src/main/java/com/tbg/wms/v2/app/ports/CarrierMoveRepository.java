package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;

/**
 * Loads prepared carrier-move label data for application use cases.
 */
public interface CarrierMoveRepository {
    CarrierMoveLabels findByCarrierMoveId(String carrierMoveId);
}

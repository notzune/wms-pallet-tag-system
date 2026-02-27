/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.3.0
 */

package com.tbg.wms.core.model;

import java.time.LocalDateTime;

/**
 * Lightweight mapping row from carrier move to shipment stop.
 */
public final class CarrierMoveStopRef {

    private final String carrierMoveId;
    private final String stopId;
    private final Integer stopSequence;
    private final Integer tmsStopSequence;
    private final String shipmentId;
    private final String shipmentStatus;
    private final LocalDateTime shipmentCreatedDate;

    public CarrierMoveStopRef(String carrierMoveId,
                              String stopId,
                              Integer stopSequence,
                              Integer tmsStopSequence,
                              String shipmentId,
                              String shipmentStatus,
                              LocalDateTime shipmentCreatedDate) {
        this.carrierMoveId = carrierMoveId;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
        this.tmsStopSequence = tmsStopSequence;
        this.shipmentId = shipmentId;
        this.shipmentStatus = shipmentStatus;
        this.shipmentCreatedDate = shipmentCreatedDate;
    }

    public String getCarrierMoveId() {
        return carrierMoveId;
    }

    public String getStopId() {
        return stopId;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public Integer getTmsStopSequence() {
        return tmsStopSequence;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public LocalDateTime getShipmentCreatedDate() {
        return shipmentCreatedDate;
    }
}

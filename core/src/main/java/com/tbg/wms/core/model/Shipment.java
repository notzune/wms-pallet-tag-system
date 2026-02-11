/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a shipment containing multiple pallets (LPNs).
 *
 * A shipment is the top-level container for shipping operations, including
 * destination information, carrier details, and a list of pallets to be shipped.
 */
public final class Shipment {

    private final String shipmentId;
    private final String orderId;
    private final String shipToName;
    private final String shipToAddress;
    private final String shipToCity;
    private final String shipToState;
    private final String shipToZip;
    private final String carrierCode;
    private final String serviceCode;
    private final LocalDateTime createdDate;
    private final List<Lpn> lpns;

    /**
     * Creates a new Shipment.
     *
     * @param shipmentId unique shipment identifier
     * @param orderId the order identifier
     * @param shipToName destination company/person name
     * @param shipToAddress street address
     * @param shipToCity city
     * @param shipToState state/province
     * @param shipToZip postal code
     * @param carrierCode carrier code (e.g., "UPS", "FDX")
     * @param serviceCode service code (e.g., "GND", "OVR")
     * @param createdDate shipment creation date and time
     * @param lpns list of pallets in this shipment
     */
    public Shipment(String shipmentId, String orderId, String shipToName, String shipToAddress,
                    String shipToCity, String shipToState, String shipToZip,
                    String carrierCode, String serviceCode, LocalDateTime createdDate, List<Lpn> lpns) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.shipToName = shipToName;
        this.shipToAddress = shipToAddress;
        this.shipToCity = shipToCity;
        this.shipToState = shipToState;
        this.shipToZip = shipToZip;
        this.carrierCode = carrierCode;
        this.serviceCode = serviceCode;
        this.createdDate = createdDate;
        this.lpns = lpns != null ? new ArrayList<>(lpns) : new ArrayList<>();
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getShipToName() {
        return shipToName;
    }

    public String getShipToAddress() {
        return shipToAddress;
    }

    public String getShipToCity() {
        return shipToCity;
    }

    public String getShipToState() {
        return shipToState;
    }

    public String getShipToZip() {
        return shipToZip;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public List<Lpn> getLpns() {
        return Collections.unmodifiableList(lpns);
    }

    public int getLpnCount() {
        return lpns.size();
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "shipmentId='" + shipmentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", shipToName='" + shipToName + '\'' +
                ", carrierCode='" + carrierCode + '\'' +
                ", serviceCode='" + serviceCode + '\'' +
                ", lpnCount=" + lpns.size() +
                '}';
    }
}


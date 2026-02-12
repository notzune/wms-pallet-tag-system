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

    // Core identifiers
    private final String shipmentId;           // SHIPMENT.SHIP_ID
    private final String externalId;          // SHIPMENT.HOST_EXT_ID (ERP reference)
    private final String orderId;             // SHIPMENT_LINE.ORDNUM (may differ from shipmentId)
    private final String warehouseId;         // SHIPMENT.WH_ID (e.g., "3002" for TBG3002)

    // Ship-to address (from ADRMST table)
    private final String shipToName;          // ADRMST.ADRNAM
    private final String shipToAddress1;      // ADRMST.ADRLN1 (was: shipToAddress)
    private final String shipToAddress2;      // ADRMST.ADRLN2
    private final String shipToAddress3;      // ADRMST.ADRLN3
    private final String shipToCity;          // ADRMST.ADRCTY
    private final String shipToState;         // ADRMST.ADRSTC (Province for Canada)
    private final String shipToZip;           // ADRMST.ADRPSZ
    private final String shipToCountry;       // ADRMST.CTRY_NAME (e.g., "CAN", "USA")
    private final String shipToPhone;         // ADRMST.PHNNUM

    // Carrier and shipping details
    private final String carrierCode;         // SHIPMENT.CARCOD (SCAC: "MDLE", "CB", "CPU", etc.)
    private final String serviceLevel;        // SHIPMENT.SRVLVL (was: serviceCode; e.g., "TL", "IM")
    private final String documentNumber;      // SHIPMENT.DOC_NUM (BOL/shipping document number)
    private final String trackingNumber;      // SHIPMENT.TRACK_NUM (shipment-level tracking)
    private final String destinationLocation; // SHIPMENT.DSTLOC (staging zone, e.g., "ROSSI")

    // Order-level details (from ORD table)
    private final String customerPo;          // ORD.CPONUM (Customer PO Number - different from orderNum!)
    private final String locationNumber;      // ORD.DEST_NUM (Walmart DC/Store location, e.g., "6080")
    private final String departmentNumber;    // ORD.DEPTNO (Walmart department number)

    // Stop and carrier move details
    private final String stopId;              // SHIPMENT.STOP_ID (FK → STOP.STOP_ID)
    private final Integer stopSequence;       // STOP.STOP_SEQ (stop number in route)
    private final String carrierMoveId;       // SHIPMENT.TMS_MOVE_ID (FK → CAR_MOVE.CAR_MOVE_ID)
    private final String proNumber;           // CAR_MOVE.TRACK_NUM (Carrier PRO number)
    private final String bolNumber;           // CAR_MOVE.DOC_NUM (BOL at move level)

    // Status and dates
    private final String status;              // SHIPMENT.SHPSTS (e.g., "C"=Complete, "R"=Ready/Released)
    private final LocalDateTime shipDate;     // SHIPMENT.EARLY_SHPDTE
    private final LocalDateTime deliveryDate; // SHIPMENT.LATE_DLVDTE
    private final LocalDateTime createdDate;  // SHIPMENT.ADDDTE

    // Pallets in this shipment
    private final List<Lpn> lpns;

    /**
     * Creates a new Shipment with comprehensive WMS database fields.
     *
     * @param shipmentId unique shipment identifier (SHIP_ID)
     * @param externalId external ERP reference (HOST_EXT_ID)
     * @param orderId order identifier (may differ from shipmentId)
     * @param warehouseId warehouse code (e.g., "3002")
     * @param shipToName ship-to customer name
     * @param shipToAddress1 primary address line
     * @param shipToAddress2 secondary address line (optional)
     * @param shipToAddress3 tertiary address line (optional)
     * @param shipToCity destination city
     * @param shipToState state or province (e.g., "ON" for Ontario)
     * @param shipToZip postal code
     * @param shipToCountry country name (e.g., "CAN")
     * @param shipToPhone phone number
     * @param carrierCode carrier SCAC code (e.g., "MDLE")
     * @param serviceLevel service level (e.g., "TL", "IM")
     * @param documentNumber BOL or shipping document number
     * @param trackingNumber tracking number
     * @param destinationLocation staging location (e.g., "ROSSI")
     * @param customerPo customer PO number (from ORD table)
     * @param locationNumber Walmart DC/store location code
     * @param departmentNumber Walmart department number
     * @param stopId stop identifier
     * @param stopSequence stop sequence number in route
     * @param carrierMoveId carrier move ID (TMS reference)
     * @param proNumber carrier PRO number
     * @param bolNumber BOL at move level
     * @param status shipment status (C=Complete, R=Ready, etc.)
     * @param shipDate expected ship date
     * @param deliveryDate expected delivery date
     * @param createdDate shipment creation date
     * @param lpns list of pallets/LPNs in this shipment
     */
    public Shipment(
            String shipmentId, String externalId, String orderId, String warehouseId,
            String shipToName, String shipToAddress1, String shipToAddress2, String shipToAddress3,
            String shipToCity, String shipToState, String shipToZip, String shipToCountry, String shipToPhone,
            String carrierCode, String serviceLevel, String documentNumber, String trackingNumber,
            String destinationLocation, String customerPo, String locationNumber, String departmentNumber,
            String stopId, Integer stopSequence, String carrierMoveId, String proNumber, String bolNumber,
            String status, LocalDateTime shipDate, LocalDateTime deliveryDate, LocalDateTime createdDate,
            List<Lpn> lpns) {

        this.shipmentId = shipmentId;
        this.externalId = externalId;
        this.orderId = orderId;
        this.warehouseId = warehouseId;
        this.shipToName = shipToName;
        this.shipToAddress1 = shipToAddress1;
        this.shipToAddress2 = shipToAddress2;
        this.shipToAddress3 = shipToAddress3;
        this.shipToCity = shipToCity;
        this.shipToState = shipToState;
        this.shipToZip = shipToZip;
        this.shipToCountry = shipToCountry;
        this.shipToPhone = shipToPhone;
        this.carrierCode = carrierCode;
        this.serviceLevel = serviceLevel;
        this.documentNumber = documentNumber;
        this.trackingNumber = trackingNumber;
        this.destinationLocation = destinationLocation;
        this.customerPo = customerPo;
        this.locationNumber = locationNumber;
        this.departmentNumber = departmentNumber;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
        this.carrierMoveId = carrierMoveId;
        this.proNumber = proNumber;
        this.bolNumber = bolNumber;
        this.status = status;
        this.shipDate = shipDate;
        this.deliveryDate = deliveryDate;
        this.createdDate = createdDate;
        this.lpns = lpns != null ? new ArrayList<>(lpns) : new ArrayList<>();
    }

    // Getters for all fields

    public String getShipmentId() {
        return shipmentId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getShipToName() {
        return shipToName;
    }

    public String getShipToAddress1() {
        return shipToAddress1;
    }

    public String getShipToAddress2() {
        return shipToAddress2;
    }

    public String getShipToAddress3() {
        return shipToAddress3;
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

    public String getShipToCountry() {
        return shipToCountry;
    }

    public String getShipToPhone() {
        return shipToPhone;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public String getServiceLevel() {
        return serviceLevel;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public String getCustomerPo() {
        return customerPo;
    }

    public String getLocationNumber() {
        return locationNumber;
    }

    public String getDepartmentNumber() {
        return departmentNumber;
    }

    public String getStopId() {
        return stopId;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public String getCarrierMoveId() {
        return carrierMoveId;
    }

    public String getProNumber() {
        return proNumber;
    }

    public String getBolNumber() {
        return bolNumber;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getShipDate() {
        return shipDate;
    }

    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
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
                ", externalId='" + externalId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", warehouseId='" + warehouseId + '\'' +
                ", shipToName='" + shipToName + '\'' +
                ", shipToCity='" + shipToCity + '\'' +
                ", shipToCountry='" + shipToCountry + '\'' +
                ", carrierCode='" + carrierCode + '\'' +
                ", serviceLevel='" + serviceLevel + '\'' +
                ", status='" + status + '\'' +
                ", destinationLocation='" + destinationLocation + '\'' +
                ", lpnCount=" + lpns.size() +
                '}';
    }
}


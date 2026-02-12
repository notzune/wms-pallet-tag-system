/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a license plate number (LPN) or pallet in a shipment.
 *
 * Each LPN contains shipment details, line items, and metadata needed for
 * label generation including barcode information, staging location, and lot tracking.
 */
public final class Lpn {

    private final String lpnId;                // INVLOD.LODNUM
    private final String shipmentId;           // FK to SHIPMENT
    private final String sscc;                 // INVLOD.LODUCC (SSCC-18 barcode)
    private final int caseCount;               // Packed case count
    private final int unitCount;               // Total units
    private final double weight;               // INVLOD.LODWGT
    private final String stagingLocation;      // INVLOD.STOLOC (e.g., "ROSSI" for Canada)

    // Lot tracking fields (from INVDTL table via PCKWRK_DTL chain)
    private final String warehouseLot;         // INVDTL.LOTNUM (internal warehouse lot)
    private final String customerLot;          // INVDTL.SUP_LOTNUM (supplier/customer lot)
    private final LocalDate manufactureDate;   // INVDTL.MANDTE (production date)
    private final LocalDate bestByDate;        // INVDTL.EXPIRE_DTE (expiration/best-by date)

    private final List<LineItem> lineItems;

    /**
     * Creates a new Lpn with comprehensive WMS database fields.
     *
     * @param lpnId the license plate number identifier (LODNUM)
     * @param shipmentId the parent shipment identifier
     * @param sscc the SSCC (Serial Shipping Container Code) barcode value (LODUCC)
     * @param caseCount number of cases in this pallet
     * @param unitCount total number of units
     * @param weight total weight of the pallet (LODWGT)
     * @param stagingLocation the staging location (STOLOC, e.g., "ROSSI" for Canada)
     * @param warehouseLot internal warehouse lot number (INVDTL.LOTNUM)
     * @param customerLot supplier/customer lot number (INVDTL.SUP_LOTNUM)
     * @param manufactureDate production/manufacturing date (INVDTL.MANDTE)
     * @param bestByDate best-by/expiration date (INVDTL.EXPIRE_DTE)
     * @param lineItems list of line items contained in this pallet
     */
    public Lpn(String lpnId, String shipmentId, String sscc, int caseCount, int unitCount,
               double weight, String stagingLocation, String warehouseLot, String customerLot,
               LocalDate manufactureDate, LocalDate bestByDate, List<LineItem> lineItems) {
        this.lpnId = lpnId;
        this.shipmentId = shipmentId;
        this.sscc = sscc;
        this.caseCount = caseCount;
        this.unitCount = unitCount;
        this.weight = weight;
        this.stagingLocation = stagingLocation;
        this.warehouseLot = warehouseLot;
        this.customerLot = customerLot;
        this.manufactureDate = manufactureDate;
        this.bestByDate = bestByDate;
        this.lineItems = lineItems != null ? new ArrayList<>(lineItems) : new ArrayList<>();
    }

    public String getLpnId() {
        return lpnId;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getSscc() {
        return sscc;
    }

    public int getCaseCount() {
        return caseCount;
    }

    public int getUnitCount() {
        return unitCount;
    }

    public double getWeight() {
        return weight;
    }

    public String getStagingLocation() {
        return stagingLocation;
    }

    public String getWarehouseLot() {
        return warehouseLot;
    }

    public String getCustomerLot() {
        return customerLot;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public LocalDate getBestByDate() {
        return bestByDate;
    }

    public List<LineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    @Override
    public String toString() {
        return "Lpn{" +
                "lpnId='" + lpnId + '\'' +
                ", shipmentId='" + shipmentId + '\'' +
                ", sscc='" + sscc + '\'' +
                ", caseCount=" + caseCount +
                ", unitCount=" + unitCount +
                ", weight=" + weight +
                ", stagingLocation='" + stagingLocation + '\'' +
                ", warehouseLot='" + warehouseLot + '\'' +
                ", customerLot='" + customerLot + '\'' +
                ", manufactureDate=" + manufactureDate +
                ", bestByDate=" + bestByDate +
                ", lineItems=" + lineItems.size() +
                '}';
    }
}


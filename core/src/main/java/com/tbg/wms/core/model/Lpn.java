/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a license plate number (LPN) or pallet in a shipment.
 *
 * Each LPN contains shipment details, line items, and metadata needed for
 * label generation including barcode information and staging location.
 */
public final class Lpn {

    private final String lpnId;
    private final String shipmentId;
    private final String sscc;
    private final int caseCount;
    private final int unitCount;
    private final double weight;
    private final String stagingLocation;
    private final List<LineItem> lineItems;

    /**
     * Creates a new Lpn.
     *
     * @param lpnId the license plate number identifier
     * @param shipmentId the parent shipment identifier
     * @param sscc the SSCC (Serial Shipping Container Code) barcode value
     * @param caseCount number of cases in this pallet
     * @param unitCount total number of units
     * @param weight total weight of the pallet
     * @param stagingLocation the staging location where this pallet is stored
     * @param lineItems list of line items contained in this pallet
     */
    public Lpn(String lpnId, String shipmentId, String sscc, int caseCount, int unitCount,
               double weight, String stagingLocation, List<LineItem> lineItems) {
        this.lpnId = lpnId;
        this.shipmentId = shipmentId;
        this.sscc = sscc;
        this.caseCount = caseCount;
        this.unitCount = unitCount;
        this.weight = weight;
        this.stagingLocation = stagingLocation;
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
                ", lineItems=" + lineItems.size() +
                '}';
    }
}


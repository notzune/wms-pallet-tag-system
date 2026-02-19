/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

/**
 * Aggregated shipment SKU row with item footprint data from WMS.
 *
 * <p>Data is sourced from shipment lines plus item footprint maintenance tables
 * (PRTFTP / PRTFTP_DTL). Values may be partially missing if footprint setup is incomplete.</p>
 */
public final class ShipmentSkuFootprint {

    private final String sku;
    private final String itemDescription;
    private final int totalUnits;
    private final Integer unitsPerCase;
    private final Integer unitsPerPallet;
    private final Double palletLength;
    private final Double palletWidth;
    private final Double palletHeight;

    /**
     * Creates a footprint row without a description fallback.
     *
     * @param sku internal SKU (PRTNUM)
     * @param totalUnits total units across the shipment
     * @param unitsPerCase footprint units-per-case value
     * @param unitsPerPallet footprint units-per-pallet value
     * @param palletLength footprint pallet length
     * @param palletWidth footprint pallet width
     * @param palletHeight footprint pallet height
     */
    public ShipmentSkuFootprint(String sku,
                                int totalUnits,
                                Integer unitsPerCase,
                                Integer unitsPerPallet,
                                Double palletLength,
                                Double palletWidth,
                                Double palletHeight) {
        this(sku, null, totalUnits, unitsPerCase, unitsPerPallet, palletLength, palletWidth, palletHeight);
    }

    /**
     * Creates a footprint row with an item description fallback.
     *
     * @param sku internal SKU (PRTNUM)
     * @param itemDescription optional human-readable description
     * @param totalUnits total units across the shipment
     * @param unitsPerCase footprint units-per-case value
     * @param unitsPerPallet footprint units-per-pallet value
     * @param palletLength footprint pallet length
     * @param palletWidth footprint pallet width
     * @param palletHeight footprint pallet height
     */
    public ShipmentSkuFootprint(String sku,
                                String itemDescription,
                                int totalUnits,
                                Integer unitsPerCase,
                                Integer unitsPerPallet,
                                Double palletLength,
                                Double palletWidth,
                                Double palletHeight) {
        this.sku = sku;
        this.itemDescription = itemDescription;
        this.totalUnits = totalUnits;
        this.unitsPerCase = unitsPerCase;
        this.unitsPerPallet = unitsPerPallet;
        this.palletLength = palletLength;
        this.palletWidth = palletWidth;
        this.palletHeight = palletHeight;
    }

    public String getSku() {
        return sku;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public Integer getUnitsPerCase() {
        return unitsPerCase;
    }

    public Integer getUnitsPerPallet() {
        return unitsPerPallet;
    }

    public Double getPalletLength() {
        return palletLength;
    }

    public Double getPalletWidth() {
        return palletWidth;
    }

    public Double getPalletHeight() {
        return palletHeight;
    }
}

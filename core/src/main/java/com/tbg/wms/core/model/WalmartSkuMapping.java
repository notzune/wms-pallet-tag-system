/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

/**
 * Represents a mapping from TBG internal SKU to Walmart item number.
 *
 * This is a value object loaded from the SKU matrix CSV file. It provides
 * the Walmart item number and product description for a given TBG SKU.
 */
public final class WalmartSkuMapping {

    private final String tbgSku;          // TBG internal SKU (e.g., "205641")
    private final String walmartItemNo;   // Walmart item number (e.g., "30081705")
    private final String description;     // Product description (e.g., "1.36L PL 1/6 NJ STRW BAN")

    /**
     * Creates a new WalmartSkuMapping.
     *
     * @param tbgSku TBG internal SKU number
     * @param walmartItemNo Walmart item number
     * @param description product description
     */
    public WalmartSkuMapping(String tbgSku, String walmartItemNo, String description) {
        this.tbgSku = tbgSku;
        this.walmartItemNo = walmartItemNo;
        this.description = description;
    }

    public String getTbgSku() {
        return tbgSku;
    }

    public String getWalmartItemNo() {
        return walmartItemNo;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "WalmartSkuMapping{" +
                "tbgSku='" + tbgSku + '\'' +
                ", walmartItemNo='" + walmartItemNo + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WalmartSkuMapping)) return false;

        WalmartSkuMapping that = (WalmartSkuMapping) o;

        if (!tbgSku.equals(that.tbgSku)) return false;
        if (!walmartItemNo.equals(that.walmartItemNo)) return false;
        return description.equals(that.description);
    }

    @Override
    public int hashCode() {
        int result = tbgSku.hashCode();
        result = 31 * result + walmartItemNo.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }
}


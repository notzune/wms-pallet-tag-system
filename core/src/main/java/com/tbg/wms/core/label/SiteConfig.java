/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.label;

import java.util.Objects;

/**
 * Site-specific configuration for label generation.
 *
 * Holds information about the shipping site (warehouse) such as the "ship from"
 * address and other site-specific metadata used on labels.
 *
 * This is typically loaded from environment configuration or hardcoded per site.
 */
public final class SiteConfig {

    private final String shipFromName;        // Company name (e.g., "TROPICANA PRODUCTS, INC.")
    private final String shipFromAddress;     // Street address (e.g., "20405 E Business Parkway Rd")
    private final String shipFromCityStateZip; // City, state, zip (e.g., "Walnut, CA 91789")

    /**
     * Creates a new SiteConfig.
     *
     * @param shipFromName the shipping company/site name
     * @param shipFromAddress the street address
     * @param shipFromCityStateZip the city, state, and zip code (pre-formatted)
     */
    public SiteConfig(String shipFromName, String shipFromAddress, String shipFromCityStateZip) {
        this.shipFromName = Objects.requireNonNull(shipFromName, "shipFromName cannot be null");
        this.shipFromAddress = Objects.requireNonNull(shipFromAddress, "shipFromAddress cannot be null");
        this.shipFromCityStateZip = Objects.requireNonNull(shipFromCityStateZip, "shipFromCityStateZip cannot be null");
    }

    public String getShipFromName() {
        return shipFromName;
    }

    public String getShipFromAddress() {
        return shipFromAddress;
    }

    public String getShipFromCityStateZip() {
        return shipFromCityStateZip;
    }

    @Override
    public String toString() {
        return "SiteConfig{" +
                "shipFromName='" + shipFromName + '\'' +
                ", shipFromAddress='" + shipFromAddress + '\'' +
                ", shipFromCityStateZip='" + shipFromCityStateZip + '\'' +
                '}';
    }
}


/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

/**
 * Represents a line item within a shipment.
 *
 * Line items describe individual SKUs and quantities within a shipment,
 * including details needed for label generation such as product descriptions
 * and quantity information.
 */
public final class LineItem {

    private final String lineNumber;
    private final String sku;
    private final String description;
    private final int quantity;
    private final double weight;
    private final String uom;

    /**
     * Creates a new LineItem.
     *
     * @param lineNumber the line number within the shipment
     * @param sku the stock keeping unit identifier
     * @param description product description
     * @param quantity quantity of items
     * @param weight total weight of line item
     * @param uom unit of measure (e.g., "LB", "KG")
     */
    public LineItem(String lineNumber, String sku, String description, int quantity, double weight, String uom) {
        this.lineNumber = lineNumber;
        this.sku = sku;
        this.description = description;
        this.quantity = quantity;
        this.weight = weight;
        this.uom = uom;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getSku() {
        return sku;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getWeight() {
        return weight;
    }

    public String getUom() {
        return uom;
    }

    @Override
    public String toString() {
        return "LineItem{" +
                "lineNumber='" + lineNumber + '\'' +
                ", sku='" + sku + '\'' +
                ", description='" + description + '\'' +
                ", quantity=" + quantity +
                ", weight=" + weight +
                ", uom='" + uom + '\'' +
                '}';
    }
}


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
 * including details needed for label generation such as product descriptions,
 * order references, and Walmart item code mappings.
 */
public final class LineItem {

    // Line identifiers and SKU
    private final String lineNumber;           // ORD_LINE line number
    private final String lineSubNumber;        // ORD_LINE sub-line number
    private final String sku;                  // ORD_LINE.PRTNUM (full 17-digit internal SKU)
    private final String description;          // Product description
    private final String customerPartNumber;   // ORD_LINE.CSTPRT (customer's part number, optional)

    // Order references
    private final String orderNumber;          // ORD_LINE.ORDNUM (order number)
    private final String consolidationBatch;   // SHIPMENT_LINE.CONS_BATCH (consolidation reference)
    private final String salesOrderNumber;     // ORD_LINE.SALES_ORDNUM (SAP/ERP reference)

    // Quantity and UOM
    private final int quantity;                // Shipped quantity
    private final int unitsPerCase;            // ORD_LINE.UNTPAK (units per case)
    private final String uom;                  // Unit of measure

    // Weight
    private final double weight;               // Total weight for this line

    // Barcode and alternate numbers
    private final String walmartItemNumber;    // From CSV mapping (Walmart Item#)
    private final String gtinBarcode;          // ALT_PRTMST type=GTIN
    private final String upcCode;              // ALT_PRTMST type=UPC

    /**
     * Creates a new LineItem with comprehensive WMS database fields.
     *
     * @param lineNumber line number within shipment
     * @param lineSubNumber sub-line number
     * @param sku full 17-digit internal SKU (PRTNUM)
     * @param description product description
     * @param customerPartNumber customer's part number (optional)
     * @param orderNumber order number
     * @param consolidationBatch consolidation batch reference
     * @param salesOrderNumber SAP/ERP order reference
     * @param quantity shipped quantity
     * @param unitsPerCase units per case
     * @param uom unit of measure
     * @param weight total weight for this line
     * @param walmartItemNumber Walmart item number (from CSV mapping)
     * @param gtinBarcode GTIN barcode (from ALT_PRTMST)
     * @param upcCode UPC code (from ALT_PRTMST)
     */
    public LineItem(String lineNumber, String lineSubNumber, String sku, String description,
                    String customerPartNumber, String orderNumber, String consolidationBatch,
                    String salesOrderNumber, int quantity, int unitsPerCase, String uom,
                    double weight, String walmartItemNumber, String gtinBarcode, String upcCode) {
        this.lineNumber = lineNumber;
        this.lineSubNumber = lineSubNumber;
        this.sku = sku;
        this.description = description;
        this.customerPartNumber = customerPartNumber;
        this.orderNumber = orderNumber;
        this.consolidationBatch = consolidationBatch;
        this.salesOrderNumber = salesOrderNumber;
        this.quantity = quantity;
        this.unitsPerCase = unitsPerCase;
        this.uom = uom;
        this.weight = weight;
        this.walmartItemNumber = walmartItemNumber;
        this.gtinBarcode = gtinBarcode;
        this.upcCode = upcCode;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getLineSubNumber() {
        return lineSubNumber;
    }

    public String getSku() {
        return sku;
    }

    public String getDescription() {
        return description;
    }

    public String getCustomerPartNumber() {
        return customerPartNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getConsolidationBatch() {
        return consolidationBatch;
    }

    public String getSalesOrderNumber() {
        return salesOrderNumber;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getUnitsPerCase() {
        return unitsPerCase;
    }

    public String getUom() {
        return uom;
    }

    public double getWeight() {
        return weight;
    }

    public String getWalmartItemNumber() {
        return walmartItemNumber;
    }

    public String getGtinBarcode() {
        return gtinBarcode;
    }

    public String getUpcCode() {
        return upcCode;
    }

    @Override
    public String toString() {
        return "LineItem{" +
                "lineNumber='" + lineNumber + '\'' +
                ", lineSubNumber='" + lineSubNumber + '\'' +
                ", sku='" + sku + '\'' +
                ", description='" + description + '\'' +
                ", orderNumber='" + orderNumber + '\'' +
                ", quantity=" + quantity +
                ", walmartItemNumber='" + walmartItemNumber + '\'' +
                ", weight=" + weight +
                ", uom='" + uom + '\'' +
                '}';
    }
}


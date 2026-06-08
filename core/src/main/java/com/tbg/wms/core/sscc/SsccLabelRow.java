package com.tbg.wms.core.sscc;

/**
 * One raw SSCC source row imported from CSV or entered manually.
 */
public record SsccLabelRow(
        String salesOrder,
        String purchaseOrder,
        String shipment,
        String carrierCode,
        String trailerId,
        String destination,
        String destinationAddress,
        String customerName,
        String facility,
        String itemNumber,
        String level2Reference,
        String originallyShippedLpn,
        double sumOfShipCases,
        String newReceivedLpn
) {
    public SsccLabelRow {
        salesOrder = trim(salesOrder);
        purchaseOrder = trim(purchaseOrder);
        shipment = trim(shipment);
        carrierCode = trim(carrierCode);
        trailerId = trim(trailerId);
        destination = trim(destination);
        destinationAddress = trim(destinationAddress);
        customerName = trim(customerName);
        facility = trim(facility);
        itemNumber = trim(itemNumber);
        level2Reference = trim(level2Reference);
        originallyShippedLpn = trim(originallyShippedLpn);
        newReceivedLpn = trim(newReceivedLpn);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public boolean hasLabelIdentity() {
        return !salesOrder.isBlank() && !newReceivedLpn.isBlank();
    }

    public boolean hasGroupingMetadata() {
        return !purchaseOrder.isBlank()
                || !shipment.isBlank()
                || !carrierCode.isBlank()
                || !trailerId.isBlank()
                || !destination.isBlank()
                || !destinationAddress.isBlank()
                || !customerName.isBlank()
                || !facility.isBlank();
    }

    public SsccLabelRow withNonNegativeCases() {
        return sumOfShipCases < 0.0d
                ? new SsccLabelRow(salesOrder, purchaseOrder, shipment, carrierCode, trailerId,
                destination, destinationAddress, customerName, facility, itemNumber,
                level2Reference, originallyShippedLpn, 0.0d, newReceivedLpn)
                : this;
    }

    @Override
    public String toString() {
        return "SsccLabelRow[salesOrder=" + salesOrder + ", newReceivedLpn=" + newReceivedLpn + "]";
    }
}

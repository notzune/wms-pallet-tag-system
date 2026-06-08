package com.tbg.wms.core.sscc;

import java.util.List;
import java.util.Objects;

/**
 * One rendered SSCC label after grouping source rows by sales order and new LPN.
 */
public record SsccLabelGroup(
        String salesOrder,
        String purchaseOrder,
        String shipment,
        String carrierCode,
        String trailerId,
        String destination,
        String destinationAddress,
        String customerName,
        String facility,
        String newReceivedLpn,
        double sumOfShipCases,
        List<String> items,
        List<String> level2ReferenceCodes
) {
    public SsccLabelGroup {
        salesOrder = trim(salesOrder);
        purchaseOrder = trim(purchaseOrder);
        shipment = trim(shipment);
        carrierCode = trim(carrierCode);
        trailerId = trim(trailerId);
        destination = trim(destination);
        destinationAddress = trim(destinationAddress);
        customerName = trim(customerName);
        facility = trim(facility);
        newReceivedLpn = trim(newReceivedLpn);
        items = List.copyOf(Objects.requireNonNull(items, "items cannot be null"));
        level2ReferenceCodes = List.copyOf(Objects.requireNonNull(level2ReferenceCodes, "level2ReferenceCodes cannot be null"));
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public int itemCount() {
        return items.size();
    }

    public boolean mixedSku() {
        return itemCount() > 1;
    }

    public String bannerText() {
        if (mixedSku()) {
            return "MIXED SKU";
        }
        String firstItem = firstItem();
        return firstItem.isBlank() ? "SKU" : "SKU " + firstItem;
    }

    public String itemLineText() {
        return mixedSku() ? "MIXED SKU" : firstItem();
    }

    public String cartonText() {
        if (sumOfShipCases <= 0.0d) {
            return "";
        }
        return String.valueOf(Math.round(sumOfShipCases));
    }

    public String ssccDigits() {
        return newReceivedLpn.replaceAll("\\D", "");
    }

    private String firstItem() {
        return items.isEmpty() ? "" : items.get(0);
    }
}

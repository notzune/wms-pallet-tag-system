/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.label;

import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.model.WalmartSkuMapping;
import com.tbg.wms.core.sku.SkuMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Populates product and footprint label fields for a shipment pallet.
 */
final class LabelProductFieldSupport {

    private static final Logger log = LoggerFactory.getLogger(LabelProductFieldSupport.class);
    private static final String SPACE_SAFE_DEFAULT = " ";

    private final SkuMappingService skuMapping;
    private final Map<String, ShipmentSkuFootprint> footprintBySku;

    LabelProductFieldSupport(SkuMappingService skuMapping, Map<String, ShipmentSkuFootprint> footprintBySku) {
        this.skuMapping = Objects.requireNonNull(skuMapping, "skuMapping cannot be null");
        this.footprintBySku = footprintBySku == null ? Map.of() : Map.copyOf(footprintBySku);
    }

    void populateFields(Map<String, String> fields, Shipment shipment, Lpn lpn) {
        Objects.requireNonNull(fields, "fields cannot be null");
        Objects.requireNonNull(shipment, "shipment cannot be null");
        Objects.requireNonNull(lpn, "lpn cannot be null");

        RepresentativeItemSelection selection = selectRepresentativeItem(lpn);
        if (selection == null) {
            applyDefaultProductFields(fields);
            return;
        }

        LineItem item = selection.item();
        String sku = orDefault(item.getSku(), SPACE_SAFE_DEFAULT);
        fields.put("tbgSku", sku);
        fields.put("quantity", String.valueOf(item.getQuantity()));
        fields.put("unitOfMeasure", orDefault(item.getUom(), "EA"));

        WalmartSkuMapping mapping = selection.mapping();
        if (mapping != null) {
            fields.put("walmartItemNumber", mapping.getWalmartItemNo());
            fields.put("itemDescription", mapping.getDescription());
        } else {
            fields.put("walmartItemNumber", SPACE_SAFE_DEFAULT);
            fields.put("itemDescription", orDefault(item.getDescription(), SPACE_SAFE_DEFAULT));
            log.info("Walmart item code not found in matrix for SKU {} (shipment {})",
                    sku, shipment.getShipmentId());
        }

        fields.put("gtinBarcode", orDefault(item.getGtinBarcode(), SPACE_SAFE_DEFAULT));
        fields.put("upcCode", orDefault(item.getUpcCode(), SPACE_SAFE_DEFAULT));

        ShipmentSkuFootprint footprint = footprintBySku.get(item.getSku());
        fields.put("unitsPerCase", numberOrDefault(footprint == null ? null : footprint.getUnitsPerCase()));
        fields.put("unitsPerPallet", numberOrDefault(footprint == null ? null : footprint.getUnitsPerPallet()));
        fields.put("palletLength", numberOrDefault(footprint == null ? null : footprint.getPalletLength()));
        fields.put("palletWidth", numberOrDefault(footprint == null ? null : footprint.getPalletWidth()));
        fields.put("palletHeight", numberOrDefault(footprint == null ? null : footprint.getPalletHeight()));
    }

    private RepresentativeItemSelection selectRepresentativeItem(Lpn lpn) {
        LineItem fallback = null;
        for (LineItem item : lpn.getLineItems()) {
            if (item == null || item.getSku() == null || item.getSku().trim().isEmpty()) {
                continue;
            }
            if (fallback == null) {
                fallback = item;
            }
            WalmartSkuMapping mapping = skuMapping.findByPrtnum(item.getSku());
            if (mapping != null) {
                return new RepresentativeItemSelection(item, mapping);
            }
        }
        if (fallback == null) {
            return null;
        }
        return new RepresentativeItemSelection(fallback, skuMapping.findByPrtnum(fallback.getSku()));
    }

    private void applyDefaultProductFields(Map<String, String> fields) {
        fields.put("tbgSku", SPACE_SAFE_DEFAULT);
        fields.put("quantity", "0");
        fields.put("unitOfMeasure", "EA");
        fields.put("walmartItemNumber", SPACE_SAFE_DEFAULT);
        fields.put("itemDescription", SPACE_SAFE_DEFAULT);
        fields.put("gtinBarcode", SPACE_SAFE_DEFAULT);
        fields.put("upcCode", SPACE_SAFE_DEFAULT);
        fields.put("unitsPerCase", SPACE_SAFE_DEFAULT);
        fields.put("unitsPerPallet", SPACE_SAFE_DEFAULT);
        fields.put("palletLength", SPACE_SAFE_DEFAULT);
        fields.put("palletWidth", SPACE_SAFE_DEFAULT);
        fields.put("palletHeight", SPACE_SAFE_DEFAULT);
    }

    private String orDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String numberOrDefault(Number value) {
        return value == null ? SPACE_SAFE_DEFAULT : String.valueOf(value);
    }

    private record RepresentativeItemSelection(LineItem item, WalmartSkuMapping mapping) {
    }
}

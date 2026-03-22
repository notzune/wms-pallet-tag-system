/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.labeling.LabelingSupport;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.model.WalmartSkuMapping;
import com.tbg.wms.core.sku.SkuMappingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds preview planning artifacts such as SKU math rows and virtual LPN fallback rows.
 */
final class LabelWorkflowPlanningSupport {

    List<LabelWorkflowService.SkuMathRow> buildSkuMathRows(List<ShipmentSkuFootprint> rows, SkuMappingService skuMapping) {
        Objects.requireNonNull(rows, "rows cannot be null");
        Objects.requireNonNull(skuMapping, "skuMapping cannot be null");

        List<LabelWorkflowService.SkuMathRow> mathRows = new ArrayList<>();
        for (ShipmentSkuFootprint row : rows) {
            if (row == null) {
                continue;
            }

            String sku = row.getSku();
            if (sku == null || sku.isBlank()) {
                continue;
            }

            int units = Math.max(0, row.getTotalUnits());
            Integer upp = row.getUnitsPerPallet();
            int fullPallets = 0;
            int partialPallets = 0;
            int estimatedPallets = 0;
            if (upp != null && upp > 0) {
                fullPallets = units / upp;
                partialPallets = units % upp > 0 ? 1 : 0;
                estimatedPallets = fullPallets + partialPallets;
            }

            String description = row.getItemDescription();
            if (!LabelingSupport.isHumanReadable(description)) {
                WalmartSkuMapping mapping = skuMapping.findByPrtnum(sku);
                if (mapping != null && LabelingSupport.isHumanReadable(mapping.getDescription())) {
                    description = mapping.getDescription();
                }
            }

            mathRows.add(new LabelWorkflowService.SkuMathRow(
                    sku,
                    description == null ? "" : description,
                    units,
                    upp,
                    fullPallets,
                    partialPallets,
                    estimatedPallets
            ));
        }
        return mathRows;
    }

    List<Lpn> resolveLpnsForLabeling(Shipment shipment, List<ShipmentSkuFootprint> footprintRows) {
        Objects.requireNonNull(shipment, "shipment cannot be null");
        Objects.requireNonNull(footprintRows, "footprintRows cannot be null");
        List<Lpn> lpns = shipment.getLpns();
        if (!lpns.isEmpty()) {
            return lpns;
        }
        List<Lpn> virtual = LabelingSupport.buildVirtualLpnsFromFootprints(shipment, footprintRows);
        return virtual.isEmpty() ? lpns : virtual;
    }
}

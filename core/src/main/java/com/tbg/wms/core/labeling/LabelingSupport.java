/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.2.0
 */

package com.tbg.wms.core.labeling;

import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared helpers for label workflows (CLI/GUI).
 */
public final class LabelingSupport {

    private static final List<Path> SKU_MATRIX_CANDIDATES = List.of(
            Paths.get("config/walmart-sku-matrix.csv"),
            Paths.get("config/walmart_sku_matrix.csv"),
            Paths.get("config/TBG3002/walmart-sku-matrix.csv"),
            Paths.get("config/TBG3002/walmart_sku_matrix.csv"),
            Paths.get("walmart-sku-matrix.csv"),
            Paths.get("walmart_sku_matrix.csv")
    );

    /**
     * Resolves the Walmart SKU matrix CSV from standard locations.
     *
     * @return CSV path or null if not found
     */
    public static Path resolveSkuMatrixCsv() {
        for (Path candidate : SKU_MATRIX_CANDIDATES) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Builds a map of SKU -> footprint row.
     */
    public static Map<String, ShipmentSkuFootprint> buildFootprintMap(List<ShipmentSkuFootprint> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ShipmentSkuFootprint> bySku = new HashMap<>();
        for (ShipmentSkuFootprint row : rows) {
            if (row != null && row.getSku() != null && !row.getSku().isBlank()) {
                bySku.put(row.getSku(), row);
            }
        }
        return bySku;
    }

    /**
     * Builds virtual LPNs when a shipment lacks physical LPN rows.
     */
    public static List<Lpn> buildVirtualLpnsFromFootprints(Shipment shipment, List<ShipmentSkuFootprint> footprintRows) {
        if (footprintRows == null || footprintRows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Lpn> virtualLpns = new ArrayList<>();
        int seq = 0;

        for (ShipmentSkuFootprint row : footprintRows) {
            if (row == null || row.getSku() == null || row.getSku().isBlank()) {
                continue;
            }

            int totalUnits = Math.max(0, row.getTotalUnits());
            if (totalUnits == 0) {
                continue;
            }

            Integer unitsPerPallet = row.getUnitsPerPallet();
            int palletsForSku;
            if (unitsPerPallet == null || unitsPerPallet <= 0) {
                palletsForSku = 1;
            } else {
                palletsForSku = totalUnits / unitsPerPallet;
                if (totalUnits % unitsPerPallet != 0) {
                    palletsForSku += 1;
                }
            }

            for (int palletIndex = 0; palletIndex < palletsForSku; palletIndex++) {
                int palletUnits;
                if (unitsPerPallet == null || unitsPerPallet <= 0) {
                    palletUnits = totalUnits;
                } else if (palletIndex < palletsForSku - 1) {
                    palletUnits = unitsPerPallet;
                } else {
                    int remainder = totalUnits % unitsPerPallet;
                    palletUnits = remainder == 0 ? unitsPerPallet : remainder;
                }

                seq++;
                String description = isHumanReadable(row.getItemDescription()) ? row.getItemDescription() : null;
                LineItem item = new LineItem(
                        String.valueOf(seq),
                        "0",
                        row.getSku(),
                        description,
                        null,
                        shipment.getOrderId(),
                        null,
                        null,
                        palletUnits,
                        row.getUnitsPerCase() != null ? row.getUnitsPerCase() : 0,
                        "EA",
                        0.0,
                        null,
                        null,
                        null
                );

                Lpn virtualLpn = new Lpn(
                        "NO_LPN_" + seq,
                        shipment.getShipmentId(),
                        String.format("%018d", seq),
                        0,
                        palletUnits,
                        0.0,
                        shipment.getDestinationLocation(),
                        null,
                        null,
                        LocalDate.now(),
                        LocalDate.now(),
                        List.of(item)
                );
                virtualLpns.add(virtualLpn);
            }
        }

        return virtualLpns;
    }

    /**
     * Checks if a value has readable text (letters).
     */
    public static boolean isHumanReadable(String value) {
        return value != null && value.chars().anyMatch(Character::isLetter);
    }

    private LabelingSupport() {
    }
}

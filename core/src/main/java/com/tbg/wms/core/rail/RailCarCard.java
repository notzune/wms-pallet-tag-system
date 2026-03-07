/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Print-ready railcar card model.
 */
public final class RailCarCard {
    private final String trainId;
    private final String sequence;
    private final String vehicleId;
    private final String loadNumbers;
    private final List<RailStopRecord.ItemQuantity> itemLines;
    private final int canPallets;
    private final int domPallets;
    private final int kevPallets;
    private final List<String> topFamilies;
    private final List<String> missingFootprintItems;

    public RailCarCard(String trainId,
                       String sequence,
                       String vehicleId,
                       String loadNumbers,
                       List<RailStopRecord.ItemQuantity> itemLines,
                       int canPallets,
                       int domPallets,
                       int kevPallets,
                       List<String> topFamilies,
                       List<String> missingFootprintItems) {
        this.trainId = normalize(trainId);
        this.sequence = normalize(sequence);
        this.vehicleId = normalize(vehicleId);
        this.loadNumbers = normalize(loadNumbers);
        this.itemLines = Collections.unmodifiableList(new ArrayList<>(itemLines));
        this.canPallets = canPallets;
        this.domPallets = domPallets;
        this.kevPallets = kevPallets;
        this.topFamilies = Collections.unmodifiableList(new ArrayList<>(topFamilies));
        this.missingFootprintItems = Collections.unmodifiableList(new ArrayList<>(missingFootprintItems));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public String getTrainId() {
        return trainId;
    }

    public String getSequence() {
        return sequence;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getLoadNumbers() {
        return loadNumbers;
    }

    public List<RailStopRecord.ItemQuantity> getItemLines() {
        return itemLines;
    }

    public int getCanPallets() {
        return canPallets;
    }

    public int getDomPallets() {
        return domPallets;
    }

    /**
     * Returns KEV-family pallet count for this railcar card.
     */
    public int getKevPallets() {
        return kevPallets;
    }

    public List<String> getTopFamilies() {
        return topFamilies;
    }

    public List<String> getMissingFootprintItems() {
        return missingFootprintItems;
    }
}

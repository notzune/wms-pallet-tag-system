/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.core.rail;

import java.util.*;

/**
 * Computes deterministic family percentages for rail labels using item footprint lookup data.
 */
public final class RailLabelPlanner {
    private static final int DEFAULT_ITEM_SLOTS = 13;
    private final int itemSlots;

    public RailLabelPlanner() {
        this(DEFAULT_ITEM_SLOTS);
    }

    public RailLabelPlanner(int itemSlots) {
        if (itemSlots <= 0) {
            throw new IllegalArgumentException("itemSlots must be > 0");
        }
        this.itemSlots = itemSlots;
    }

    /**
     * Calculates per-row rail label callouts.
     *
     * @param records    flattened rail rows
     * @param footprints item-family footprint lookup by item number
     * @return list of planned rows in input order
     */
    public List<PlannedRailLabel> plan(List<RailStopRecord> records, Map<String, RailFamilyFootprint> footprints) {
        Objects.requireNonNull(records, "records cannot be null");
        Objects.requireNonNull(footprints, "footprints cannot be null");

        List<PlannedRailLabel> planned = new ArrayList<>();
        for (RailStopRecord record : records) {
            if (record == null) {
                continue;
            }
            planned.add(planOne(record, footprints));
        }
        return Collections.unmodifiableList(planned);
    }

    /**
     * Calculates a planned row for one rail stop record.
     *
     * @param record     one flattened rail row
     * @param footprints item-family footprint lookup by item number
     * @return planned row with family callouts and diagnostics
     */
    public PlannedRailLabel planOne(RailStopRecord record, Map<String, RailFamilyFootprint> footprints) {
        Objects.requireNonNull(record, "record cannot be null");
        Objects.requireNonNull(footprints, "footprints cannot be null");
        return planRecord(record, footprints);
    }

    private PlannedRailLabel planRecord(RailStopRecord record, Map<String, RailFamilyFootprint> footprints) {
        Map<String, Double> equivalentByFamily = new HashMap<>();
        List<String> missingItems = new ArrayList<>();
        List<RailStopRecord.ItemQuantity> overflowItems = new ArrayList<>();
        double totalEquivalent = 0.0d;
        int slotCount = 0;

        for (RailStopRecord.ItemQuantity item : record.getItems()) {
            if (item == null || !item.isValid()) {
                continue;
            }
            if (slotCount >= itemSlots) {
                overflowItems.add(item);
            }
            slotCount++;
            RailFamilyFootprint footprint = footprints.get(item.getItemNumber());
            if (footprint == null || !footprint.isValid()) {
                missingItems.add(item.getItemNumber());
                continue;
            }
            double equivalent = ((double) item.getCases()) / (double) footprint.getCasesPerPallet();
            totalEquivalent += equivalent;
            equivalentByFamily.merge(footprint.getFamilyCode(), equivalent, Double::sum);
        }

        List<FamilyShare> shares = buildSortedShares(equivalentByFamily, totalEquivalent);
        List<FamilyShare> topThree = shares.size() <= 3 ? shares : shares.subList(0, 3);
        return new PlannedRailLabel(record, topThree, missingItems, overflowItems, totalEquivalent, itemSlots);
    }

    private List<FamilyShare> buildSortedShares(Map<String, Double> equivalentByFamily, double totalEquivalent) {
        List<FamilyShare> shares = new ArrayList<>(equivalentByFamily.size());
        if (totalEquivalent <= 0.0d) {
            return shares;
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(equivalentByFamily.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        Map<String, Integer> percentByFamily = allocatePercentages(entries, totalEquivalent);

        for (Map.Entry<String, Double> entry : entries) {
            int percent = percentByFamily.getOrDefault(entry.getKey(), 0);
            shares.add(new FamilyShare(entry.getKey(), percent, entry.getValue()));
        }
        return shares;
    }

    /**
     * Allocates integer percentages that always sum to 100 using a largest-remainder policy.
     *
     * <p>This prevents per-family Math.round drift (e.g. 33/33/33 -> 99 or 34/34/34 -> 102)
     * while preserving deterministic ordering on ties.</p>
     */
    private Map<String, Integer> allocatePercentages(List<Map.Entry<String, Double>> entries, double totalEquivalent) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (entries.isEmpty() || totalEquivalent <= 0.0d) {
            return result;
        }

        List<PercentCandidate> candidates = new ArrayList<>(entries.size());
        int floorSum = 0;
        for (Map.Entry<String, Double> entry : entries) {
            double exact = (entry.getValue() / totalEquivalent) * 100.0d;
            int floor = (int) Math.floor(exact);
            floorSum += floor;
            candidates.add(new PercentCandidate(entry.getKey(), exact - floor));
            result.put(entry.getKey(), floor);
        }

        int remaining = Math.max(0, 100 - floorSum);
        candidates.sort(Comparator.comparingDouble(PercentCandidate::remainder).reversed()
                .thenComparing(PercentCandidate::familyCode));

        for (int i = 0; i < remaining && i < candidates.size(); i++) {
            PercentCandidate candidate = candidates.get(i);
            result.put(candidate.familyCode(), result.get(candidate.familyCode()) + 1);
        }

        return result;
    }

    private static final class PercentCandidate {
        private final String familyCode;
        private final double remainder;

        private PercentCandidate(String familyCode, double remainder) {
            this.familyCode = familyCode;
            this.remainder = remainder;
        }

        private String familyCode() {
            return familyCode;
        }

        private double remainder() {
            return remainder;
        }
    }

    /**
     * Planned row with computed family callouts and diagnostics.
     */
    public static final class PlannedRailLabel {
        private final RailStopRecord sourceRecord;
        private final List<FamilyShare> topFamilies;
        private final List<String> missingFootprintItems;
        private final List<RailStopRecord.ItemQuantity> overflowItems;
        private final double totalPalletEquivalent;
        private final int itemSlots;

        private PlannedRailLabel(RailStopRecord sourceRecord,
                                 List<FamilyShare> topFamilies,
                                 List<String> missingFootprintItems,
                                 List<RailStopRecord.ItemQuantity> overflowItems,
                                 double totalPalletEquivalent,
                                 int itemSlots) {
            this.sourceRecord = sourceRecord;
            this.topFamilies = Collections.unmodifiableList(new ArrayList<>(topFamilies));
            this.missingFootprintItems = Collections.unmodifiableList(new ArrayList<>(missingFootprintItems));
            this.overflowItems = Collections.unmodifiableList(new ArrayList<>(overflowItems));
            this.totalPalletEquivalent = totalPalletEquivalent;
            this.itemSlots = itemSlots;
        }

        private static void appendItemPairs(Map<String, String> fields,
                                            List<RailStopRecord.ItemQuantity> items,
                                            int slots) {
            int count = 0;
            for (RailStopRecord.ItemQuantity item : items) {
                if (item == null || !item.isValid()) {
                    continue;
                }
                if (count >= slots) {
                    break;
                }
                int index = count + 1;
                fields.put("ITEM_NBR_" + index, item.getItemNumber());
                fields.put("TOTAL_CS_ITM_" + index, Integer.toString(item.getCases()));
                count++;
            }
            while (count < slots) {
                int index = count + 1;
                fields.put("ITEM_NBR_" + index, "");
                fields.put("TOTAL_CS_ITM_" + index, "");
                count++;
            }
        }

        private static void appendTopFamilies(Map<String, String> fields, List<FamilyShare> topFamilies) {
            for (int i = 0; i < 3; i++) {
                if (i < topFamilies.size()) {
                    FamilyShare share = topFamilies.get(i);
                    fields.put("Item_" + (i + 1), share.familyCode + ":" + share.percent);
                } else {
                    fields.put("Item_" + (i + 1), "");
                }
            }
        }

        public RailStopRecord getSourceRecord() {
            return sourceRecord;
        }

        public List<FamilyShare> getTopFamilies() {
            return topFamilies;
        }

        public List<String> getMissingFootprintItems() {
            return missingFootprintItems;
        }

        public double getTotalPalletEquivalent() {
            return totalPalletEquivalent;
        }

        public List<RailStopRecord.ItemQuantity> getOverflowItems() {
            return overflowItems;
        }

        public Map<String, String> toMergeFields() {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("DATE", sourceRecord.getDate());
            fields.put("SEQ", sourceRecord.getSequence());
            fields.put("TRAIN_NBR", sourceRecord.getTrainNumber());
            fields.put("VEHICLE_ID", sourceRecord.getVehicleId());
            fields.put("DCS_WHSE", sourceRecord.getWarehouse());
            fields.put("LOAD_NBR", sourceRecord.getLoadNumber());

            appendItemPairs(fields, sourceRecord.getItems(), itemSlots);
            appendTopFamilies(fields, topFamilies);
            return fields;
        }
    }

    /**
     * Family share summary.
     */
    public static final class FamilyShare {
        private final String familyCode;
        private final int percent;
        private final double equivalentPallets;

        private FamilyShare(String familyCode, int percent, double equivalentPallets) {
            this.familyCode = familyCode;
            this.percent = percent;
            this.equivalentPallets = equivalentPallets;
        }

        public String getFamilyCode() {
            return familyCode;
        }

        public int getPercent() {
            return percent;
        }

        public double getEquivalentPallets() {
            return equivalentPallets;
        }
    }
}

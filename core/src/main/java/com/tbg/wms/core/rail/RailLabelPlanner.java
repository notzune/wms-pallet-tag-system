package com.tbg.wms.core.rail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes deterministic family percentages for rail labels using item footprint lookup data.
 */
public final class RailLabelPlanner {

    /**
     * Calculates per-row rail label callouts.
     *
     * @param records flattened rail rows
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
            planned.add(planRecord(record, footprints));
        }
        return Collections.unmodifiableList(planned);
    }

    private PlannedRailLabel planRecord(RailStopRecord record, Map<String, RailFamilyFootprint> footprints) {
        Map<String, Double> equivalentByFamily = new HashMap<>();
        List<String> missingItems = new ArrayList<>();
        double totalEquivalent = 0.0d;

        for (RailStopRecord.ItemQuantity item : record.getItems()) {
            if (item == null || !item.isValid()) {
                continue;
            }
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
        return new PlannedRailLabel(record, topThree, missingItems, totalEquivalent);
    }

    private List<FamilyShare> buildSortedShares(Map<String, Double> equivalentByFamily, double totalEquivalent) {
        List<FamilyShare> shares = new ArrayList<>();
        if (totalEquivalent <= 0.0d) {
            return shares;
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(equivalentByFamily.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));

        for (Map.Entry<String, Double> entry : entries) {
            int percent = BigDecimal.valueOf((entry.getValue() / totalEquivalent) * 100.0d)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            shares.add(new FamilyShare(entry.getKey(), percent, entry.getValue()));
        }
        return shares;
    }

    /**
     * Planned row with computed family callouts and diagnostics.
     */
    public static final class PlannedRailLabel {
        private final RailStopRecord sourceRecord;
        private final List<FamilyShare> topFamilies;
        private final List<String> missingFootprintItems;
        private final double totalPalletEquivalent;

        private PlannedRailLabel(RailStopRecord sourceRecord,
                                 List<FamilyShare> topFamilies,
                                 List<String> missingFootprintItems,
                                 double totalPalletEquivalent) {
            this.sourceRecord = sourceRecord;
            this.topFamilies = Collections.unmodifiableList(new ArrayList<>(topFamilies));
            this.missingFootprintItems = Collections.unmodifiableList(new ArrayList<>(missingFootprintItems));
            this.totalPalletEquivalent = totalPalletEquivalent;
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

        public Map<String, String> toMergeFields() {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("DATE", sourceRecord.getDate());
            fields.put("SEQ", sourceRecord.getSequence());
            fields.put("TRAIN_NBR", sourceRecord.getTrainNumber());
            fields.put("VEHICLE_ID", sourceRecord.getVehicleId());
            fields.put("DCS_WHSE", sourceRecord.getWarehouse());
            fields.put("LOAD_NBR", sourceRecord.getLoadNumber());

            appendItemPairs(fields, sourceRecord.getItems(), 6);
            appendTopFamilies(fields, topFamilies);
            return fields;
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

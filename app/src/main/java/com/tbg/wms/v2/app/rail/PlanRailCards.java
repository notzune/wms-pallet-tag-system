package com.tbg.wms.v2.app.rail;

import com.tbg.wms.v2.domain.rail.RailCarCard;
import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import com.tbg.wms.v2.domain.rail.RailStopRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Groups rail stop rows into printable railcar card plans.
 */
public final class PlanRailCards {
    /**
     * Builds a rail plan for a train using normalized family footprint data.
     *
     * @param trainId requested train identifier
     * @param rows rail stop rows returned by the repository
     * @param footprints cases-per-pallet lookup keyed by item or family footprint item
     * @return printable rail plan with missing footprint warnings
     */
    public RailPlan plan(
            String trainId,
            List<RailStopRecord> rows,
            Map<String, RailFamilyFootprint> footprints
    ) {
        String normalizedTrainId = upper(trainId);
        if (normalizedTrainId.isBlank()) {
            throw new IllegalArgumentException("Train ID is required.");
        }

        Map<String, RailFamilyFootprint> footprintByItem = normalizeFootprints(footprints);
        Map<GroupKey, CardAccumulator> groups = new LinkedHashMap<>();
        for (RailStopRecord row : rows == null ? List.<RailStopRecord>of() : rows) {
            if (row == null) {
                continue;
            }
            GroupKey key = new GroupKey(row.trainNumber(), row.sequence(), row.vehicleId());
            groups.computeIfAbsent(key, ignored -> new CardAccumulator(row))
                    .add(row);
        }

        TreeSet<String> planMissingItems = new TreeSet<>();
        List<RailCarCard> cards = new ArrayList<>();
        for (CardAccumulator group : groups.values()) {
            RailCarCard card = group.toCard(normalizedTrainId, footprintByItem);
            planMissingItems.addAll(card.missingFootprintItems());
            cards.add(card);
        }

        return new RailPlan(normalizedTrainId, cards, new ArrayList<>(planMissingItems));
    }

    private static Map<String, RailFamilyFootprint> normalizeFootprints(Map<String, RailFamilyFootprint> footprints) {
        Map<String, RailFamilyFootprint> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, RailFamilyFootprint> entry : (footprints == null ? Map.<String, RailFamilyFootprint>of() : footprints).entrySet()) {
            RailFamilyFootprint footprint = entry.getValue();
            if (footprint != null) {
                normalized.put(footprint.itemNumber(), footprint);
                normalized.put(upper(entry.getKey()), footprint);
            }
        }
        return normalized;
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record GroupKey(String trainNumber, String sequence, String vehicleId) {
    }

    private static final class CardAccumulator {
        private final RailStopRecord firstRow;
        private final TreeSet<String> loadNumbers = new TreeSet<>();
        private final Map<String, Integer> casesByItem = new LinkedHashMap<>();

        private CardAccumulator(RailStopRecord firstRow) {
            this.firstRow = Objects.requireNonNull(firstRow);
        }

        private void add(RailStopRecord row) {
            if (!row.loadNumber().isBlank()) {
                loadNumbers.add(row.loadNumber());
            }
            for (RailItemQuantity item : row.items()) {
                if (item != null && item.isUsable()) {
                    casesByItem.merge(item.itemNumber(), item.cases(), Integer::sum);
                }
            }
        }

        private RailCarCard toCard(String trainId, Map<String, RailFamilyFootprint> footprintByItem) {
            List<RailItemQuantity> itemLines = casesByItem.entrySet().stream()
                    .map(entry -> new RailItemQuantity(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparingInt(RailItemQuantity::cases).reversed()
                            .thenComparing(RailItemQuantity::itemNumber))
                    .toList();

            PalletTotals totals = new PalletTotals();
            TreeMap<String, Integer> familyCases = new TreeMap<>();
            TreeSet<String> missingItems = new TreeSet<>();
            for (RailItemQuantity item : itemLines) {
                RailFamilyFootprint footprint = footprintByItem.get(item.itemNumber());
                if (footprint == null) {
                    missingItems.add(item.itemNumber());
                    continue;
                }
                familyCases.merge(footprint.familyCode(), item.cases(), Integer::sum);
                totals.add(footprint.familyCode(), ceilDiv(item.cases(), footprint.casesPerPallet()));
            }

            String loadDisplay = String.join(", ", loadNumbers);
            return new RailCarCard(
                    trainId,
                    firstRow.sequence(),
                    firstRow.vehicleId(),
                    loadDisplay,
                    routeHeader(firstRow, loadDisplay),
                    itemLines,
                    totals.can,
                    totals.dom,
                    totals.kev,
                    topFamilies(familyCases),
                    new ArrayList<>(missingItems)
            );
        }

        private static String routeHeader(RailStopRecord row, String loadDisplay) {
            List<String> parts = new ArrayList<>();
            if (!row.trainNumber().isBlank()) {
                parts.add(row.trainNumber());
            }
            if (!row.warehouse().isBlank()) {
                parts.add(row.warehouse());
            }
            if (!loadDisplay.isBlank()) {
                parts.add(loadDisplay);
            }
            return String.join(" ", parts);
        }

        private static List<String> topFamilies(TreeMap<String, Integer> familyCases) {
            int totalCases = familyCases.values().stream().mapToInt(Integer::intValue).sum();
            if (totalCases <= 0) {
                return List.of();
            }
            return familyCases.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(3)
                    .map(entry -> entry.getKey() + ":" + Math.round(entry.getValue() * 100.0 / totalCases))
                    .toList();
        }

        private static int ceilDiv(int numerator, int denominator) {
            return (numerator + denominator - 1) / denominator;
        }
    }

    private static final class PalletTotals {
        private int can;
        private int dom;
        private int kev;

        private void add(String familyCode, int pallets) {
            if (familyCode.contains("CAN")) {
                can += pallets;
            } else if (familyCode.contains("KEV")) {
                kev += pallets;
            } else {
                dom += pallets;
            }
        }
    }
}

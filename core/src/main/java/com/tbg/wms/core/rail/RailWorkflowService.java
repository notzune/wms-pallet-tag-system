/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.*;
import java.util.stream.Collectors;

/**
 * End-to-end rail planning workflow:
 * train -> WMS rows -> railcar aggregates -> pallet totals -> printable cards.
 */
public final class RailWorkflowService {
    private final RailDbRepository repository;
    private final RailAggregationService aggregationService;
    private final RailPalletCalculator palletCalculator;
    private final RailLabelPlanner labelPlanner;

    public RailWorkflowService(RailDbRepository repository) {
        this(repository, new RailAggregationService(), new RailPalletCalculator(), new RailLabelPlanner());
    }

    RailWorkflowService(RailDbRepository repository,
                        RailAggregationService aggregationService,
                        RailPalletCalculator palletCalculator,
                        RailLabelPlanner labelPlanner) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.aggregationService = Objects.requireNonNull(aggregationService, "aggregationService cannot be null");
        this.palletCalculator = Objects.requireNonNull(palletCalculator, "palletCalculator cannot be null");
        this.labelPlanner = Objects.requireNonNull(labelPlanner, "labelPlanner cannot be null");
    }

    /**
     * Builds print-ready cards for a train.
     *
     * @param trainId train identifier
     * @return deterministic workflow result
     */
    public RailWorkflowResult prepare(String trainId) {
        String normalizedTrainId = normalizeTrainId(trainId);

        List<RailStopRecord> rawRows = repository.findRailStopsByTrainId(normalizedTrainId);
        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("No rail rows found for train: " + normalizedTrainId);
        }

        Set<String> shortCodes = collectShortCodes(rawRows);
        Map<String, List<RailFootprintCandidate>> candidates =
                repository.findRailFootprintsByShortCode(new ArrayList<>(shortCodes));
        Map<String, RailFamilyFootprint> resolvedFootprints = resolveFootprints(candidates);

        List<RailCarAggregate> aggregates = aggregationService.aggregateByRailcar(rawRows);
        List<RailCarCard> cards = new ArrayList<>(aggregates.size());
        Set<String> missingFromPlanner = new TreeSet<>();
        for (RailCarAggregate aggregate : aggregates) {
            cards.add(buildCard(normalizedTrainId, aggregate, resolvedFootprints, missingFromPlanner));
        }

        Set<String> unresolvedFootprints = new TreeSet<>(shortCodes);
        unresolvedFootprints.removeAll(resolvedFootprints.keySet());

        return new RailWorkflowResult(
                normalizedTrainId,
                rawRows,
                aggregates,
                cards,
                resolvedFootprints,
                unresolvedFootprints,
                missingFromPlanner
        );
    }

    private RailCarCard buildCard(String trainId,
                                  RailCarAggregate aggregate,
                                  Map<String, RailFamilyFootprint> footprints,
                                  Set<String> missingFromPlanner) {
        RailPalletCalculator.RailPalletResult palletResult = palletCalculator.calculate(aggregate, footprints);
        missingFromPlanner.addAll(palletResult.getMissingItems());

        RailStopRecord flattened = new RailStopRecord(
                aggregate.getDate(),
                aggregate.getSequence(),
                aggregate.getTrainNumber(),
                aggregate.getVehicleId(),
                aggregate.getWarehouse(),
                aggregate.getLoadNumberDisplay(),
                aggregate.getSortedItemsByCasesDesc()
        );
        RailLabelPlanner.PlannedRailLabel planned = labelPlanner.plan(List.of(flattened), footprints).get(0);
        List<String> families = planned.getTopFamilies().stream()
                .map(family -> family.getFamilyCode() + ":" + family.getPercent())
                .collect(Collectors.toList());

        return new RailCarCard(
                trainId,
                aggregate.getSequence(),
                aggregate.getVehicleId(),
                aggregate.getLoadNumberDisplay(),
                aggregate.getSortedItemsByCasesDesc(),
                palletResult.getCanPallets(),
                palletResult.getDomPallets(),
                families,
                palletResult.getMissingItems()
        );
    }

    private String normalizeTrainId(String trainId) {
        if (trainId == null || trainId.trim().isEmpty()) {
            throw new IllegalArgumentException("Train ID is required.");
        }
        return trainId.trim().toUpperCase(Locale.ROOT);
    }

    private Set<String> collectShortCodes(List<RailStopRecord> rows) {
        Set<String> codes = new TreeSet<>();
        for (RailStopRecord row : rows) {
            for (RailStopRecord.ItemQuantity item : row.getItems()) {
                if (item != null && item.isValid()) {
                    codes.add(item.getItemNumber());
                }
            }
        }
        return codes;
    }

    private Map<String, RailFamilyFootprint> resolveFootprints(Map<String, List<RailFootprintCandidate>> candidates) {
        Map<String, RailFamilyFootprint> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, List<RailFootprintCandidate>> entry : candidates.entrySet()) {
            List<RailFootprintCandidate> rows = entry.getValue();
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            RailFootprintCandidate first = rows.get(0);
            boolean consistent = true;
            for (int i = 1; i < rows.size(); i++) {
                RailFootprintCandidate current = rows.get(i);
                if (!first.getFamilyCode().equals(current.getFamilyCode())
                        || first.getCasesPerPallet() != current.getCasesPerPallet()) {
                    consistent = false;
                    break;
                }
            }
            if (!consistent) {
                continue;
            }
            resolved.put(entry.getKey(),
                    new RailFamilyFootprint(entry.getKey(), first.getFamilyCode(), first.getCasesPerPallet()));
        }
        return Collections.unmodifiableMap(resolved);
    }

    /**
     * Immutable workflow output for CLI/GUI.
     */
    public static final class RailWorkflowResult {
        private final String trainId;
        private final List<RailStopRecord> rawRows;
        private final List<RailCarAggregate> aggregates;
        private final List<RailCarCard> cards;
        private final Map<String, RailFamilyFootprint> resolvedFootprints;
        private final Set<String> unresolvedShortCodes;
        private final Set<String> missingItemsInCards;

        private RailWorkflowResult(String trainId,
                                   List<RailStopRecord> rawRows,
                                   List<RailCarAggregate> aggregates,
                                   List<RailCarCard> cards,
                                   Map<String, RailFamilyFootprint> resolvedFootprints,
                                   Set<String> unresolvedShortCodes,
                                   Set<String> missingItemsInCards) {
            this.trainId = trainId;
            this.rawRows = Collections.unmodifiableList(new ArrayList<>(rawRows));
            this.aggregates = Collections.unmodifiableList(new ArrayList<>(aggregates));
            this.cards = Collections.unmodifiableList(new ArrayList<>(cards));
            this.resolvedFootprints = resolvedFootprints;
            this.unresolvedShortCodes = Collections.unmodifiableSet(new TreeSet<>(unresolvedShortCodes));
            this.missingItemsInCards = Collections.unmodifiableSet(new TreeSet<>(missingItemsInCards));
        }

        public String getTrainId() {
            return trainId;
        }

        public List<RailStopRecord> getRawRows() {
            return rawRows;
        }

        public List<RailCarAggregate> getAggregates() {
            return aggregates;
        }

        public List<RailCarCard> getCards() {
            return cards;
        }

        public Map<String, RailFamilyFootprint> getResolvedFootprints() {
            return resolvedFootprints;
        }

        public Set<String> getUnresolvedShortCodes() {
            return unresolvedShortCodes;
        }

        public Set<String> getMissingItemsInCards() {
            return missingItemsInCards;
        }
    }
}

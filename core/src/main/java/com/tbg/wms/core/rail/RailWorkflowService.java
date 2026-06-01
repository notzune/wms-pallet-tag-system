/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.*;

/**
 * End-to-end rail planning workflow:
 * train -> WMS rows -> railcar aggregates -> pallet totals -> printable cards.
 */
public final class RailWorkflowService {
    private final RailDbRepository repository;
    private final RailAggregationService aggregationService;
    private final RailCardPlanningSupport cardPlanningSupport;
    private final RailFootprintResolver footprintResolver;
    private final RailConsistMarkerSupport consistMarkerSupport;

    public RailWorkflowService(RailDbRepository repository) {
        this(
                repository,
                new RailAggregationService(),
                new RailCardPlanningSupport(),
                new RailFootprintResolver(),
                new RailConsistMarkerSupport()
        );
    }

    RailWorkflowService(RailDbRepository repository,
                        RailAggregationService aggregationService,
                        RailCardPlanningSupport cardPlanningSupport,
                        RailFootprintResolver footprintResolver,
                        RailConsistMarkerSupport consistMarkerSupport) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.aggregationService = Objects.requireNonNull(aggregationService, "aggregationService cannot be null");
        this.cardPlanningSupport = Objects.requireNonNull(cardPlanningSupport, "cardPlanningSupport cannot be null");
        this.footprintResolver = Objects.requireNonNull(footprintResolver, "footprintResolver cannot be null");
        this.consistMarkerSupport = Objects.requireNonNull(consistMarkerSupport, "consistMarkerSupport cannot be null");
    }

    /**
     * Builds print-ready cards for a train.
     *
     * @param trainId train identifier
     * @return deterministic workflow result
     */
    public RailWorkflowResult prepare(String trainId) {
        return prepare(trainId, "");
    }

    /**
     * Builds print-ready cards for a train with a caller-selected label date.
     *
     * @param trainId    train identifier
     * @param labelDate  optional operator-selected date shown on labels
     * @return deterministic workflow result
     */
    public RailWorkflowResult prepare(String trainId, String labelDate) {
        String normalizedTrainId = normalizeTrainId(trainId);
        String normalizedLabelDate = normalize(labelDate);

        List<RailStopRecord> rawRows = repository.findRailStopsByTrainId(normalizedTrainId);
        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("No rail rows found for train: " + normalizedTrainId);
        }

        List<String> shortCodes = collectShortCodes(rawRows);
        Map<String, List<RailFootprintCandidate>> candidates =
                repository.findRailFootprintsByShortCode(shortCodes);
        Map<String, RailFamilyFootprint> resolvedFootprints = footprintResolver.resolve(candidates);

        List<RailCarAggregate> aggregates = aggregationService.aggregateByRailcar(rawRows);
        List<RailCarCard> cards = new ArrayList<>(aggregates.size());
        Set<String> missingFromPlanner = new TreeSet<>();
        for (RailCarAggregate aggregate : aggregates) {
            cards.add(buildCard(normalizedTrainId, normalizedLabelDate, aggregate, resolvedFootprints, missingFromPlanner));
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

    /**
     * Builds print-ready cards for multiple trains in the caller-provided order.
     *
     * @param trainIds train identifiers
     * @return deterministic combined workflow result
     */
    public RailWorkflowBatchResult prepareAll(List<String> trainIds) {
        return prepareAll(trainIds, "");
    }

    /**
     * Builds print-ready cards for multiple trains using one caller-selected label date.
     *
     * @param trainIds  train identifiers
     * @param labelDate optional operator-selected date shown on labels
     * @return deterministic combined workflow result
     */
    public RailWorkflowBatchResult prepareAll(List<String> trainIds, String labelDate) {
        if (trainIds == null || trainIds.isEmpty()) {
            throw new IllegalArgumentException("At least one train ID is required.");
        }
        String normalizedLabelDate = normalize(labelDate);

        List<String> normalizedTrainIds = new ArrayList<>(trainIds.size());
        List<RailWorkflowResult> results = new ArrayList<>(trainIds.size());
        List<RailStopRecord> rawRows = new ArrayList<>();
        List<RailCarAggregate> aggregates = new ArrayList<>();
        List<RailCarCard> cards = new ArrayList<>();
        Map<String, RailFamilyFootprint> resolvedFootprints = new LinkedHashMap<>();
        Set<String> unresolvedShortCodes = new TreeSet<>();
        Set<String> missingItemsInCards = new TreeSet<>();

        for (String trainId : trainIds) {
            String normalizedTrainId = normalizeTrainId(trainId);
            RailWorkflowResult result = prepare(normalizedTrainId, normalizedLabelDate);
            normalizedTrainIds.add(normalizedTrainId);
            results.add(result);
            rawRows.addAll(result.getRawRows());
            aggregates.addAll(result.getAggregates());
            cards.addAll(result.getCards());
            resolvedFootprints.putAll(result.getResolvedFootprints());
            unresolvedShortCodes.addAll(result.getUnresolvedShortCodes());
            missingItemsInCards.addAll(result.getMissingItemsInCards());
        }

        return new RailWorkflowBatchResult(
                normalizedTrainIds,
                results,
                rawRows,
                aggregates,
                cards,
                resolvedFootprints,
                unresolvedShortCodes,
                missingItemsInCards
        );
    }

    private RailCarCard buildCard(String trainId,
                                  String labelDate,
                                  RailCarAggregate aggregate,
                                  Map<String, RailFamilyFootprint> footprints,
                                  Set<String> missingFromPlanner) {
        RailCardPlanningSupport.RailCardPlan cardPlan = cardPlanningSupport.plan(aggregate, footprints);
        missingFromPlanner.addAll(cardPlan.missingItems());
        List<RailStopRecord.ItemQuantity> sortedItems = aggregate.getSortedItemsByCasesDesc();

        return new RailCarCard(
                trainId,
                aggregate.getSequence(),
                aggregate.getVehicleId(),
                aggregate.getLoadNumberDisplay(),
                routeHeader(aggregate),
                effectiveLabelDate(labelDate, aggregate),
                consistMarkerSupport.buildMarker(sortedItems, footprints),
                sortedItems,
                cardPlan.canPallets(),
                cardPlan.domPallets(),
                cardPlan.kevPallets(),
                cardPlan.topFamilies(),
                cardPlan.missingItems()
        );
    }

    private String routeHeader(RailCarAggregate aggregate) {
        List<String> parts = new ArrayList<>(3);
        if (!aggregate.getTrainNumber().isBlank()) {
            parts.add(aggregate.getTrainNumber());
        }
        if (!aggregate.getWarehouse().isBlank()) {
            parts.add(aggregate.getWarehouse());
        }
        if (!aggregate.getLoadNumberDisplay().isBlank()) {
            parts.add(aggregate.getLoadNumberDisplay());
        }
        return String.join(" ", parts);
    }

    private String effectiveLabelDate(String labelDate, RailCarAggregate aggregate) {
        return labelDate.isBlank() ? aggregate.getDate() : labelDate;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeTrainId(String trainId) {
        if (trainId == null || trainId.trim().isEmpty()) {
            throw new IllegalArgumentException("Train ID is required.");
        }
        return trainId.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> collectShortCodes(List<RailStopRecord> rows) {
        Set<String> codes = new LinkedHashSet<>();
        for (RailStopRecord row : rows) {
            for (RailStopRecord.ItemQuantity item : row.getItems()) {
                if (item != null && item.isValid()) {
                    codes.add(item.getItemNumber());
                }
            }
        }
        return new ArrayList<>(codes);
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
            this.resolvedFootprints = Collections.unmodifiableMap(new LinkedHashMap<>(resolvedFootprints));
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

    /**
     * Immutable combined output for multi-train rail label preparation.
     */
    public static final class RailWorkflowBatchResult {
        private final List<String> trainIds;
        private final List<RailWorkflowResult> results;
        private final List<RailStopRecord> rawRows;
        private final List<RailCarAggregate> aggregates;
        private final List<RailCarCard> cards;
        private final Map<String, RailFamilyFootprint> resolvedFootprints;
        private final Set<String> unresolvedShortCodes;
        private final Set<String> missingItemsInCards;

        private RailWorkflowBatchResult(List<String> trainIds,
                                        List<RailWorkflowResult> results,
                                        List<RailStopRecord> rawRows,
                                        List<RailCarAggregate> aggregates,
                                        List<RailCarCard> cards,
                                        Map<String, RailFamilyFootprint> resolvedFootprints,
                                        Set<String> unresolvedShortCodes,
                                        Set<String> missingItemsInCards) {
            this.trainIds = Collections.unmodifiableList(new ArrayList<>(trainIds));
            this.results = Collections.unmodifiableList(new ArrayList<>(results));
            this.rawRows = Collections.unmodifiableList(new ArrayList<>(rawRows));
            this.aggregates = Collections.unmodifiableList(new ArrayList<>(aggregates));
            this.cards = Collections.unmodifiableList(new ArrayList<>(cards));
            this.resolvedFootprints = Collections.unmodifiableMap(new LinkedHashMap<>(resolvedFootprints));
            this.unresolvedShortCodes = Collections.unmodifiableSet(new TreeSet<>(unresolvedShortCodes));
            this.missingItemsInCards = Collections.unmodifiableSet(new TreeSet<>(missingItemsInCards));
        }

        public List<String> getTrainIds() {
            return trainIds;
        }

        public List<RailWorkflowResult> getResults() {
            return results;
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

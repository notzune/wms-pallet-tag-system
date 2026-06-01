/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the rail consist marker printed under the route header.
 */
public final class RailConsistMarkerSupport {
    private final RailFamilyClassifier classifier;

    public RailConsistMarkerSupport() {
        this(new RailFamilyClassifier());
    }

    RailConsistMarkerSupport(RailFamilyClassifier classifier) {
        this.classifier = Objects.requireNonNull(classifier, "classifier cannot be null");
    }

    /**
     * Counts displayed card line items by resolved WMS family.
     *
     * @param items      item lines shown on the card
     * @param footprints resolved item family lookup
     * @return compact marker such as {@code D-4 C-3}
     */
    public String buildMarker(List<RailStopRecord.ItemQuantity> items,
                              Map<String, RailFamilyFootprint> footprints) {
        Objects.requireNonNull(items, "items cannot be null");
        Objects.requireNonNull(footprints, "footprints cannot be null");
        Map<RailFamilyClassifier.FamilyBucket, Integer> counts =
                new EnumMap<>(RailFamilyClassifier.FamilyBucket.class);

        for (RailStopRecord.ItemQuantity item : items) {
            if (item == null || !item.isValid()) {
                continue;
            }
            RailFamilyFootprint footprint = footprints.get(item.getItemNumber());
            if (footprint == null || !footprint.isValid()) {
                continue;
            }
            RailFamilyClassifier.FamilyBucket bucket = classifier.classify(footprint.getFamilyCode(), item.getItemNumber());
            counts.merge(bucket, 1, Integer::sum);
        }

        return String.join(" ",
                markerPart("D", counts.get(RailFamilyClassifier.FamilyBucket.DOM)),
                markerPart("C", counts.get(RailFamilyClassifier.FamilyBucket.CAN)),
                markerPart("K", counts.get(RailFamilyClassifier.FamilyBucket.KEV)),
                markerPart("CC", counts.get(RailFamilyClassifier.FamilyBucket.CLUB))
        ).trim().replaceAll(" +", " ");
    }

    private String markerPart(String prefix, Integer count) {
        return count == null || count <= 0 ? "" : prefix + "-" + count;
    }
}

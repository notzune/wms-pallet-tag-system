package com.tbg.wms.core.sscc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Groups raw SSCC rows into final label payloads.
 */
public final class SsccLabelPlanner {
    public List<SsccLabelGroup> groupRows(List<SsccLabelRow> rows) {
        Objects.requireNonNull(rows, "rows cannot be null");
        Map<String, MutableGroup> groups = new LinkedHashMap<>();
        for (SsccLabelRow row : rows) {
            if (row == null || !row.hasLabelIdentity()) {
                continue;
            }
            String key = row.salesOrder() + "|" + row.newReceivedLpn();
            MutableGroup group = groups.computeIfAbsent(key, ignored -> new MutableGroup(row));
            group.add(row);
        }

        return groups.values().stream()
                .map(MutableGroup::toGroup)
                .sorted(Comparator.comparing(SsccLabelGroup::salesOrder).thenComparing(SsccLabelGroup::newReceivedLpn))
                .toList();
    }

    private static final class MutableGroup {
        private final SsccLabelRow seed;
        private double totalCases;
        private final TreeSet<String> items = new TreeSet<>();
        private final TreeSet<String> level2ReferenceCodes = new TreeSet<>();

        private MutableGroup(SsccLabelRow seed) {
            this.seed = seed;
        }

        private void add(SsccLabelRow row) {
            totalCases += row.sumOfShipCases();
            if (!row.itemNumber().isBlank()) {
                items.add(row.itemNumber());
            }
            if (!row.level2Reference().isBlank()) {
                level2ReferenceCodes.add(row.level2Reference());
            }
        }

        private SsccLabelGroup toGroup() {
            return new SsccLabelGroup(
                    seed.salesOrder(),
                    seed.purchaseOrder(),
                    seed.shipment(),
                    seed.carrierCode(),
                    seed.trailerId(),
                    seed.destination(),
                    seed.destinationAddress(),
                    seed.customerName(),
                    seed.facility(),
                    seed.newReceivedLpn(),
                    totalCases,
                    new ArrayList<>(items),
                    new ArrayList<>(level2ReferenceCodes)
            );
        }
    }
}

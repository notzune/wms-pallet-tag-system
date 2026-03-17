package com.tbg.wms.core.label;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared helpers for resolving ordered label selections from operator input.
 */
public final class LabelSelectionSupport {

    private LabelSelectionSupport() {
    }

    public static List<Integer> parseOneBasedSelection(String expression, int availableCount) {
        if (availableCount <= 0) {
            throw new IllegalArgumentException("No labels are available for selection.");
        }

        String normalized = expression == null ? "" : expression.trim();
        if (normalized.isEmpty() || normalized.equalsIgnoreCase("all")) {
            List<Integer> indexes = new ArrayList<>(availableCount);
            for (int i = 1; i <= availableCount; i++) {
                indexes.add(i);
            }
            return indexes;
        }

        Set<Integer> selected = new LinkedHashSet<>();
        String[] tokens = normalized.split(",");
        for (String token : tokens) {
            String part = token.trim();
            if (part.isEmpty()) {
                continue;
            }
            int dash = part.indexOf('-');
            if (dash >= 0) {
                int start = parseIndex(part.substring(0, dash), availableCount);
                int end = parseIndex(part.substring(dash + 1), availableCount);
                if (end < start) {
                    throw new IllegalArgumentException("Invalid label range: " + part);
                }
                for (int i = start; i <= end; i++) {
                    selected.add(i);
                }
            } else {
                selected.add(parseIndex(part, availableCount));
            }
        }

        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label.");
        }

        List<Integer> ordered = new ArrayList<>(selected);
        ordered.sort(Integer::compareTo);
        return ordered;
    }

    public static <T> List<T> selectByOneBasedIndexes(List<T> availableItems, List<Integer> selectedIndexes) {
        Objects.requireNonNull(availableItems, "availableItems cannot be null");
        Objects.requireNonNull(selectedIndexes, "selectedIndexes cannot be null");
        if (selectedIndexes.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label.");
        }

        List<T> selected = new ArrayList<>(selectedIndexes.size());
        for (Integer index : selectedIndexes) {
            if (index == null || index < 1 || index > availableItems.size()) {
                throw new IllegalArgumentException("Label index out of range: " + index);
            }
            selected.add(availableItems.get(index - 1));
        }
        return selected;
    }

    private static int parseIndex(String token, int availableCount) {
        String value = token == null ? "" : token.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty label index.");
        }
        try {
            int index = Integer.parseInt(value);
            if (index < 1 || index > availableCount) {
                throw new IllegalArgumentException("Label index out of range: " + index);
            }
            return index;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid label selector: " + value, ex);
        }
    }
}

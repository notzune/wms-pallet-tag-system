package com.tbg.wms.v2.app.rail;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ParseRailTrainInput {
    private static final Pattern DELIMITERS = Pattern.compile("[,\\s:/;]+");

    public List<String> parse(String input) {
        Set<String> trainIds = new LinkedHashSet<>();
        for (String token : DELIMITERS.split(input == null ? "" : input)) {
            String trainId = token.trim().toUpperCase(Locale.ROOT);
            if (!trainId.isBlank()) {
                trainIds.add(trainId);
            }
        }
        if (trainIds.isEmpty()) {
            throw new IllegalArgumentException("At least one train ID is required.");
        }
        return new ArrayList<>(trainIds);
    }
}

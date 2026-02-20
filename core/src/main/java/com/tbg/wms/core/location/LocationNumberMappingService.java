/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.2.2
 */

package com.tbg.wms.core.location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads sold-to to DC location mappings from CSV.
 *
 * <p>CSV format:</p>
 * <pre>
 * Sold-To Name,Location #,Sold-To #
 * WAL-MART CANADA 7087R,7087R,0100003434
 * </pre>
 */
public final class LocationNumberMappingService {

    private static final Logger log = LoggerFactory.getLogger(LocationNumberMappingService.class);

    private final Map<String, String> dcBySoldToKey = new HashMap<>();

    /**
     * Creates a mapping service from the provided matrix CSV.
     *
     * @param csvFile matrix file path
     * @throws IOException if the file cannot be read
     */
    public LocationNumberMappingService(Path csvFile) throws IOException {
        Objects.requireNonNull(csvFile, "csvFile cannot be null");
        load(csvFile);
        log.info("Loaded {} sold-to location mappings from {}", dcBySoldToKey.size(), csvFile);
    }

    /**
     * Resolves sold-to value to mapped DC number, or returns original when no mapping exists.
     *
     * @param soldToOrLocation sold-to value (e.g., {@code C100003434}) or already a DC code
     * @return mapped DC location (e.g., {@code 7087R}) or original trimmed value
     */
    public String resolveDcLocation(String soldToOrLocation) {
        if (soldToOrLocation == null || soldToOrLocation.isBlank()) {
            return soldToOrLocation;
        }

        String input = soldToOrLocation.trim();
        String key = canonicalSoldToKey(input);
        if (key == null) {
            return input;
        }

        String mapped = dcBySoldToKey.get(key);
        return mapped == null || mapped.isBlank() ? input : mapped;
    }

    private void load(Path csvFile) throws IOException {
        if (!Files.isRegularFile(csvFile)) {
            throw new IllegalArgumentException("Location matrix CSV not found: " + csvFile);
        }

        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 || line.isBlank()) {
                    continue;
                }
                parseLine(line, lineNum);
            }
        }
    }

    private void parseLine(String line, int lineNum) {
        String[] fields = line.split(",", 3);
        if (fields.length < 3) {
            log.warn("Skipping location matrix line {} - insufficient fields: {}", lineNum, line);
            return;
        }

        String locationNumber = fields[1].trim();
        String soldToNumber = fields[2].trim();
        if (locationNumber.isEmpty() || soldToNumber.isEmpty()) {
            log.warn("Skipping location matrix line {} - missing location/sold-to: {}", lineNum, line);
            return;
        }

        String key = canonicalSoldToKey(soldToNumber);
        if (key == null) {
            log.warn("Skipping location matrix line {} - sold-to has no digits: {}", lineNum, soldToNumber);
            return;
        }

        dcBySoldToKey.put(key, locationNumber);
    }

    private static String canonicalSoldToKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim().toUpperCase();
        if (trimmed.startsWith("C")) {
            trimmed = trimmed.substring(1);
        }

        StringBuilder digits = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return null;
        }

        int nonZero = 0;
        while (nonZero < digits.length() && digits.charAt(nonZero) == '0') {
            nonZero++;
        }
        return nonZero == digits.length() ? "0" : digits.substring(nonZero);
    }
}

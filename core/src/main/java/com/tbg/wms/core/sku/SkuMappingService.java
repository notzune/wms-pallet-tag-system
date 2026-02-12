/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.sku;

import com.tbg.wms.core.model.WalmartSkuMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Service for loading and accessing Walmart SKU mappings from CSV.
 *
 * This service loads a CSV file mapping TBG internal SKU numbers to Walmart
 * item numbers. It provides O(1) lookup by either TBG SKU or Walmart item number.
 *
 * CSV Format (4 columns):
 *   TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU
 *   205641,30081705,1.36L PL 1/6 NJ STRW BAN,1.36L PL 1/6 NJ STRW BAN
 *   ...
 */
public final class SkuMappingService {

    private static final Logger log = LoggerFactory.getLogger(SkuMappingService.class);

    // Map: TBG SKU# → WalmartSkuMapping
    private final Map<String, WalmartSkuMapping> mappingByTbgSku;

    // Map: Walmart Item# → WalmartSkuMapping (reverse lookup)
    private final Map<String, WalmartSkuMapping> mappingByWalmartItem;

    /**
     * Creates a new SkuMappingService and loads mappings from CSV file.
     *
     * @param csvFile path to the CSV file
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if CSV format is invalid
     */
    public SkuMappingService(Path csvFile) throws IOException {
        Objects.requireNonNull(csvFile, "csvFile cannot be null");

        this.mappingByTbgSku = new HashMap<>();
        this.mappingByWalmartItem = new HashMap<>();

        loadMappingsFromCsv(csvFile);
        log.info("Loaded {} SKU mappings from {}", mappingByTbgSku.size(), csvFile);
    }

    /**
     * Looks up Walmart mapping by TBG SKU number.
     *
     * @param tbgSku TBG internal SKU (e.g., "205641")
     * @return WalmartSkuMapping, or null if not found
     */
    public WalmartSkuMapping findByTbgSku(String tbgSku) {
        if (tbgSku == null || tbgSku.trim().isEmpty()) {
            return null;
        }
        return mappingByTbgSku.get(tbgSku.trim());
    }

    /**
     * Looks up Walmart mapping by Walmart item number.
     *
     * @param walmartItemNo Walmart item number (e.g., "30081705")
     * @return WalmartSkuMapping, or null if not found
     */
    public WalmartSkuMapping findByWalmartItem(String walmartItemNo) {
        if (walmartItemNo == null || walmartItemNo.trim().isEmpty()) {
            return null;
        }
        return mappingByWalmartItem.get(walmartItemNo.trim());
    }

    /**
     * Attempts to find mapping by extracting TBG SKU from a database PRTNUM.
     *
     * PRTNUM format from database is 17 digits (e.g., "10048500019792000").
     * CSV uses short TBG SKU (5-6 digits, e.g., "205641").
     *
     * This method tries multiple extraction strategies:
     * 1. Last 6 digits of PRTNUM
     * 2. Middle portion (digits 5-11)
     * 3. Direct match if PRTNUM already is 5-6 digits
     *
     * @param prtnum the database PRTNUM (17 digits)
     * @return WalmartSkuMapping, or null if extraction fails
     */
    public WalmartSkuMapping findByPrtnum(String prtnum) {
        if (prtnum == null || prtnum.trim().isEmpty()) {
            return null;
        }

        prtnum = prtnum.trim();

        // Strategy 1: Try direct match (in case it's already short format)
        WalmartSkuMapping mapping = findByTbgSku(prtnum);
        if (mapping != null) {
            return mapping;
        }

        // Strategy 2: Try last 6 digits
        if (prtnum.length() >= 6) {
            String last6 = prtnum.substring(prtnum.length() - 6);
            mapping = findByTbgSku(last6);
            if (mapping != null) {
                log.debug("Found mapping via last 6 digits of PRTNUM: {} → {}", prtnum, mapping.getWalmartItemNo());
                return mapping;
            }
        }

        // Strategy 3: Try middle portion (digits 5-11, 0-indexed: 4-10)
        if (prtnum.length() >= 11) {
            String middle = prtnum.substring(4, 10);
            mapping = findByTbgSku(middle);
            if (mapping != null) {
                log.debug("Found mapping via middle digits of PRTNUM: {} → {}", prtnum, mapping.getWalmartItemNo());
                return mapping;
            }
        }

        log.debug("No SKU mapping found for PRTNUM: {}", prtnum);
        return null;
    }

    /**
     * Gets the total number of loaded mappings.
     *
     * @return count of mappings
     */
    public int getMappingCount() {
        return mappingByTbgSku.size();
    }

    /**
     * Gets an immutable view of all mappings by TBG SKU (for testing/inspection).
     *
     * @return unmodifiable map
     */
    public Map<String, WalmartSkuMapping> getAllMappings() {
        return Collections.unmodifiableMap(mappingByTbgSku);
    }

    /**
     * Loads all mappings from CSV file.
     *
     * CSV Format:
     *   Line 1 (header): TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU
     *   Lines 2+: 205641,30081705,1.36L PL 1/6 NJ STRW BAN,1.36L PL 1/6 NJ STRW BAN
     *
     * @param csvFile path to CSV file
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if CSV format is invalid
     */
    private void loadMappingsFromCsv(Path csvFile) throws IOException {
        if (!Files.exists(csvFile)) {
            throw new IllegalArgumentException("CSV file not found: " + csvFile);
        }

        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;

                // Skip empty lines and header
                if (lineNum == 1 || line.trim().isEmpty()) {
                    continue;
                }

                parseCsvLine(line, lineNum);
            }
        }
    }

    /**
     * Parses a single CSV line and adds mapping.
     *
     * @param line CSV line with 4 comma-separated fields
     * @param lineNum line number (for error reporting)
     */
    private void parseCsvLine(String line, int lineNum) {
        String[] fields = line.split(",");

        if (fields.length < 2) {
            log.warn("Skipping line {} - insufficient fields: {}", lineNum, line);
            return;
        }

        String tbgSku = fields[0].trim();
        String walmartItemNo = fields[1].trim();
        String description = fields.length > 2 ? fields[2].trim() : "";

        if (tbgSku.isEmpty() || walmartItemNo.isEmpty()) {
            log.warn("Skipping line {} - empty SKU or Walmart item number", lineNum);
            return;
        }

        WalmartSkuMapping mapping = new WalmartSkuMapping(tbgSku, walmartItemNo, description);

        // Store in both maps
        mappingByTbgSku.put(tbgSku, mapping);
        mappingByWalmartItem.put(walmartItemNo, mapping);

        log.trace("Loaded SKU mapping: {} → {} ({})", tbgSku, walmartItemNo, description);
    }

    @Override
    public String toString() {
        return "SkuMappingService{" +
                "mappingCount=" + mappingByTbgSku.size() +
                '}';
    }
}


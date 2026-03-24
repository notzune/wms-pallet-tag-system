/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.sku;

import com.tbg.wms.core.model.WalmartSkuMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

/**
 * Resolves Walmart SKU mappings from mixed-format Oracle PRTNUM values.
 */
final class SkuPrtnumLookupSupport {

    private static final Logger log = LoggerFactory.getLogger(SkuPrtnumLookupSupport.class);
    private static final int MIN_SKU_LENGTH = 5;

    Optional<WalmartSkuMapping> findByPrtnum(
            String normalizedPrtnum,
            Function<String, WalmartSkuMapping> mappingLookup
    ) {
        WalmartSkuMapping mapping = mappingLookup.apply(normalizedPrtnum);
        if (mapping != null) {
            return Optional.of(mapping);
        }

        String digits = extractDigits(normalizedPrtnum);
        if (!digits.isEmpty()) {
            for (int len = digits.length(); len >= MIN_SKU_LENGTH; len--) {
                for (int i = 0; i <= digits.length() - len; i++) {
                    String candidate = digits.substring(i, i + len);

                    mapping = mappingLookup.apply(candidate);
                    if (mapping != null) {
                        log.debug("Found mapping via embedded PRTNUM segment: {} -> {}",
                                normalizedPrtnum, mapping.getWalmartItemNo());
                        return Optional.of(mapping);
                    }

                    String noLeadingZeros = trimLeadingZeros(candidate);
                    if (!noLeadingZeros.equals(candidate)) {
                        mapping = mappingLookup.apply(noLeadingZeros);
                        if (mapping != null) {
                            log.debug("Found mapping via zero-trimmed PRTNUM segment: {} -> {}",
                                    normalizedPrtnum, mapping.getWalmartItemNo());
                            return Optional.of(mapping);
                        }
                    }
                }
            }
        }

        log.debug("No SKU mapping found for PRTNUM: {}", normalizedPrtnum);
        return Optional.empty();
    }

    static String normalizeLookupKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String extractDigits(String value) {
        StringBuilder digits = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        return digits.toString();
    }

    private static String trimLeadingZeros(String value) {
        int i = 0;
        int max = value.length() - 1;
        while (i < max && value.charAt(i) == '0') {
            i++;
        }
        return i == 0 ? value : value.substring(i);
    }
}

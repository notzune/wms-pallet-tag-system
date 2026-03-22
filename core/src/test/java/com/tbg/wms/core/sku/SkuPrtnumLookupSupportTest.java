/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.sku;

import com.tbg.wms.core.model.WalmartSkuMapping;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkuPrtnumLookupSupportTest {

    private final SkuPrtnumLookupSupport support = new SkuPrtnumLookupSupport();

    @Test
    void normalizeLookupKeyTrimsAndRejectsBlankValues() {
        assertEquals("205641", SkuPrtnumLookupSupport.normalizeLookupKey(" 205641 "));
        assertNull(SkuPrtnumLookupSupport.normalizeLookupKey("   "));
        assertNull(SkuPrtnumLookupSupport.normalizeLookupKey(null));
    }

    @Test
    void findByPrtnumSupportsEmbeddedAndZeroTrimmedCandidates() {
        WalmartSkuMapping mapping = new WalmartSkuMapping("205641", "30081705", "desc");
        Map<String, WalmartSkuMapping> mappings = Map.of("205641", mapping);

        Optional<WalmartSkuMapping> found = support.findByPrtnum("1004850000205641000", mappings::get);

        assertTrue(found.isPresent());
        assertEquals("30081705", found.orElseThrow().getWalmartItemNo());
    }
}

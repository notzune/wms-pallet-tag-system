/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailFamilyClassifierTest {

    private final RailFamilyClassifier classifier = new RailFamilyClassifier();

    @Test
    void classifyMapsKnownBuckets() {
        assertEquals(RailFamilyClassifier.FamilyBucket.CAN, classifier.classify("CAN"));
        assertEquals(RailFamilyClassifier.FamilyBucket.DOM, classifier.classify("DOM"));
        assertEquals(RailFamilyClassifier.FamilyBucket.KEV, classifier.classify("KEV"));
    }

    @Test
    void classifyHandlesCommonWmsVariants() {
        assertEquals(RailFamilyClassifier.FamilyBucket.CAN, classifier.classify("*CAN*"));
        assertEquals(RailFamilyClassifier.FamilyBucket.KEV, classifier.classify("kev"));
        assertEquals(RailFamilyClassifier.FamilyBucket.DOM, classifier.classify("Domestic"));
    }

    @Test
    void classifyDefaultsToDomForUnknownOrBlankValues() {
        assertEquals(RailFamilyClassifier.FamilyBucket.DOM, classifier.classify(null));
        assertEquals(RailFamilyClassifier.FamilyBucket.DOM, classifier.classify(""));
        assertEquals(RailFamilyClassifier.FamilyBucket.DOM, classifier.classify("XYZ"));
    }
}


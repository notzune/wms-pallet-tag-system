package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailLocaleNormalizationTest {

    @Test
    void footprintAndCandidateUseLocaleRootForUppercaseNormalization() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            RailFamilyFootprint footprint = new RailFamilyFootprint("01830", "idi", 56);
            RailFootprintCandidate candidate = new RailFootprintCandidate("01830", "ITEMA", "idi", 56);

            assertEquals("IDI", footprint.getFamilyCode());
            assertEquals("IDI", candidate.getFamilyCode());
        } finally {
            Locale.setDefault(previous);
        }
    }
}

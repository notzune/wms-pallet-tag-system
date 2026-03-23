package com.tbg.wms.cli.gui.analyzers.unpicked;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnpickedPartialsRuleClassifierTest {

    @Test
    void classify_shouldMapKnownSoldToNamesToRuleBuckets() {
        UnpickedPartialsRuleClassifier classifier = new UnpickedPartialsRuleClassifier();

        assertEquals(UnpickedPartialsRule.LOBLAWS, classifier.classify("LOBLAWS DC 67", "LOBLAWS"));
        assertEquals(UnpickedPartialsRule.METRO, classifier.classify("Metro Richelieu", "METRO"));
        assertEquals(UnpickedPartialsRule.WALMART, classifier.classify("WALMART DC #7014", "WALMART"));
        assertEquals(UnpickedPartialsRule.DEFAULT, classifier.classify("Restaurant Depot", "#N/A"));
    }
}

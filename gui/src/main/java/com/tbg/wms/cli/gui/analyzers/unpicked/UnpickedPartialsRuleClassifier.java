package com.tbg.wms.cli.gui.analyzers.unpicked;

import java.util.Locale;

public final class UnpickedPartialsRuleClassifier {

    public UnpickedPartialsRule classify(String soldToName, String customerAccount) {
        String soldTo = normalize(soldToName);
        String customer = normalize(customerAccount);
        String combined = soldTo + " " + customer;

        if (combined.contains("LOBLAWS")) {
            return UnpickedPartialsRule.LOBLAWS;
        }
        if (combined.contains("CORE") && combined.contains("MARK")) {
            return UnpickedPartialsRule.CORE_MARK;
        }
        if (combined.contains("MR") && combined.contains("DAIRY")) {
            return UnpickedPartialsRule.MR_DAIRY;
        }
        if (combined.contains("WALMART")) {
            return UnpickedPartialsRule.WALMART;
        }
        if (combined.contains("SOBEYS")) {
            return UnpickedPartialsRule.SOBEYS;
        }
        if (combined.contains("METRO")) {
            return UnpickedPartialsRule.METRO;
        }
        return UnpickedPartialsRule.DEFAULT;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

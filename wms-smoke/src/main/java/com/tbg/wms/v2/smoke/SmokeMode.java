package com.tbg.wms.v2.smoke;

public enum SmokeMode {
    REPO("repo"),
    PACKAGED("packaged");

    private final String wireName;

    SmokeMode(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}

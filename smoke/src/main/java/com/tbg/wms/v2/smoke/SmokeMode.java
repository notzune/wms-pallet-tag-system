package com.tbg.wms.v2.smoke;

/**
 * Enumerates supported smoke mode values for WMS 2.0 workflows.
 */
public enum SmokeMode {
    REPO("repo"),
    PACKAGED("packaged");

    private final String wireName;

    SmokeMode(String wireName) {
        this.wireName = wireName;
    }

    /**
     * Returns the serialized name used in smoke metadata.
     *
     * @return command-line wire name
     */
    public String wireName() {
        return wireName;
    }
}

package com.tbg.wms.v2.oracle;

public record OracleSettings(
        String jdbcUrl,
        String username,
        String password,
        int maximumPoolSize,
        long connectionTimeoutMs,
        long validationTimeoutMs,
        String poolName,
        boolean readOnly,
        Integer minimumIdle,
        Long initializationFailTimeoutMs,
        Long leakDetectionThresholdMs
) {
    public OracleSettings {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("jdbcUrl is required.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required.");
        }
        if (maximumPoolSize < 1) {
            throw new IllegalArgumentException("maximumPoolSize must be positive.");
        }
        if (connectionTimeoutMs < 1) {
            throw new IllegalArgumentException("connectionTimeoutMs must be positive.");
        }
        if (validationTimeoutMs < 1) {
            throw new IllegalArgumentException("validationTimeoutMs must be positive.");
        }
        poolName = poolName == null || poolName.isBlank() ? "wms-v2-oracle" : poolName.trim();
    }
}

package com.tbg.wms.v2.oracle;

/**
 * Carries oracle settings data across WMS 2.0 module boundaries.
 *
 * @param jdbcUrl the jdbc url.
 * @param username the username.
 * @param password the password.
 * @param maximumPoolSize the maximum pool size.
 * @param connectionTimeoutMs the connection timeout ms.
 * @param validationTimeoutMs the validation timeout ms.
 * @param poolName the pool name.
 * @param readOnly the read only.
 * @param minimumIdle the minimum idle.
 * @param initializationFailTimeoutMs the initialization fail timeout ms.
 * @param leakDetectionThresholdMs the leak detection threshold ms.
 */
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

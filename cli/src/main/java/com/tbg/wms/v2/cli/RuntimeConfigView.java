package com.tbg.wms.v2.cli;

import java.nio.file.Path;
import java.util.List;

/**
 * Exposes runtime configuration values in a display-safe form for CLI output.
 *
 * @param siteCode configured site code
 * @param environment configured runtime environment
 * @param loadedConfigFiles configuration files loaded for this runtime
 * @param oracleJdbcUrl display-safe Oracle JDBC URL
 * @param oracleUsername display-safe Oracle username
 * @param oraclePassword Oracle password value, usually blank for display
 */
public record RuntimeConfigView(
        String siteCode,
        String environment,
        List<Path> loadedConfigFiles,
        String oracleJdbcUrl,
        String oracleUsername,
        String oraclePassword
) {
    public RuntimeConfigView {
        siteCode = defaultValue(siteCode, "TBG3002");
        environment = defaultValue(environment, "unknown");
        loadedConfigFiles = List.copyOf(loadedConfigFiles == null ? List.of() : loadedConfigFiles);
        oracleJdbcUrl = defaultValue(oracleJdbcUrl, "(not configured)");
        oracleUsername = defaultValue(oracleUsername, "(not configured)");
        oraclePassword = oraclePassword == null ? "" : oraclePassword;
    }

    /**
     * Creates a builder for the configuration view.
     *
     * @return mutable builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static final class Builder {
        private String siteCode;
        private String environment;
        private List<Path> loadedConfigFiles = List.of();
        private String oracleJdbcUrl;
        private String oracleUsername;
        private String oraclePassword;

        /**
         * Sets the site code.
         *
         * @param siteCode configured site code
         * @return this builder
         */
        public Builder siteCode(String siteCode) {
            this.siteCode = siteCode;
            return this;
        }

        /**
         * Sets the runtime environment.
         *
         * @param environment configured environment name
         * @return this builder
         */
        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Sets the list of loaded configuration files.
         *
         * @param loadedConfigFiles configuration files loaded for this runtime
         * @return this builder
         */
        public Builder loadedConfigFiles(List<Path> loadedConfigFiles) {
            this.loadedConfigFiles = loadedConfigFiles;
            return this;
        }

        /**
         * Sets the Oracle JDBC URL.
         *
         * @param oracleJdbcUrl configured Oracle JDBC URL
         * @return this builder
         */
        public Builder oracleJdbcUrl(String oracleJdbcUrl) {
            this.oracleJdbcUrl = oracleJdbcUrl;
            return this;
        }

        /**
         * Sets the Oracle username.
         *
         * @param oracleUsername configured Oracle username
         * @return this builder
         */
        public Builder oracleUsername(String oracleUsername) {
            this.oracleUsername = oracleUsername;
            return this;
        }

        /**
         * Sets the Oracle password value.
         *
         * @param oraclePassword configured Oracle password
         * @return this builder
         */
        public Builder oraclePassword(String oraclePassword) {
            this.oraclePassword = oraclePassword;
            return this;
        }

        /**
         * Builds the immutable configuration view.
         *
         * @return runtime configuration view
         */
        public RuntimeConfigView build() {
            return new RuntimeConfigView(
                    siteCode,
                    environment,
                    loadedConfigFiles,
                    oracleJdbcUrl,
                    oracleUsername,
                    oraclePassword
            );
        }
    }
}

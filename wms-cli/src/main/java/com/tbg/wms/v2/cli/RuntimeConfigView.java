package com.tbg.wms.v2.cli;

import java.nio.file.Path;
import java.util.List;

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

        public Builder siteCode(String siteCode) {
            this.siteCode = siteCode;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder loadedConfigFiles(List<Path> loadedConfigFiles) {
            this.loadedConfigFiles = loadedConfigFiles;
            return this;
        }

        public Builder oracleJdbcUrl(String oracleJdbcUrl) {
            this.oracleJdbcUrl = oracleJdbcUrl;
            return this;
        }

        public Builder oracleUsername(String oracleUsername) {
            this.oracleUsername = oracleUsername;
            return this;
        }

        public Builder oraclePassword(String oraclePassword) {
            this.oraclePassword = oraclePassword;
            return this;
        }

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

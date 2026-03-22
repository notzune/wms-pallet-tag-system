/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Encapsulates Oracle and WMS environment config resolution policy.
 */
final class OracleConnectionConfigSupport {
    private final Function<String, String> rawLookup;
    private final Function<String, String> envOrFileLookup;
    private final Function<String, String> requiredLookup;

    OracleConnectionConfigSupport(
            Function<String, String> rawLookup,
            Function<String, String> envOrFileLookup,
            Function<String, String> requiredLookup
    ) {
        this.rawLookup = Objects.requireNonNull(rawLookup, "rawLookup cannot be null");
        this.envOrFileLookup = Objects.requireNonNull(envOrFileLookup, "envOrFileLookup cannot be null");
        this.requiredLookup = Objects.requireNonNull(requiredLookup, "requiredLookup cannot be null");
    }

    String resolveWmsEnvironment() {
        String value = firstNonBlank(
                envOrFileLookup.apply("WMS_ENV"),
                envOrFileLookup.apply("ACTIVE_ENV"),
                rawLookup.apply("WMS_ENV"),
                rawLookup.apply("ACTIVE_ENV")
        );
        return value == null ? "PROD" : value.trim().toUpperCase(Locale.ROOT);
    }

    String resolveSiteHost(String siteCode) {
        String scoped = rawLookup.apply("SITE_" + siteCode + "_" + resolveWmsEnvironment() + "_HOST");
        if (scoped != null && !scoped.isBlank()) {
            return scoped.trim();
        }
        return requiredLookup.apply("SITE_" + siteCode + "_HOST");
    }

    String resolveJdbcUrl(String activeSiteCode, IntSupplier oraclePort, Supplier<String> oracleService) {
        String explicitJdbc = rawLookup.apply("ORACLE_JDBC_URL");
        if (explicitJdbc != null && !explicitJdbc.isBlank()) {
            return explicitJdbc.trim();
        }

        String dsn = rawLookup.apply("ORACLE_DSN");
        if (dsn != null && !dsn.isBlank()) {
            String trimmed = dsn.trim();
            return trimmed.startsWith("jdbc:") ? trimmed : "jdbc:oracle:thin:@" + trimmed;
        }

        return buildHostPortServiceUrl(activeSiteCode, oraclePort, oracleService);
    }

    List<String> resolveJdbcUrlCandidates(String activeSiteCode, IntSupplier oraclePort, Supplier<String> oracleService) {
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add(resolveJdbcUrl(activeSiteCode, oraclePort, oracleService));

        String alias = resolveOdbcAliasOrNull();
        if (alias != null && !alias.isBlank()) {
            ordered.add("jdbc:oracle:thin:@" + alias.trim());
        }

        ordered.add(buildHostPortServiceUrl(activeSiteCode, oraclePort, oracleService));
        return List.copyOf(ordered);
    }

    String resolveOdbcAliasOrNull() {
        return firstNonBlank(
                rawLookup.apply("ORACLE_ODBC_DSN"),
                rawLookup.apply("ORACLE_NET_SERVICE"),
                rawLookup.apply("ORACLE_TNS_ALIAS")
        );
    }

    private String buildHostPortServiceUrl(String activeSiteCode, IntSupplier oraclePort, Supplier<String> oracleService) {
        return "jdbc:oracle:thin:@//" + resolveSiteHost(activeSiteCode) + ":" + oraclePort.getAsInt() + "/" + oracleService.get();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }
}

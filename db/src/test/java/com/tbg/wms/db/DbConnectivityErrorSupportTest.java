/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.db;

import com.tbg.wms.core.AppConfig;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class DbConnectivityErrorSupportTest {

    private final DbConnectivityErrorSupport support = new DbConnectivityErrorSupport();

    @Test
    void detectsAuthenticationFailuresAcrossNestedCauses() {
        Throwable nested = new RuntimeException("wrapper",
                new IllegalStateException("ORA-01017 invalid username/password"));

        assertTrue(support.isAuthenticationFailure(nested));
        assertFalse(support.isAuthenticationFailure(new RuntimeException("connection timed out")));
    }

    @Test
    void mapsServiceAndTimeoutFailuresToActionableRemediation() {
        AppConfig config = mock(AppConfig.class);
        when(config.oracleService()).thenReturn("WMSP");
        when(config.oraclePort()).thenReturn(1521);

        SQLException serviceMissing = new SQLException("listener does not currently know", "12514");
        String serviceHint = support.remediationHint(serviceMissing, config);
        assertTrue(serviceHint.contains("ORACLE_SERVICE=WMSP"));

        SQLException timedOut = new SQLException("network error", "99999", new RuntimeException("connection timed out"));
        String timeoutHint = support.remediationHint(timedOut, config);
        assertTrue(timeoutHint.contains("port 1521"));
    }
}

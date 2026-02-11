/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.db;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.exception.WmsDbConnectivityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DbConnectionPool}.
 */
@ExtendWith(MockitoExtension.class)
class DbConnectionPoolTest {

    @Mock
    private AppConfig mockConfig;

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    @Mock
    private DatabaseMetaData mockMetaData;

    @Test
    void testDbConnectionPoolCreation() {
        // Setup
        when(mockConfig.activeSiteCode()).thenReturn("TBG3002");
        when(mockConfig.oracleJdbcUrl()).thenReturn("jdbc:oracle:thin:@//localhost:1521/WMSP");
        when(mockConfig.oracleUsername()).thenReturn("RPTADM");
        when(mockConfig.oraclePassword()).thenReturn("password");
        when(mockConfig.dbPoolMaxSize()).thenReturn(5);
        when(mockConfig.dbPoolConnectionTimeoutMs()).thenReturn(3000L);
        when(mockConfig.dbPoolValidationTimeoutMs()).thenReturn(2000L);

        // Actual test: pool creation will fail because we're not using real HikariCP
        // For integration tests, use a real DB or TestContainers
        // For unit tests, we just verify the exception handling path
        assertThrows(WmsDbConnectivityException.class, () -> {
            new DbConnectionPool(mockConfig);
        }, "Expected exception when DataSourceFactory fails");
    }

    @Test
    void testConnectivityDiagnosticsRecord() {
        // Test the diagnostic class
        DbConnectivityDiagnostics diag = new DbConnectivityDiagnostics(
            true,
            150,
            2,
            3,
            "Oracle Database 19c",
            null
        );

        assertTrue(diag.isConnected());
        assertEquals(150, diag.durationMs());
        assertEquals(2, diag.activeConnections());
        assertEquals(3, diag.idleConnections());
        assertEquals("Oracle Database 19c", diag.databaseVersion());
        assertNull(diag.errorMessage());
    }
}


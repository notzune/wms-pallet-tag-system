/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.db;

import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.model.Shipment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OracleDbQueryRepository using mocked database connections.
 */
@ExtendWith(MockitoExtension.class)
class OracleDbQueryRepositoryTest {

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    private OracleDbQueryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new OracleDbQueryRepository(mockDataSource);
    }

    @Test
    void testConstructorRequiresDataSource() {
        assertThrows(NullPointerException.class, () -> new OracleDbQueryRepository(null),
                "Constructor should reject null DataSource");
    }

    @Test
    void testFindShipmentWithLpnsRequiresShipmentId() {
        assertThrows(NullPointerException.class, () -> repository.findShipmentWithLpnsAndLineItems(null),
                "Should reject null shipmentId");

        assertThrows(IllegalArgumentException.class, () -> repository.findShipmentWithLpnsAndLineItems(""),
                "Should reject empty shipmentId");

        assertThrows(IllegalArgumentException.class, () -> repository.findShipmentWithLpnsAndLineItems("   "),
                "Should reject whitespace-only shipmentId");
    }

    @Test
    void testShipmentExistsRequiresShipmentId() {
        assertThrows(NullPointerException.class, () -> repository.shipmentExists(null),
                "Should reject null shipmentId");

        assertThrows(IllegalArgumentException.class, () -> repository.shipmentExists(""),
                "Should reject empty shipmentId");
    }

    @Test
    void testGetStagingLocationRequiresShipmentId() {
        assertThrows(NullPointerException.class, () -> repository.getStagingLocation(null),
                "Should reject null shipmentId");

        assertThrows(IllegalArgumentException.class, () -> repository.getStagingLocation(""),
                "Should reject empty shipmentId");
    }

    @Test
    void testShipmentExistsWhenNoLpns() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("shipment_count")).thenReturn(0);

        boolean exists = repository.shipmentExists("SHIP123");

        assertFalse(exists, "Shipment with no LPNs should return false");
        verify(mockConnection).close();
    }

    @Test
    void testShipmentExistsWhenLpnsPresent() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("shipment_count")).thenReturn(3);

        boolean exists = repository.shipmentExists("SHIP123");

        assertTrue(exists, "Shipment with LPNs should return true");
    }

    @Test
    void testGetStagingLocationSuccess() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("DSTLOC")).thenReturn("rossi");

        String location = repository.getStagingLocation("SHIP123");

        assertEquals("ROSSI", location, "Location should be normalized to uppercase");
    }

    @Test
    void testGetStagingLocationNotFound() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        String location = repository.getStagingLocation("SHIP999");

        assertNull(location, "Non-existent shipment should return null");
    }

    @Test
    void testFindShipmentHandlesDatabaseException() throws SQLException {
        when(mockDataSource.getConnection()).thenThrow(new SQLException("17002", "Connection refused"));

        WmsDbConnectivityException thrown = assertThrows(WmsDbConnectivityException.class,
                () -> repository.findShipmentWithLpnsAndLineItems("SHIP123"));

        assertEquals(3, thrown.getExitCode());
        assertTrue(thrown.getMessage().contains("Failed to retrieve shipment"));
        assertNotNull(thrown.getRemediationHint());
    }

    @Test
    void testShipmentExistsHandlesDatabaseException() throws SQLException {
        when(mockDataSource.getConnection()).thenThrow(new SQLException("ORA-12514", "Service not found"));

        WmsDbConnectivityException thrown = assertThrows(WmsDbConnectivityException.class,
                () -> repository.shipmentExists("SHIP123"));

        assertEquals(3, thrown.getExitCode());
        assertTrue(thrown.getMessage().contains("Failed to check shipment existence"));
    }

    @Test
    void testGetStagingLocationHandlesDatabaseException() throws SQLException {
        when(mockDataSource.getConnection()).thenThrow(new SQLException("ORA-1017", "Invalid username/password"));

        WmsDbConnectivityException thrown = assertThrows(WmsDbConnectivityException.class,
                () -> repository.getStagingLocation("SHIP123"));

        assertEquals(3, thrown.getExitCode());
        assertTrue(thrown.getMessage().contains("Failed to retrieve staging location"));
    }

    @Test
    void testCloseIsNoOp() {
        // Should not throw any exception
        assertDoesNotThrow(() -> repository.close());
    }
}


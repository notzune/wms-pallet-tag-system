/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.db;

import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.rail.RailFootprintCandidate;
import com.tbg.wms.core.rail.RailStopRecord;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void testFindRailStopsByTrainIdRequiresTrainId() {
        assertThrows(NullPointerException.class, () -> repository.findRailStopsByTrainId(null));
        assertThrows(IllegalArgumentException.class, () -> repository.findRailStopsByTrainId(" "));
    }

    @Test
    void testFindRailFootprintsByShortCodeRequiresNonNullList() {
        assertThrows(NullPointerException.class, () -> repository.findRailFootprintsByShortCode(null));
        assertEquals(Map.of(), repository.findRailFootprintsByShortCode(List.of("", "  ")));
    }

    @Test
    void testShipmentExistsWhenNoMatch() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        boolean exists = repository.shipmentExists("SHIP123");

        assertFalse(exists, "Missing shipment should return false");
        verify(mockConnection).close();
    }

    @Test
    void testShipmentExistsWhenMatchPresent() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        boolean exists = repository.shipmentExists("SHIP123");

        assertTrue(exists, "Existing shipment should return true");
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
    void testFindRailStopsByTrainIdMapsAndGroupsRows() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.wasNull()).thenReturn(false);

        when(mockResultSet.getString("RUN_DATE")).thenReturn("03-02-26", "03-02-26");
        when(mockResultSet.getInt("SEQ")).thenReturn(1, 1);
        when(mockResultSet.getString("TRAIN_NBR")).thenReturn("0303", "0303");
        when(mockResultSet.getString("DCS_WHSE")).thenReturn("3002", "3002");
        when(mockResultSet.getString("LOAD_NBR")).thenReturn("LD100", "LD100");
        when(mockResultSet.getString("VEHICLE_ID")).thenReturn("V100", "V100");
        when(mockResultSet.getString("SHORT_CODE")).thenReturn("01831", "01830");
        when(mockResultSet.getInt("TOTAL_CASES")).thenReturn(12, 30);

        List<RailStopRecord> rows = repository.findRailStopsByTrainId("JC03032026");

        assertEquals(1, rows.size());
        RailStopRecord row = rows.get(0);
        assertEquals("0303", row.getTrainNumber());
        assertEquals("LD100", row.getLoadNumber());
        assertEquals(2, row.getItems().size());
        assertEquals("01830", row.getItems().get(0).getItemNumber());
        assertEquals("01831", row.getItems().get(1).getItemNumber());
    }

    @Test
    void testFindRailFootprintsByShortCodeMapsCandidates() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.wasNull()).thenReturn(false);

        when(mockResultSet.getString("SHORT_CODE")).thenReturn("01830", "01830");
        when(mockResultSet.getString("ITEM_NBR")).thenReturn("ITEMB", "ITEMA");
        when(mockResultSet.getString("PRTFAM")).thenReturn("Domestic", "Domestic");
        when(mockResultSet.getInt("UC_PARS_FLG")).thenReturn(0, 0);
        when(mockResultSet.getInt("UNITS_PER_PALLET")).thenReturn(70, 56);

        Map<String, List<RailFootprintCandidate>> byShortCode =
                repository.findRailFootprintsByShortCode(List.of("01830"));

        assertEquals(1, byShortCode.size());
        List<RailFootprintCandidate> candidates = byShortCode.get("01830");
        assertEquals(2, candidates.size());
        assertEquals("ITEMA", candidates.get(0).getItemNumber());
        assertEquals("ITEMB", candidates.get(1).getItemNumber());
    }

    @Test
    void testFindRailFootprintsByShortCodeDeduplicatesQueryParameters() throws SQLException {
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(argThat(sql -> sql != null && sql.contains("IN (?)"))))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.wasNull()).thenReturn(false);
        when(mockResultSet.getString("SHORT_CODE")).thenReturn("01830");
        when(mockResultSet.getString("ITEM_NBR")).thenReturn("ITEMA");
        when(mockResultSet.getString("PRTFAM")).thenReturn("Domestic");
        when(mockResultSet.getInt("UC_PARS_FLG")).thenReturn(0);
        when(mockResultSet.getInt("UNITS_PER_PALLET")).thenReturn(56);

        Map<String, List<RailFootprintCandidate>> byShortCode =
                repository.findRailFootprintsByShortCode(List.of("01830", "01830"));

        assertEquals(1, byShortCode.size());
        assertEquals(1, byShortCode.get("01830").size());
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

    @Test
    void testBuildSkuCandidatesAvoidsDuplicatesWhenPrefixAndZeroTrimConverge() {
        List<String> candidates = SkuCandidateBuilder.buildCandidates("100000123");

        assertEquals(List.of("100000123", "000123"), candidates);
    }

    @Test
    void testBuildSkuCandidatesDoesNotDuplicateWhenTransformsConverge() {
        List<String> candidates = SkuCandidateBuilder.buildCandidates("100123");

        assertEquals(List.of("100123", "123"), candidates);
    }
}


/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
final class PrtmstDescriptionColumnResolverTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement statement;

    @Mock
    private ResultSet resultSet;

    @Test
    void getColumnsReturnsPreferredOrderFromDictionary() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(resultSet.getString("COLUMN_NAME")).thenReturn("LNGDSC", "PRT_DISP", "SHORT_DSC");

        PrtmstDescriptionColumnResolver resolver = new PrtmstDescriptionColumnResolver();
        List<String> columns = resolver.getColumns(connection);

        assertEquals(List.of("SHORT_DSC", "LNGDSC", "PRT_DISP"), columns);
    }

    @Test
    void getColumnsCachesResolvedResult() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("COLUMN_NAME")).thenReturn("SHORT_DSC");

        PrtmstDescriptionColumnResolver resolver = new PrtmstDescriptionColumnResolver();
        List<String> first = resolver.getColumns(connection);
        List<String> second = resolver.getColumns(connection);

        assertEquals(List.of("SHORT_DSC"), first);
        assertEquals(first, second);
        verify(connection, times(1)).prepareStatement(anyString());
    }

    @Test
    void getColumnsFallsBackToDirectSelectWhenDictionaryLookupFails() throws SQLException {
        PreparedStatement fallbackStatement = org.mockito.Mockito.mock(PreparedStatement.class);
        ResultSet fallbackResultSet = org.mockito.Mockito.mock(ResultSet.class);

        when(connection.prepareStatement(argThat(sql -> sql != null && sql.contains("ALL_TAB_COLUMNS"))))
                .thenThrow(new SQLException("dictionary access denied"));
        when(connection.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM WMSP.PRTMST"))))
                .thenReturn(fallbackStatement);
        when(fallbackStatement.executeQuery()).thenReturn(fallbackResultSet);

        PrtmstDescriptionColumnResolver resolver = new PrtmstDescriptionColumnResolver();
        List<String> columns = resolver.getColumns(connection);

        assertEquals(List.of("SHORT_DSC", "LNGDSC", "PRT_DISP", "PRT_DISPTN"), columns);
    }
}

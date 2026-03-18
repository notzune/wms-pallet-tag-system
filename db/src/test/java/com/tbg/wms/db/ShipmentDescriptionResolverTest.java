package com.tbg.wms.db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentDescriptionResolverTest {

    @Test
    void resolveDescription_shouldCacheRepeatedLookupsForSameSkuClientAndWarehouse() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement prtdscStatement = mock(PreparedStatement.class);
        PreparedStatement prtmstMetadataStatement = mock(PreparedStatement.class);
        ResultSet prtdscResultSet = mock(ResultSet.class);
        ResultSet prtmstMetadataResultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.contains("FROM WMSP.PRTDSC")) {
                return prtdscStatement;
            }
            if (sql.contains("ALL_TAB_COLUMNS")) {
                return prtmstMetadataStatement;
            }
            throw new IllegalArgumentException("Unexpected SQL: " + sql);
        });

        when(prtdscStatement.executeQuery()).thenReturn(prtdscResultSet);
        when(prtdscResultSet.next()).thenReturn(true, false);
        when(prtdscResultSet.getString("SHORT_DSC")).thenReturn("Tropicana 12oz");

        when(prtmstMetadataStatement.executeQuery()).thenReturn(prtmstMetadataResultSet);
        when(prtmstMetadataResultSet.next()).thenReturn(false);

        ShipmentDescriptionResolver resolver = new ShipmentDescriptionResolver(
                connection,
                List.of("SHORT_DSC"),
                mock(PrtmstDescriptionColumnResolver.class)
        );

        assertEquals("Tropicana 12oz",
                resolver.resolveDescription("SKU123", "CLIENT1", "WH1", "Fallback"));
        assertEquals("Tropicana 12oz",
                resolver.resolveDescription("SKU123", "CLIENT1", "WH1", "Fallback"));

        verify(prtdscStatement, times(1)).executeQuery();
    }
}

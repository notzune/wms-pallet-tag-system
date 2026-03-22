package com.tbg.wms.db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentDescriptionSupportTest {
    private final ShipmentDescriptionSupport support = new ShipmentDescriptionSupport();

    @Test
    void chooseBestDescription_shouldPreferPrtdscThenPrtmstThenFallback() {
        assertEquals("PRTDSC value", support.chooseBestDescription("PRTDSC value", "PRTMST value", "Fallback"));
        assertEquals("PRTMST value", support.chooseBestDescription("----", "PRTMST value", "Fallback"));
        assertEquals("Fallback", support.chooseBestDescription("1234", "5678", "Fallback"));
    }

    @Test
    void chooseBestDescription_shouldReturnNullWhenNothingReadable() {
        assertNull(support.chooseBestDescription("1234", "5678", ""));
    }

    @Test
    void resolveItemDescription_shouldReuseCachedLookupWithinSingleLoad() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(org.mockito.ArgumentMatchers.contains("FROM WMSP.PRTDSC"))).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("SHORT_DSC")).thenReturn("Readable description");

        assertEquals(
                "Readable description",
                support.resolveItemDescription(conn, "100000123", "CLIENT1", "W1", "Fallback", List.of("PRTDESC"))
        );
        assertEquals(
                "Readable description",
                support.resolveItemDescription(conn, "100000123", "CLIENT1", "W1", "Fallback", List.of("PRTDESC"))
        );

        verify(conn, times(1)).prepareStatement(org.mockito.ArgumentMatchers.contains("FROM WMSP.PRTDSC"));
        verify(stmt, times(1)).executeQuery();
    }
}

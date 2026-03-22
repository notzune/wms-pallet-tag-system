package com.tbg.wms.cli.gui;

import com.tbg.wms.core.exception.WmsDbConnectivityException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiDbStatusSupportTest {

    private final GuiDbStatusSupport support = new GuiDbStatusSupport();

    @Test
    void connectedShouldUseServiceNameInFooterText() {
        GuiDbStatusSupport.StatusState state = support.connected("WMSP");

        assertEquals("Connected - WMSP", state.text());
        assertTrue(state.tooltip().contains("WMSP"));
    }

    @Test
    void failureShouldShowOnlyOracleCodeInShortText() {
        WmsDbConnectivityException exception = new WmsDbConnectivityException(
                "Database connectivity test failed: 12514: ORA-12514 listener does not currently know",
                new IllegalStateException("ORA-12514 listener does not currently know"),
                "Verify ORACLE_SERVICE=WMSP is correct."
        );

        GuiDbStatusSupport.StatusState state = support.failure("WMSP", exception).orElseThrow();

        assertEquals("Not connected - ORA-12514", state.text());
        assertTrue(state.tooltip().contains("ORA-12514 listener does not currently know"));
        assertTrue(state.tooltip().contains("Verify ORACLE_SERVICE=WMSP is correct."));
    }

    @Test
    void failureShouldIgnoreNonConnectivityErrors() {
        IllegalArgumentException exception = new IllegalArgumentException("Shipment ID is required.");

        assertTrue(support.failure("WMSP", exception).isEmpty());
    }
}

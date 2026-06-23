package com.tbg.wms.cli.gui;

import com.tbg.wms.core.exception.WmsDbConnectivityException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiDbStatusRefreshSupportTest {

    private final GuiDbStatusRefreshSupport support = new GuiDbStatusRefreshSupport();

    @Test
    void checking_shouldReturnCheckingStateForService() {
        GuiDbStatusSupport.StatusState state = support.checking("WMSP");

        assertEquals("Checking - WMSP", state.text());
    }

    @Test
    void verify_shouldRunProbeAndReturnConnectedState() throws Exception {
        AtomicInteger calls = new AtomicInteger();

        GuiDbStatusSupport.StatusState state = support.verify("WMSP", calls::incrementAndGet);

        assertEquals(1, calls.get());
        assertEquals("Connected - WMSP", state.text());
    }

    @Test
    void failure_shouldReturnConnectivityFailureState() {
        WmsDbConnectivityException exception = new WmsDbConnectivityException(
                "Database connectivity test failed: ORA-12514 listener does not currently know",
                "Verify ORACLE_SERVICE=WMSP is correct."
        );

        Optional<GuiDbStatusSupport.StatusState> state = support.failure("WMSP", exception);

        assertTrue(state.isPresent());
        assertEquals("Not connected - ORA-12514", state.orElseThrow().text());
    }

    @Test
    void failure_shouldIgnoreNonConnectivityFailure() {
        Optional<GuiDbStatusSupport.StatusState> state =
                support.failure("WMSP", new IllegalArgumentException("Shipment ID is required."));

        assertTrue(state.isEmpty());
    }
}

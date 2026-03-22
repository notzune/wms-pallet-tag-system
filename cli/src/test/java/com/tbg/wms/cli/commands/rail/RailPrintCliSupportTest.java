/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.commands.rail;

import com.tbg.wms.core.rail.RailCarCard;
import com.tbg.wms.core.rail.RailFamilyFootprint;
import com.tbg.wms.core.rail.RailStopRecord;
import com.tbg.wms.core.rail.RailWorkflowService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailPrintCliSupportTest {

    private final RailPrintCliSupport support = new RailPrintCliSupport();

    @Test
    void validateOptionsRejectsInvalidCombinations() {
        assertEquals(
                "Error: --validate-system-default-print cannot be combined with train/template print options.",
                support.validateOptions(true, false, "JC08312025", false)
        );
        assertEquals(
                "Error: --train is required unless --template is used.",
                support.validateOptions(false, false, " ", false)
        );
        assertNull(support.validateOptions(false, true, null, false));
    }

    @Test
    void buildPreviewTextIncludesOperationalSummary() throws Exception {
        RailWorkflowService.RailWorkflowResult result = workflowResult(
                List.of(new RailCarCard(
                        "JC08312025",
                        "142",
                        "CAR-100",
                        "LOAD-1",
                        List.of(),
                        1,
                        2,
                        3,
                        List.of("DOM:50"),
                        List.of("01830")
                )),
                List.of(new RailStopRecord("03-02-26", "142", "0303", "CAR-100", "BR", "LOAD-1", List.of())),
                Map.of("01830", new RailFamilyFootprint("01830", "DOM", 100)),
                Set.of("01831"),
                Set.of("01830")
        );

        String preview = support.buildPreviewText(result);

        assertTrue(preview.contains("SEQ   VEHICLE      CAN   DOM   KEV"));
        assertTrue(preview.contains("Railcars: 1"));
        assertTrue(preview.contains("WMS rows: 1"));
        assertTrue(preview.contains("Resolved footprints: 1"));
        assertTrue(preview.contains("Unresolved short codes: 1"));
        assertTrue(preview.contains("Missing in card math: 01830"));
    }

    @SuppressWarnings("unchecked")
    private static RailWorkflowService.RailWorkflowResult workflowResult(
            List<RailCarCard> cards,
            List<RailStopRecord> rawRows,
            Map<String, RailFamilyFootprint> resolvedFootprints,
            Set<String> unresolvedShortCodes,
            Set<String> missingItemsInCards
    ) throws Exception {
        Constructor<RailWorkflowService.RailWorkflowResult> ctor =
                (Constructor<RailWorkflowService.RailWorkflowResult>) RailWorkflowService.RailWorkflowResult.class
                        .getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(
                "JC08312025",
                rawRows,
                List.of(),
                cards,
                resolvedFootprints,
                unresolvedShortCodes,
                missingItemsInCards
        );
    }
}

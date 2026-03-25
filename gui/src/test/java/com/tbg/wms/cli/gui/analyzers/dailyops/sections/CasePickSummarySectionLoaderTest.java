package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CasePickSummarySectionLoaderTest {

    @Test
    void summaryLoader_shouldNormalizeNullCountsToZero() {
        CasePickSummarySectionLoader loader = new CasePickSummarySectionLoader();

        CasePickSummaryRow row = loader.mapRow(new CasePickSummarySectionLoader.QueryRow(
                LocalDate.of(2026, 3, 25),
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertEquals(0, row.picks());
        assertEquals(0, row.remaining());
        assertEquals(0, row.domesticTotal());
        assertEquals(0, row.domesticRemaining());
        assertEquals(0, row.canadianTotal());
        assertEquals(0, row.canadianRemaining());
    }
}

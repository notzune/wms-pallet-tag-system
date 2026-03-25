package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import java.time.LocalDate;

public record CasePickSummaryRow(
        LocalDate casePicks,
        int picks,
        int remaining,
        int domesticTotal,
        int domesticRemaining,
        int canadianTotal,
        int canadianRemaining
) {
}

package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import java.time.LocalDate;

public record CasePickShiftThroughputRow(
        LocalDate casePicks,
        int thirdA,
        int first,
        int second,
        int thirdB
) {
}

package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import java.time.LocalDate;

public record UnloadLoadActivityRow(
        String metric,
        LocalDate activityDate,
        int thirdA,
        int first,
        int second,
        int thirdB
) {
}

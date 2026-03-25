package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import java.time.LocalDate;

public record AppointmentsRow(
        LocalDate appointmentDay,
        int trucks,
        int outbounds,
        int completed,
        int outRemaining,
        int inbounds,
        int inboundCompleted,
        int inboundRemaining
) {
}

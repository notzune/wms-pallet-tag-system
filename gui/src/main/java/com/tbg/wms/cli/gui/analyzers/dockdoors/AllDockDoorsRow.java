package com.tbg.wms.cli.gui.analyzers.dockdoors;

import java.time.LocalDateTime;

public record AllDockDoorsRow(
        String door,
        String dropLive,
        String inboundFlag,
        String trailerId,
        String moveNumber,
        LocalDateTime appointmentTime,
        LocalDateTime checkInTime,
        LocalDateTime movedToDoorAt,
        int minutesAtDoor,
        double shipmentPercentComplete,
        double appointmentOverHours,
        double stops,
        String shortFlag,
        String customer,
        String soldToNumber,
        String soldToMatchKey,
        String airbagFlag,
        double rossiPallets,
        double rossiCompletedPicks
) {
}

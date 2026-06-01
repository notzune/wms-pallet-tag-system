package com.tbg.wms.cli.gui.rail;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RailLabelDateSupportTest {

    @Test
    void parseLabelDateAcceptsTypedMonthDayYear() {
        RailLabelDateSupport support = new RailLabelDateSupport();

        assertEquals("01-30-24", support.parseLabelDate("01-30-24"));
        assertEquals("01-30-24", support.parseLabelDate("1-30-24"));
    }

    @Test
    void parseLabelDateRejectsInvalidText() {
        RailLabelDateSupport support = new RailLabelDateSupport();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> support.parseLabelDate("2024-01-30")
        );

        assertEquals("Label date must use MM-DD-YY.", ex.getMessage());
    }

    @Test
    void formatTodayUsesSuppliedClockDate() {
        RailLabelDateSupport support = new RailLabelDateSupport();

        assertEquals("06-01-26", support.formatDate(LocalDate.of(2026, 6, 1)));
    }
}

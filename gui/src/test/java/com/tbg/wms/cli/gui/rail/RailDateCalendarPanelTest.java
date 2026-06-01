package com.tbg.wms.cli.gui.rail;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailDateCalendarPanelTest {

    @Test
    void calendarBuildsMonthGridWithLeadingAndCurrentMonthDays() {
        RailDateCalendarPanel panel = new RailDateCalendarPanel(
                LocalDate.of(2026, 6, 1),
                ignored -> {
                }
        );

        assertEquals(YearMonth.of(2026, 6), panel.visibleMonth());
        assertEquals("June", panel.monthTitleText());
        assertEquals("1", panel.dayButtonText(LocalDate.of(2026, 6, 1)));
        assertEquals("31", panel.dayButtonText(LocalDate.of(2026, 5, 31)));
        assertFalse(panel.isCurrentMonthDay(LocalDate.of(2026, 5, 31)));
        assertTrue(panel.isCurrentMonthDay(LocalDate.of(2026, 6, 1)));
    }

    @Test
    void nextAndPreviousButtonsMoveVisibleMonth() {
        RailDateCalendarPanel panel = new RailDateCalendarPanel(
                LocalDate.of(2026, 6, 1),
                ignored -> {
                }
        );

        panel.nextMonth();
        assertEquals(YearMonth.of(2026, 7), panel.visibleMonth());

        panel.previousMonth();
        assertEquals(YearMonth.of(2026, 6), panel.visibleMonth());
    }

    @Test
    void clickingDayReportsSelectedDate() {
        List<LocalDate> selected = new ArrayList<>();
        RailDateCalendarPanel panel = new RailDateCalendarPanel(
                LocalDate.of(2026, 6, 1),
                selected::add
        );

        JButton button = panel.dayButton(LocalDate.of(2026, 6, 15));
        button.doClick();

        assertEquals(List.of(LocalDate.of(2026, 6, 15)), selected);
    }
}

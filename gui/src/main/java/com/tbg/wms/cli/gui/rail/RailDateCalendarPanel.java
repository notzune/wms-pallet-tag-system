/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.Serial;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Month-grid calendar used by rail label date entry.
 */
final class RailDateCalendarPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Color SELECTED_BACKGROUND = new Color(223, 232, 252);
    private static final Color OTHER_MONTH_TEXT = new Color(140, 148, 160);
    private static final Color CURRENT_MONTH_TEXT = new Color(30, 43, 60);
    private static final int GRID_DAYS = 42;
    private final transient Consumer<LocalDate> selectionHandler;
    private final LocalDate selectedDate;
    private final JLabel monthTitle = new JLabel("", SwingConstants.CENTER);
    private final JPanel daysPanel = new JPanel(new GridLayout(6, 7, 4, 4));
    private final HashMap<LocalDate, JButton> dayButtons = new HashMap<>();
    private YearMonth visibleMonth;

    RailDateCalendarPanel(LocalDate selectedDate, Consumer<LocalDate> selectionHandler) {
        this.selectedDate = Objects.requireNonNull(selectedDate, "selectedDate cannot be null");
        this.selectionHandler = Objects.requireNonNull(selectionHandler, "selectionHandler cannot be null");
        this.visibleMonth = YearMonth.from(selectedDate);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCalendarBody(), BorderLayout.CENTER);
        renderMonth();
    }

    private JPanel buildHeader() {
        JButton previousButton = new JButton("<");
        previousButton.setFocusable(false);
        previousButton.addActionListener(e -> previousMonth());

        JButton nextButton = new JButton(">");
        nextButton.setFocusable(false);
        nextButton.addActionListener(e -> nextMonth());

        monthTitle.setFont(monthTitle.getFont().deriveFont(Font.BOLD, 16f));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.add(previousButton, BorderLayout.WEST);
        header.add(monthTitle, BorderLayout.CENTER);
        header.add(nextButton, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCalendarBody() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        JPanel weekdays = new JPanel(new GridLayout(1, 7, 4, 0));
        for (String day : new String[]{"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"}) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
            label.setForeground(OTHER_MONTH_TEXT);
            weekdays.add(label);
        }
        body.add(weekdays, BorderLayout.NORTH);
        body.add(daysPanel, BorderLayout.CENTER);
        return body;
    }

    void previousMonth() {
        visibleMonth = visibleMonth.minusMonths(1);
        renderMonth();
    }

    void nextMonth() {
        visibleMonth = visibleMonth.plusMonths(1);
        renderMonth();
    }

    YearMonth visibleMonth() {
        return visibleMonth;
    }

    String monthTitleText() {
        return monthTitle.getText();
    }

    JButton dayButton(LocalDate date) {
        return dayButtons.get(date);
    }

    String dayButtonText(LocalDate date) {
        JButton button = dayButton(date);
        return button == null ? "" : button.getText();
    }

    boolean isCurrentMonthDay(LocalDate date) {
        JButton button = dayButton(date);
        return button != null && Boolean.TRUE.equals(button.getClientProperty("currentMonth"));
    }

    private void renderMonth() {
        monthTitle.setText(visibleMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        daysPanel.removeAll();
        dayButtons.clear();

        LocalDate firstVisibleDate = firstVisibleDate();
        for (int i = 0; i < GRID_DAYS; i++) {
            LocalDate date = firstVisibleDate.plusDays(i);
            JButton button = createDayButton(date);
            dayButtons.put(date, button);
            daysPanel.add(button);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    private LocalDate firstVisibleDate() {
        LocalDate firstOfMonth = visibleMonth.atDay(1);
        LocalDate sunday = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        return sunday;
    }

    private JButton createDayButton(LocalDate date) {
        JButton button = new JButton(String.valueOf(date.getDayOfMonth()));
        button.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.putClientProperty("currentMonth", YearMonth.from(date).equals(visibleMonth));
        if (date.equals(selectedDate)) {
            button.setBackground(SELECTED_BACKGROUND);
            button.setFont(button.getFont().deriveFont(Font.BOLD));
        } else {
            button.setBackground(Color.WHITE);
        }
        button.setForeground(isCurrentMonth(date) ? CURRENT_MONTH_TEXT : OTHER_MONTH_TEXT);
        button.addActionListener(e -> selectionHandler.accept(date));
        return button;
    }

    private boolean isCurrentMonth(LocalDate date) {
        return YearMonth.from(date).equals(visibleMonth);
    }
}

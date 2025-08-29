package com.marketplace.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class DateUtils {

    /**
     * Get all dates between start and end date (inclusive)
     */
    public static List<LocalDate> getDatesBetween(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        
        return dates;
    }

    /**
     * Get all dates for specific days of week between start and end date
     */
    public static List<LocalDate> getDatesForDaysOfWeek(LocalDate startDate, LocalDate endDate, DayOfWeek... daysOfWeek) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            for (DayOfWeek day : daysOfWeek) {
                if (currentDate.getDayOfWeek() == day) {
                    dates.add(currentDate);
                    break;
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return dates;
    }

    /**
     * Get next occurrence of specific day of week from given date
     */
    public static LocalDate getNextDayOfWeek(LocalDate fromDate, DayOfWeek dayOfWeek) {
        return fromDate.with(TemporalAdjusters.next(dayOfWeek));
    }

    /**
     * Get previous occurrence of specific day of week from given date
     */
    public static LocalDate getPreviousDayOfWeek(LocalDate fromDate, DayOfWeek dayOfWeek) {
        return fromDate.with(TemporalAdjusters.previous(dayOfWeek));
    }

    /**
     * Check if date is weekend (Saturday or Sunday)
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Check if date is weekday (Monday to Friday)
     */
    public static boolean isWeekday(LocalDate date) {
        return !isWeekend(date);
    }

    /**
     * Get start of week (Monday) for given date
     */
    public static LocalDate getStartOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Get end of week (Sunday) for given date
     */
    public static LocalDate getEndOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /**
     * Get start of month for given date
     */
    public static LocalDate getStartOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Get end of month for given date
     */
    public static LocalDate getEndOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Add weeks to date
     */
    public static LocalDate addWeeks(LocalDate date, int weeks) {
        return date.plusWeeks(weeks);
    }

    /**
     * Format date as string (yyyy-MM-dd)
     */
    public static String formatDate(LocalDate date) {
        return date.toString();
    }

    /**
     * Parse date from string (yyyy-MM-dd)
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr);
    }

    /**
     * Get list of dates for next N weeks on specific days
     */
    public static List<LocalDate> getDatesForNextWeeks(LocalDate startDate, int weeks, DayOfWeek... daysOfWeek) {
        LocalDate endDate = startDate.plusWeeks(weeks);
        return getDatesForDaysOfWeek(startDate, endDate, daysOfWeek);
    }
}

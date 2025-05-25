package com.example.productapi.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.io.IOException;

public class HolidayChecker {
    private static final String HOLIDAY_CSV = "/US_Federal_Holidays_2023_2030.csv";
    private static final Set<LocalDate> holidays = new HashSet<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    static {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(HolidayChecker.class.getResourceAsStream(HOLIDAY_CSV))))) {

            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // skip header
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 1) {
                    LocalDate date = LocalDate.parse(parts[2].trim(),formatter);
                    holidays.add(date);
                }
            }

        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Failed to load holiday data from CSV", e);
        }
    }

    public static boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }

    /**
     * Check if date is weekend
     *
     * @param date Date to check
     * @return true if date is Saturday or Sunday
     */
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 5; // Saturday = 5, Sunday = 6
    }
}

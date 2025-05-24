package com.example.productapi.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.io.IOException;

public class HolidayChecker {
    private static final String HOLIDAY_CSV = "/US_Federal_Holidays_2023_2030.csv";
    private static final Set<LocalDate> holidays = new HashSet<>();

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
                    LocalDate date = LocalDate.parse(parts[0].trim());
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
}

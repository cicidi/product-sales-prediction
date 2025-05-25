package com.example.productapi.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author walter on 5/21/25
 */
public class TimeUtils {

  private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy/MM");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Parse a date string in yyyy-MM-dd format to LocalDateTime
   *
   * @param dateStr Date string in yyyy-MM-dd format
   * @return LocalDateTime at start of the day
   */
  public static LocalDateTime parseDate(String dateStr) {
    try {
      LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
      return date.atStartOfDay();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date format. Expected format: yyyy-MM-dd");
    }
  }

  /**
   * Parse a date string in yyyy-MM-dd format to LocalDateTime at end of day
   *
   * @param dateStr Date string in yyyy-MM-dd format
   * @return LocalDateTime at end of the day (23:59:59.999999999)
   */
  public static LocalDateTime parseDateEndOfDay(String dateStr) {
    try {
      LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
      return date.plusDays(1).atStartOfDay().minusNanos(1);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date format. Expected format: yyyy-MM-dd");
    }
  }
}

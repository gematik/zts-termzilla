/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 */
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils;

import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.InstantParameterQueryGenerator;
import java.util.Calendar;
import org.hl7.fhir.r4.model.DateTimeType;

/**
 * Static utility class for date and time instant calculations.
 *
 * <p>This class provides helper methods for converting FHIR date/time values with various precision
 * levels to millisecond timestamps (long values representing milliseconds since epoch). These
 * timestamps are used for Lucene-based date range queries.
 *
 * <p><b>Key Concepts:</b>
 *
 * <ul>
 *   <li><b>Instant</b> - A point in time represented as milliseconds since Unix epoch (January 1,
 *       1970, 00:00:00 GMT)
 *   <li><b>Precision</b> - The granularity of a date/time value (YEAR, MONTH, DAY, MINUTE, SECOND,
 *       MILLI)
 *   <li><b>First Instant</b> - The earliest millisecond within a precision boundary (e.g., first
 *       millisecond of a year: January 1, 00:00:00.000)
 *   <li><b>Last Instant</b> - The latest millisecond within a precision boundary (e.g., last
 *       millisecond of a year: December 31, 23:59:59.999)
 * </ul>
 *
 * <p><b>Usage Patterns:</b>
 *
 * <p>These methods are primarily used by {@link
 * InstantParameterQueryGenerator}
 * to convert FHIR date search parameters into Lucene range queries.
 *
 * <p><b>Examples:</b>
 *
 * <ul>
 *   <li>YEAR precision (2023): First instant = 2023-01-01T00:00:00.000, Last instant =
 *       2023-12-31T23:59:59.999
 *   <li>MONTH precision (2023-06): First instant = 2023-06-01T00:00:00.000, Last instant =
 *       2023-06-30T23:59:59.999
 *   <li>DAY precision (2023-06-15): First instant = 2023-06-15T00:00:00.000, Last instant =
 *       2023-06-15T23:59:59.999
 * </ul>
 *
 * <p><b>Important Notes:</b>
 *
 * <ul>
 *   <li><b>Timezone Handling</b> - Currently uses the system default timezone via {@link
 *       Calendar#getInstance()}. This may cause issues when dealing with dates across different
 *       timezones or during daylight saving time transitions.
 *   <li><b>Month Parameter</b> - The {@code month} parameter uses Java Calendar conventions where
 *       January = 0, February = 1, ..., December = 11
 *   <li><b>Day of Month</b> - The {@code day} parameter follows standard calendar conventions
 *       (1-31), not the zero-based indexing used for months
 * </ul>
 *
 * @see java.util.Calendar
 * @see org.hl7.fhir.r4.model.DateTimeType
 */
public final class DateUtil {

  /** Private constructor to prevent instantiation of utility class. */
  private DateUtil() {
    throw new AssertionError("DateUtil is a utility class and should not be instantiated");
  }

  /**
   * Returns the first millisecond of a given year.
   *
   * <p>Equivalent to the timestamp of January 1, 00:00:00.000 of the specified year.
   *
   * <p><b>Example:</b> {@code getFirstInstantOfYear(2023)} returns the timestamp for
   * 2023-01-01T00:00:00.000
   *
   * @param year the year (e.g., 2023)
   * @return milliseconds since epoch representing the first instant of the year
   */
  public static long getFirstInstantOfYear(int year) {
    return getInstantOfDate(year, 0, 1, 0, 0, 0, 0);
  }

  /**
   * Returns the last millisecond of a given year.
   *
   * <p>Equivalent to the timestamp of December 31, 23:59:59.999 of the specified year.
   *
   * <p><b>Implementation:</b> Calculates the first instant of the next year and subtracts one
   * millisecond.
   *
   * <p><b>Example:</b> {@code getLastInstantOfYear(2023)} returns the timestamp for
   * 2023-12-31T23:59:59.999
   *
   * @param year the year (e.g., 2023)
   * @return milliseconds since epoch representing the last instant of the year
   */
  public static long getLastInstantOfYear(int year) {
    // get first instant of next year and reduce by one millisecond
    return getFirstInstantOfYear(year + 1) - 1;
  }

  /**
   * Returns the first millisecond of a given year and month.
   *
   * <p>Equivalent to the timestamp of the 1st day of the month at 00:00:00.000.
   *
   * <p><b>Example:</b> {@code getFirstInstantOfYearMonth(2023, 5)} returns the timestamp for
   * 2023-06-01T00:00:00.000 (June is month 5 in Calendar, where January = 0)
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @return milliseconds since epoch representing the first instant of the month
   */
  public static long getFirstInstantOfYearMonth(int year, int month) {
    return getInstantOfDate(year, month, 1, 0, 0, 0, 0);
  }

  /**
   * Returns the last millisecond of a given year and month.
   *
   * <p>Equivalent to the timestamp of the last day of the month at 23:59:59.999.
   *
   * <p><b>Implementation:</b> Calculates the first instant of the next month and subtracts one
   * millisecond. Handles year boundary when month is December.
   *
   * <p><b>Example:</b> {@code getLastInstantOfYearMonth(2023, 5)} returns the timestamp for
   * 2023-06-30T23:59:59.999 (June is month 5)
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @return milliseconds since epoch representing the last instant of the month
   */
  public static long getLastInstantOfYearMonth(int year, int month) {
    if (month < 11) {
      return getInstantOfDate(year, month + 1, 1, 0, 0, 0, 0) - 1;
    } else {
      return getInstantOfDate(year + 1, 0, 1, 0, 0, 0, 0) - 1;
    }
  }

  /**
   * Returns the first millisecond of a given year, month, and day.
   *
   * <p>Equivalent to the timestamp at 00:00:00.000 on the specified date.
   *
   * <p><b>Example:</b> {@code getFirstInstantOfYearMonthDay(2023, 5, 15)} returns the timestamp for
   * 2023-06-15T00:00:00.000
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @param day the day of month (1-31)
   * @return milliseconds since epoch representing the first instant of the day
   */
  public static long getFirstInstantOfYearMonthDay(int year, int month, int day) {
    return getInstantOfDate(year, month, day, 0, 0, 0, 0);
  }

  /**
   * Returns the last millisecond of a given year, month, and day.
   *
   * <p>Equivalent to the timestamp at 23:59:59.999 on the specified date.
   *
   * <p><b>Example:</b> {@code getLastInstantOfYearMonthDay(2023, 5, 15)} returns the timestamp for
   * 2023-06-15T23:59:59.999
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @param day the day of month (1-31)
   * @return milliseconds since epoch representing the last instant of the day
   */
  public static long getLastInstantOfYearMonthDay(int year, int month, int day) {
    return getInstantOfDate(year, month, day, 23, 59, 59, 999);
  }

  /**
   * Returns the first millisecond of a given year, month, day, hour, and minute.
   *
   * <p>Equivalent to the timestamp at the start of the specified minute (XX:XX:00.000).
   *
   * <p><b>Example:</b> {@code getFirstInstantOfYearMonthDayHourMinute(2023, 5, 15, 14, 30)} returns
   * the timestamp for 2023-06-15T14:30:00.000
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @param day the day of month (1-31)
   * @param hours the hour of day (0-23)
   * @param minutes the minute (0-59)
   * @return milliseconds since epoch representing the first instant of the minute
   */
  public static long getFirstInstantOfYearMonthDayHourMinute(
      int year, int month, int day, int hours, int minutes) {
    return getInstantOfDate(year, month, day, hours, minutes, 0, 0);
  }

  /**
   * Returns the last millisecond of a given year, month, day, hour, and minute.
   *
   * <p>Equivalent to the timestamp at the end of the specified minute (XX:XX:59.999).
   *
   * <p><b>Example:</b> {@code getLastInstantOfYearMonthDayHourMinute(2023, 5, 15, 14, 30)} returns
   * the timestamp for 2023-06-15T14:30:59.999
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @param day the day of month (1-31)
   * @param hours the hour of day (0-23)
   * @param minutes the minute (0-59)
   * @return milliseconds since epoch representing the last instant of the minute
   */
  public static long getLastInstantOfYearMonthDayHourMinute(
      int year, int month, int day, int hours, int minutes) {
    return getInstantOfDate(year, month, day, hours, minutes, 59, 999);
  }

  /**
   * Returns the first millisecond of a given year, month, day, hour, minute, and second.
   *
   * <p>Equivalent to the timestamp at the start of the specified second (XX:XX:XX.000).
   *
   * <p><b>Example:</b> {@code getFirstInstantOfYearMonthDayHourMinuteSecond(2023, 5, 15, 14, 30,
   * 45)} returns the timestamp for 2023-06-15T14:30:45.000
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @param day the day of month (1-31)
   * @param hours the hour of day (0-23)
   * @param minutes the minute (0-59)
   * @param seconds the second (0-59)
   * @return milliseconds since epoch representing the first instant of the second
   */
  public static long getFirstInstantOfYearMonthDayHourMinuteSecond(
      int year, int month, int day, int hours, int minutes, int seconds) {
    return getInstantOfDate(year, month, day, hours, minutes, seconds, 0);
  }

  /**
   * Returns the last millisecond of a given year, month, day, hour, minute, and second.
   *
   * <p>Equivalent to the timestamp at the end of the specified second (XX:XX:XX.999).
   *
   * <p><b>Example:</b> {@code getLastInstantOfYearMonthDayHourMinuteSecond(2023, 5, 15, 14, 30,
   * 45)} returns the timestamp for 2023-06-15T14:30:45.999
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @param day the day of month (1-31)
   * @param hours the hour of day (0-23)
   * @param minutes the minute (0-59)
   * @param seconds the second (0-59)
   * @return milliseconds since epoch representing the last instant of the second
   */
  public static long getLastInstantOfYearMonthDayHourMinuteSecond(
      int year, int month, int day, int hours, int minutes, int seconds) {
    return getInstantOfDate(year, month, day, hours, minutes, seconds, 999);
  }

  /**
   * Converts individual date/time components to a millisecond timestamp.
   *
   * <p>This is the core method that all other instant calculation methods use. It creates a {@link
   * Calendar} instance with the system default timezone and sets all provided date/time components
   * to calculate the resulting timestamp.
   *
   * <p><b>Important:</b> This method uses the system default timezone, which may cause
   * inconsistencies when working with dates across different timezones or during daylight saving
   * time transitions.
   *
   * @param year the year (e.g., 2023)
   * @param month the month using Calendar convention (0 = January, 11 = December)
   * @param day the day of month (1-31)
   * @param hours the hour of day (0-23)
   * @param minutes the minute (0-59)
   * @param seconds the second (0-59)
   * @param millis the millisecond (0-999)
   * @return milliseconds since epoch representing the specified instant
   */
  public static long getInstantOfDate(
      int year, int month, int day, int hours, int minutes, int seconds, int millis) {

    // TODO: Currently we don't handle the impact of different timezones when working with dates.
    // This uses the system default timezone which may cause inconsistencies across different
    // environments or during daylight saving time transitions. Consider using UTC consistently.
    Calendar calendar = Calendar.getInstance();

    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.MONTH, month);
    calendar.set(Calendar.DAY_OF_MONTH, day);
    calendar.set(Calendar.HOUR_OF_DAY, hours);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, seconds);
    calendar.set(Calendar.MILLISECOND, millis);

    return calendar.getTime().getTime();
  }

  /**
   * Extracts the first millisecond instant from a FHIR DateTimeType object based on its precision.
   *
   * <p>This method analyzes the precision of the provided FHIR DateTimeType and delegates to the
   * appropriate {@code getFirstInstantOf*} method to calculate the first millisecond of the
   * corresponding time period. This is essential for FHIR date range queries where a date with
   * lower precision (e.g., "2023") should match any instant within that period.
   *
   * <h3>Precision Handling</h3>
   *
   * <ul>
   *   <li><b>YEAR precision</b>: Returns first millisecond of January 1st, 00:00:00.000
   *   <li><b>MONTH precision</b>: Returns first millisecond of the 1st day, 00:00:00.000
   *   <li><b>DAY precision</b>: Returns first millisecond of the day, 00:00:00.000
   *   <li><b>MINUTE precision</b>: Returns first millisecond of the minute, XX:XX:00.000
   *   <li><b>SECOND precision</b>: Returns first millisecond of the second, XX:XX:XX.000
   *   <li><b>MILLI precision</b>: Returns the exact millisecond timestamp (no range)
   * </ul>
   *
   * <h3>Usage Example</h3>
   *
   * <pre>{@code
   * DateTimeType yearPrecision = new DateTimeType("2023");
   * long firstInstant = DateUtil.getFirstInstantOfDateTime(yearPrecision);
   * // Returns: 1672531200000 (2023-01-01T00:00:00.000Z)
   *
   * DateTimeType monthPrecision = new DateTimeType("2023-06");
   * firstInstant = DateUtil.getFirstInstantOfDateTime(monthPrecision);
   * // Returns: 1685577600000 (2023-06-01T00:00:00.000Z)
   * }</pre>
   *
   * @param dateTime the FHIR DateTimeType object containing both the date value and its precision
   * @return the first millisecond (inclusive) of the time period represented by the DateTimeType
   * @throws IllegalArgumentException if the DateTimeType contains an unsupported precision value
   * @see #getLastInstantOfDateTime(DateTimeType)
   * @see #getFirstInstantOfYear(int)
   * @see #getFirstInstantOfYearMonth(int, int)
   * @see #getFirstInstantOfYearMonthDay(int, int, int)
   */
  public static long getFirstInstantOfDateTime(DateTimeType dateTime) {

    // TODO: Currently we don't handle the impact of different timezones when working with dates.
    // The DateTimeType may contain timezone information that is not properly considered here.
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateTime.getValue());

    switch (dateTime.getPrecision()) {
      case YEAR:
        // First millisecond of the year
        return getFirstInstantOfYear(calendar.get(Calendar.YEAR));
      case MONTH:
        // First millisecond of the month
        return getFirstInstantOfYearMonth(
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
      case DAY:
        // First millisecond of the day
        return getFirstInstantOfYearMonthDay(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));
      case MINUTE:
        // First millisecond of the minute
        return getFirstInstantOfYearMonthDayHourMinute(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE));
      case SECOND:
        // First millisecond of the second
        return getFirstInstantOfYearMonthDayHourMinuteSecond(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND));
      case MILLI:
        // Highest precision we can get, therefore from and to are equal
        return dateTime.getValue().getTime();
      default:
        // This case should never be reached with valid DateTimeType precision values
        throw new IllegalArgumentException(
            "Unsupported DateTimeType precision: " + dateTime.getPrecision());
    }
  }

  /**
   * Extracts the last millisecond instant from a FHIR DateTimeType object based on its precision.
   *
   * <p>This method analyzes the precision of the provided FHIR DateTimeType and delegates to the
   * appropriate {@code getLastInstantOf*} method to calculate the last millisecond of the
   * corresponding time period. This is essential for FHIR date range queries where a date with
   * lower precision (e.g., "2023") should match any instant within that period.
   *
   * <h3>Precision Handling</h3>
   *
   * <ul>
   *   <li><b>YEAR precision</b>: Returns last millisecond of December 31st, 23:59:59.999
   *   <li><b>MONTH precision</b>: Returns last millisecond of the last day, 23:59:59.999
   *   <li><b>DAY precision</b>: Returns last millisecond of the day, 23:59:59.999
   *   <li><b>MINUTE precision</b>: Returns last millisecond of the minute, XX:XX:59.999
   *   <li><b>SECOND precision</b>: Returns last millisecond of the second, XX:XX:XX.999
   *   <li><b>MILLI precision</b>: Returns the exact millisecond timestamp (no range)
   * </ul>
   *
   * <h3>Usage Example</h3>
   *
   * <pre>{@code
   * DateTimeType yearPrecision = new DateTimeType("2023");
   * long lastInstant = DateUtil.getLastInstantOfDateTime(yearPrecision);
   * // Returns: 1704067199999 (2023-12-31T23:59:59.999Z)
   *
   * DateTimeType monthPrecision = new DateTimeType("2023-06");
   * lastInstant = DateUtil.getLastInstantOfDateTime(monthPrecision);
   * // Returns: 1688169599999 (2023-06-30T23:59:59.999Z)
   * }</pre>
   *
   * @param dateTime the FHIR DateTimeType object containing both the date value and its precision
   * @return the last millisecond (inclusive) of the time period represented by the DateTimeType
   * @throws IllegalArgumentException if the DateTimeType contains an unsupported precision value
   * @see #getFirstInstantOfDateTime(DateTimeType)
   * @see #getLastInstantOfYear(int)
   * @see #getLastInstantOfYearMonth(int, int)
   * @see #getLastInstantOfYearMonthDay(int, int, int)
   */
  public static long getLastInstantOfDateTime(DateTimeType dateTime) {

    // TODO: Currently we don't handle the impact of different timezones when working with dates.
    // The DateTimeType may contain timezone information that is not properly considered here.
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateTime.getValue());

    switch (dateTime.getPrecision()) {
      case YEAR:
        // Last millisecond of the year
        return getLastInstantOfYear(calendar.get(Calendar.YEAR));
      case MONTH:
        // Last millisecond of the month
        return getLastInstantOfYearMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
      case DAY:
        // Last millisecond of the day
        return getLastInstantOfYearMonthDay(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));
      case MINUTE:
        // Last millisecond of the minute
        return getLastInstantOfYearMonthDayHourMinute(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE));
      case SECOND:
        // Last millisecond of the second
        return getLastInstantOfYearMonthDayHourMinuteSecond(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND));
      case MILLI:
        // Highest precision we can get, therefore from and to are equal
        return dateTime.getValue().getTime();
      default:
        // This case should never be reached with valid DateTimeType precision values
        throw new IllegalArgumentException(
            "Unsupported DateTimeType precision: " + dateTime.getPrecision());
    }
  }
}

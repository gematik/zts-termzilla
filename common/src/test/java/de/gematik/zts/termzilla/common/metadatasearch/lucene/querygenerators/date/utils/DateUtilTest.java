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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for DateUtil.
 *
 * <p>The DateUtil is a static utility class that converts FHIR date/time values with various
 * precision levels to millisecond timestamps. These timestamps are used for Lucene-based date range
 * queries.
 *
 * <p><b>Test Strategy:</b>
 *
 * <ul>
 *   <li>Test year precision (first and last instant of year)
 *   <li>Test month precision (first and last instant of month)
 *   <li>Test day precision (first and last instant of day)
 *   <li>Test minute precision (first and last instant of minute)
 *   <li>Test second precision (first and last instant of second)
 *   <li>Test millisecond precision (exact instant)
 *   <li>Test DateTimeType conversion with various precisions
 *   <li>Test edge cases (leap years, month boundaries)
 *   <li>Test exception handling for unsupported precision
 *   <li>Test utility class instantiation prevention
 * </ul>
 */
@DisplayName("DateUtil Tests")
class DateUtilTest {

  // ================================================================================
  // Year Precision Tests
  // ================================================================================

  @Nested
  @DisplayName("Year Precision Tests")
  class YearPrecisionTests {

    @Test
    @DisplayName("Should return first instant of year (Jan 1, 00:00:00.000)")
    void shouldReturnFirstInstantOfYear() {
      // WHEN
      long firstInstant = DateUtil.getFirstInstantOfYear(2023);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
      assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(0, cal.get(Calendar.MINUTE));
      assertEquals(0, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should return last instant of year (Dec 31, 23:59:59.999)")
    void shouldReturnLastInstantOfYear() {
      // WHEN
      long lastInstant = DateUtil.getLastInstantOfYear(2023);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
      assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should handle leap year correctly")
    void shouldHandleLeapYear() {
      // WHEN - 2024 is a leap year
      long lastInstantFeb2024 = DateUtil.getLastInstantOfYearMonth(2024, Calendar.FEBRUARY);

      // THEN - February 2024 should have 29 days
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstantFeb2024);

      assertEquals(2024, cal.get(Calendar.YEAR));
      assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));
      assertEquals(29, cal.get(Calendar.DAY_OF_MONTH)); // Leap year has 29 days
      assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should handle non-leap year correctly")
    void shouldHandleNonLeapYear() {
      // WHEN - 2023 is not a leap year
      long lastInstantFeb2023 = DateUtil.getLastInstantOfYearMonth(2023, Calendar.FEBRUARY);

      // THEN - February 2023 should have 28 days
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstantFeb2023);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));
      assertEquals(28, cal.get(Calendar.DAY_OF_MONTH)); // Non-leap year has 28 days
    }
  }

  // ================================================================================
  // Month Precision Tests
  // ================================================================================

  @Nested
  @DisplayName("Month Precision Tests")
  class MonthPrecisionTests {

    @Test
    @DisplayName("Should return first instant of month")
    void shouldReturnFirstInstantOfMonth() {
      // WHEN - June 2023 (month = 5 in Calendar)
      long firstInstant = DateUtil.getFirstInstantOfYearMonth(2023, Calendar.JUNE);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(0, cal.get(Calendar.MINUTE));
      assertEquals(0, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should return last instant of month")
    void shouldReturnLastInstantOfMonth() {
      // WHEN - June 2023 has 30 days
      long lastInstant = DateUtil.getLastInstantOfYearMonth(2023, Calendar.JUNE);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(30, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should handle December correctly (year boundary)")
    void shouldHandleDecemberCorrectly() {
      // WHEN - December is month 11, last month of year
      long lastInstant = DateUtil.getLastInstantOfYearMonth(2023, Calendar.DECEMBER);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
      assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("Should handle January correctly")
    void shouldHandleJanuaryCorrectly() {
      // WHEN - January is month 0
      long firstInstant = DateUtil.getFirstInstantOfYearMonth(2023, Calendar.JANUARY);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
      assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }
  }

  // ================================================================================
  // Day Precision Tests
  // ================================================================================

  @Nested
  @DisplayName("Day Precision Tests")
  class DayPrecisionTests {

    @Test
    @DisplayName("Should return first instant of day")
    void shouldReturnFirstInstantOfDay() {
      // WHEN - June 15, 2023
      long firstInstant = DateUtil.getFirstInstantOfYearMonthDay(2023, Calendar.JUNE, 15);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(0, cal.get(Calendar.MINUTE));
      assertEquals(0, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should return last instant of day")
    void shouldReturnLastInstantOfDay() {
      // WHEN - June 15, 2023
      long lastInstant = DateUtil.getLastInstantOfYearMonthDay(2023, Calendar.JUNE, 15);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }
  }

  // ================================================================================
  // Minute Precision Tests
  // ================================================================================

  @Nested
  @DisplayName("Minute Precision Tests")
  class MinutePrecisionTests {

    @Test
    @DisplayName("Should return first instant of minute")
    void shouldReturnFirstInstantOfMinute() {
      // WHEN - June 15, 2023 14:30
      long firstInstant =
          DateUtil.getFirstInstantOfYearMonthDayHourMinute(2023, Calendar.JUNE, 15, 14, 30);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(30, cal.get(Calendar.MINUTE));
      assertEquals(0, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should return last instant of minute")
    void shouldReturnLastInstantOfMinute() {
      // WHEN - June 15, 2023 14:30
      long lastInstant =
          DateUtil.getLastInstantOfYearMonthDayHourMinute(2023, Calendar.JUNE, 15, 14, 30);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(30, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }
  }

  // ================================================================================
  // Second Precision Tests
  // ================================================================================

  @Nested
  @DisplayName("Second Precision Tests")
  class SecondPrecisionTests {

    @Test
    @DisplayName("Should return first instant of second")
    void shouldReturnFirstInstantOfSecond() {
      // WHEN - June 15, 2023 14:30:45
      long firstInstant =
          DateUtil.getFirstInstantOfYearMonthDayHourMinuteSecond(
              2023, Calendar.JUNE, 15, 14, 30, 45);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(30, cal.get(Calendar.MINUTE));
      assertEquals(45, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should return last instant of second")
    void shouldReturnLastInstantOfSecond() {
      // WHEN - June 15, 2023 14:30:45
      long lastInstant =
          DateUtil.getLastInstantOfYearMonthDayHourMinuteSecond(
              2023, Calendar.JUNE, 15, 14, 30, 45);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(30, cal.get(Calendar.MINUTE));
      assertEquals(45, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }
  }

  // ================================================================================
  // Core Method Tests
  // ================================================================================

  @Nested
  @DisplayName("Core getInstantOfDate Tests")
  class CoreMethodTests {

    @Test
    @DisplayName("Should create instant with all components")
    void shouldCreateInstantWithAllComponents() {
      // WHEN - June 15, 2023 14:30:45.123
      long instant = DateUtil.getInstantOfDate(2023, Calendar.JUNE, 15, 14, 30, 45, 123);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(instant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(30, cal.get(Calendar.MINUTE));
      assertEquals(45, cal.get(Calendar.SECOND));
      assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should handle midnight (00:00:00.000)")
    void shouldHandleMidnight() {
      // WHEN
      long instant = DateUtil.getInstantOfDate(2023, Calendar.JUNE, 15, 0, 0, 0, 0);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(instant);

      assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(0, cal.get(Calendar.MINUTE));
      assertEquals(0, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should handle end of day (23:59:59.999)")
    void shouldHandleEndOfDay() {
      // WHEN
      long instant = DateUtil.getInstantOfDate(2023, Calendar.JUNE, 15, 23, 59, 59, 999);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(instant);

      assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }
  }

  // ================================================================================
  // DateTimeType Conversion Tests
  // ================================================================================

  @Nested
  @DisplayName("DateTimeType Conversion Tests")
  class DateTimeTypeConversionTests {

    @Test
    @DisplayName("Should extract first instant from YEAR precision DateTimeType")
    void shouldExtractFirstInstantFromYearPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023");

      // WHEN
      long firstInstant = DateUtil.getFirstInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
      assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(0, cal.get(Calendar.MINUTE));
      assertEquals(0, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should extract last instant from YEAR precision DateTimeType")
    void shouldExtractLastInstantFromYearPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023");

      // WHEN
      long lastInstant = DateUtil.getLastInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
      assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should extract first instant from MONTH precision DateTimeType")
    void shouldExtractFirstInstantFromMonthPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023-06");

      // WHEN
      long firstInstant = DateUtil.getFirstInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    @DisplayName("Should extract last instant from MONTH precision DateTimeType")
    void shouldExtractLastInstantFromMonthPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023-06");

      // WHEN
      long lastInstant = DateUtil.getLastInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(30, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, cal.get(Calendar.MINUTE));
    }

    @Test
    @DisplayName("Should extract first instant from DAY precision DateTimeType")
    void shouldExtractFirstInstantFromDayPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023-06-15");

      // WHEN
      long firstInstant = DateUtil.getFirstInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(0, cal.get(Calendar.MINUTE));
      assertEquals(0, cal.get(Calendar.SECOND));
    }

    @Test
    @DisplayName("Should extract last instant from DAY precision DateTimeType")
    void shouldExtractLastInstantFromDayPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023-06-15");

      // WHEN
      long lastInstant = DateUtil.getLastInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(59, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
    }

    @Test
    @DisplayName("Should extract first instant from MINUTE precision DateTimeType")
    void shouldExtractFirstInstantFromMinutePrecision() {
      // GIVEN - FHIR format for minute precision
      DateTimeType dateTime = new DateTimeType("2023-06-15T14:30:00");
      dateTime.setPrecision(TemporalPrecisionEnum.MINUTE);

      // WHEN
      long firstInstant = DateUtil.getFirstInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(2023, cal.get(Calendar.YEAR));
      assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
      assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
      assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(30, cal.get(Calendar.MINUTE));
      assertEquals(0, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should extract last instant from MINUTE precision DateTimeType")
    void shouldExtractLastInstantFromMinutePrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023-06-15T14:30:00");
      dateTime.setPrecision(TemporalPrecisionEnum.MINUTE);

      // WHEN
      long lastInstant = DateUtil.getLastInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
      assertEquals(30, cal.get(Calendar.MINUTE));
      assertEquals(59, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should extract first instant from SECOND precision DateTimeType")
    void shouldExtractFirstInstantFromSecondPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023-06-15T14:30:45");

      // WHEN
      long firstInstant = DateUtil.getFirstInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(firstInstant);

      assertEquals(45, cal.get(Calendar.SECOND));
      assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should extract last instant from SECOND precision DateTimeType")
    void shouldExtractLastInstantFromSecondPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023-06-15T14:30:45");

      // WHEN
      long lastInstant = DateUtil.getLastInstantOfDateTime(dateTime);

      // THEN
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(lastInstant);

      assertEquals(45, cal.get(Calendar.SECOND));
      assertEquals(999, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Should return exact instant for MILLI precision DateTimeType")
    void shouldReturnExactInstantForMilliPrecision() {
      // GIVEN
      DateTimeType dateTime = new DateTimeType("2023-06-15T14:30:45.123");

      // WHEN
      long firstInstant = DateUtil.getFirstInstantOfDateTime(dateTime);
      long lastInstant = DateUtil.getLastInstantOfDateTime(dateTime);

      // THEN - For MILLI precision, first and last should be equal
      assertEquals(
          firstInstant, lastInstant, "First and last instant should be equal for MILLI precision");
      assertEquals(dateTime.getValue().getTime(), firstInstant, "Should return exact timestamp");
    }
  }

  // ================================================================================
  // Exception Handling Tests
  // ================================================================================

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @ParameterizedTest
    @DisplayName("Should throw NullPointerException when DateTimeType has null precision")
    @MethodSource("provideInstantMethods")
    void shouldThrowNullPointerExceptionForNullPrecision(
        Function<DateTimeType, Long> instantMethod, String methodDescription) {
      // GIVEN - Mock DateTimeType with null precision
      DateTimeType dateTime = mock(DateTimeType.class);
      when(dateTime.getValue()).thenReturn(new Date());
      when(dateTime.getPrecision()).thenReturn(null);

      // WHEN & THEN - Verify NullPointerException is thrown
      assertThatThrownBy(() -> instantMethod.apply(dateTime))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Cannot invoke")
          .hasMessageContaining("getPrecision()");
    }

    private static Stream<Arguments> provideInstantMethods() {
      return Stream.of(
          Arguments.of(
              (Function<DateTimeType, Long>) DateUtil::getFirstInstantOfDateTime,
              "getFirstInstantOfDateTime"),
          Arguments.of(
              (Function<DateTimeType, Long>) DateUtil::getLastInstantOfDateTime,
              "getLastInstantOfDateTime"));
    }
  }

  // ================================================================================
  // Range Consistency Tests
  // ================================================================================

  @Nested
  @DisplayName("Range Consistency Tests")
  class RangeConsistencyTests {

    @Test
    @DisplayName("First instant should be before last instant for same year")
    void firstShouldBeBeforeLastForYear() {
      // WHEN
      long first = DateUtil.getFirstInstantOfYear(2023);
      long last = DateUtil.getLastInstantOfYear(2023);

      // THEN
      assertTrue(first < last, "First instant should be before last instant");
    }

    @Test
    @DisplayName("First instant should be before last instant for same month")
    void firstShouldBeBeforeLastForMonth() {
      // WHEN
      long first = DateUtil.getFirstInstantOfYearMonth(2023, Calendar.JUNE);
      long last = DateUtil.getLastInstantOfYearMonth(2023, Calendar.JUNE);

      // THEN
      assertTrue(first < last, "First instant should be before last instant");
    }

    @Test
    @DisplayName("First instant should be before last instant for same day")
    void firstShouldBeBeforeLastForDay() {
      // WHEN
      long first = DateUtil.getFirstInstantOfYearMonthDay(2023, Calendar.JUNE, 15);
      long last = DateUtil.getLastInstantOfYearMonthDay(2023, Calendar.JUNE, 15);

      // THEN
      assertTrue(first < last, "First instant should be before last instant");
    }

    @Test
    @DisplayName("Last instant of year should be exactly 1ms before first instant of next year")
    void lastInstantOfYearShouldBeOneMillisecondBeforeNextYear() {
      // WHEN
      long lastOf2023 = DateUtil.getLastInstantOfYear(2023);
      long firstOf2024 = DateUtil.getFirstInstantOfYear(2024);

      // THEN
      assertEquals(firstOf2024 - 1, lastOf2023, "Last instant should be 1ms before next year");
    }

    @Test
    @DisplayName("Time range should cover full day (exactly 86400000ms - 1ms)")
    void timeBetweenFirstAndLastShouldBeDayMinusOne() {
      // WHEN
      long first = DateUtil.getFirstInstantOfYearMonthDay(2023, Calendar.JUNE, 15);
      long last = DateUtil.getLastInstantOfYearMonthDay(2023, Calendar.JUNE, 15);

      // THEN - One day = 86400000 milliseconds, range is 86399999ms (missing 1ms)
      long expectedRange = (24 * 60 * 60 * 1000) - 1; // 86399999ms
      assertEquals(expectedRange, last - first, "Range should be one full day minus 1ms");
    }
  }

  // ================================================================================
  // Utility Class Instantiation Test
  // ================================================================================

  @Nested
  @DisplayName("Utility Class Tests")
  class UtilityClassTests {

    @Test
    @DisplayName("Should not be instantiable (private constructor)")
    void shouldNotBeInstantiable() throws Exception {
      // GIVEN - Access private constructor via reflection
      Constructor<DateUtil> constructor = DateUtil.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      // WHEN - Attempt to instantiate utility class
      Exception exception =
          assertThrows(
              InvocationTargetException.class,
              constructor::newInstance,
              "Constructor should throw exception when invoked");

      // THEN - Verify AssertionError is thrown with correct message
      assertThat(exception.getCause())
          .isInstanceOf(AssertionError.class)
          .hasMessage("DateUtil is a utility class and should not be instantiated");
    }
  }
}

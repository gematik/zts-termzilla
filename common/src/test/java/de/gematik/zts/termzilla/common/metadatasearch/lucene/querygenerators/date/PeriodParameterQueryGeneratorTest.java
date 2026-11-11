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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_DATE_END;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_DATE_START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils.DateUtil;
import java.util.stream.Stream;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for PeriodParameterQueryGenerator
 *
 * <p>The PeriodParameterQueryGenerator is a static utility class for FHIR period-type date search
 * parameters. It handles period searches with interval overlap logic using various prefixes (eq,
 * ne, lt, le, gt, ge) and precision levels (year, month, day, minute, second, millisecond).
 *
 * <p><b>Key Difference from Instant:</b> Period queries use overlap logic with two fields
 * (_date_start and _date_end) instead of a single instant field. A period matches if it overlaps
 * with the search range.
 *
 * <p><b>Test Strategy:</b>
 *
 * <ul>
 *   <li>Test equals (eq) prefix with overlap logic for various precisions
 *   <li>Test not equals (ne) prefix (negation of overlap)
 *   <li>Test less than (lt) and less than or equals (le) prefixes (period end comparisons)
 *   <li>Test greater than (gt) and greater than or equals (ge) prefixes (period start comparisons)
 *   <li>Test unsupported prefixes (sa, eb, ap) throw NotImplementedException
 *   <li>Test field name construction for both start and end fields
 *   <li>Test overlap query structure (OR of three conditions)
 * </ul>
 *
 * <p><b>Period Overlap Logic:</b> A period [start, end] overlaps with search range [from, to] if:
 *
 * <ul>
 *   <li>Condition 1: Period start falls within search range: start IN [from, to]
 *   <li>Condition 2: Period end falls within search range: end IN [from, to]
 *   <li>Condition 3: Period encompasses search range: (start <= to) AND (end >= from)
 * </ul>
 */
@DisplayName("PeriodParameterQueryGenerator Tests")
class PeriodParameterQueryGeneratorTest {

  private static final String PARAM_NAME = "date";
  private static final String EXPECTED_START_FIELD = PARAM_NAME + SEPARATOR + SUFFIX_DATE_START;
  private static final String EXPECTED_END_FIELD = PARAM_NAME + SEPARATOR + SUFFIX_DATE_END;

  // Regex patterns for period queries (more complex due to OR structure)
  // Period query structure: (start:[from TO to] end:[from TO to] (start:[MIN TO to] +end:[from TO
  // MAX]))
  private static final String PERIOD_OVERLAP_PATTERN =
      "^"
          + EXPECTED_START_FIELD
          + ":\\[\\d+ TO \\d+\\] "
          + EXPECTED_END_FIELD
          + ":\\[\\d+ TO \\d+\\] "
          + "\\(\\+"
          + EXPECTED_START_FIELD
          + ":\\[-?\\d+ TO \\d+\\] \\+"
          + EXPECTED_END_FIELD
          + ":\\[\\d+ TO \\d+\\]\\)$";

  private static final String NOT_EQUALS_PATTERN =
      "^-\\("
          + EXPECTED_START_FIELD
          + ":\\[\\d+ TO \\d+\\] "
          + EXPECTED_END_FIELD
          + ":\\[\\d+ TO \\d+\\] "
          + "\\(\\+"
          + EXPECTED_START_FIELD
          + ":\\[-?\\d+ TO \\d+\\] \\+"
          + EXPECTED_END_FIELD
          + ":\\[\\d+ TO \\d+\\]\\)\\) \\+\\*\\:\\*$";

  private static String expectedPeriodOverlapQuery(long from, long to) {
    return EXPECTED_START_FIELD
        + ":["
        + from
        + " TO "
        + to
        + "] "
        + EXPECTED_END_FIELD
        + ":["
        + from
        + " TO "
        + to
        + "] (+"
        + EXPECTED_START_FIELD
        + ":["
        + Long.MIN_VALUE
        + " TO "
        + to
        + "] +"
        + EXPECTED_END_FIELD
        + ":["
        + from
        + " TO "
        + Long.MAX_VALUE
        + "])";
  }

  private static String expectedEqualsOverlapQuery(String rawValue) {
    DateTimeType dateTimeType = new DateTimeType(rawValue);
    long from = DateUtil.getFirstInstantOfDateTime(dateTimeType);
    long to = DateUtil.getLastInstantOfDateTime(dateTimeType);
    return expectedPeriodOverlapQuery(from, to);
  }

  private static String expectedNotEqualsOverlapQuery(String rawValue) {
    return "-(" + expectedEqualsOverlapQuery(rawValue) + ") +*:*";
  }

  private static String expectedLessThanOverlapQuery(String rawValue) {
    DateTimeType dateTimeType = new DateTimeType(rawValue);
    long to = DateUtil.getFirstInstantOfDateTime(dateTimeType) - 1;
    return expectedPeriodOverlapQuery(0L, to);
  }

  private static String expectedLessThanOrEqualsOverlapQuery(String rawValue) {
    DateTimeType dateTimeType = new DateTimeType(rawValue);
    long to = DateUtil.getLastInstantOfDateTime(dateTimeType);
    return expectedPeriodOverlapQuery(0L, to);
  }

  private static String expectedGreaterThanOverlapQuery(String rawValue) {
    DateTimeType dateTimeType = new DateTimeType(rawValue);
    long from = DateUtil.getLastInstantOfDateTime(dateTimeType) + 1;
    return expectedPeriodOverlapQuery(from, Long.MAX_VALUE);
  }

  private static String expectedGreaterThanOrEqualsOverlapQuery(String rawValue) {
    DateTimeType dateTimeType = new DateTimeType(rawValue);
    long from = DateUtil.getFirstInstantOfDateTime(dateTimeType);
    return expectedPeriodOverlapQuery(from, Long.MAX_VALUE);
  }

  // ================================================================================
  // Equals (eq) Prefix Tests - Period Overlap Logic
  // ================================================================================

  @Nested
  @DisplayName("Equals (eq) Prefix Tests - Period Overlap")
  class EqualsTests {

    @Test
    @DisplayName("Should create overlap query for year precision (eq2023)")
    void shouldCreateOverlapQueryForYearPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.EQUAL, "2023");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(((BooleanQuery) query).clauses())
          .as("Query should have three clauses for overlap")
          .hasSize(3);
      assertThat(queryString)
          .as("Query should match period overlap pattern")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Year precision should use exact overlap boundaries")
          .isEqualTo(expectedEqualsOverlapQuery(rawValue));
    }

    private static Stream<Arguments> equalsPrecisionOverlapCases() {
      return Stream.of(
          Arguments.of("month", "2023-06"),
          Arguments.of("day", "2023-06-15"),
          Arguments.of("minute", "2023-06-15T14:30:00"),
          Arguments.of("second", "2023-06-15T14:30:45"));
    }

    @DisplayName("Should create overlap query for month/day/minute/second precisions")
    @ParameterizedTest(name = "precision={0}, value={1}")
    @MethodSource("equalsPrecisionOverlapCases")
    void shouldCreateOverlapQueryForPrecision(String precision, String rawValue)
        throws NotImplementedException {
      // GIVEN
      DateParam param = new DateParam(ParamPrefixEnum.EQUAL, rawValue);

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(((BooleanQuery) query).clauses())
          .as("Query should have three clauses for overlap")
          .hasSize(3);
      assertThat(queryString)
          .as("Query should match period overlap pattern")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("%s precision should use exact overlap boundaries", precision)
          .isEqualTo(expectedEqualsOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should create overlap query for millisecond precision")
    void shouldCreateOverlapQueryForMillisecondPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06-15T14:30:45.123";
      DateParam param = new DateParam(ParamPrefixEnum.EQUAL, "2023-06-15T14:30:45.123");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(((BooleanQuery) query).clauses())
          .as("Query should have three clauses for overlap")
          .hasSize(3);
      assertThat(queryString)
          .as("Query should match period overlap pattern")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Millisecond precision should use exact overlap boundaries")
          .isEqualTo(expectedEqualsOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle implicit equals prefix (no prefix)")
    void shouldHandleImplicitEqualsPrefix() throws NotImplementedException {
      // GIVEN - DateParam without explicit prefix defaults to EQUAL
      String rawValue = "2023-06-15";
      DateParam param = new DateParam("2023-06-15");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(((BooleanQuery) query).clauses())
          .as("Query should have three clauses for overlap")
          .hasSize(3);
      assertThat(queryString)
          .as("Query should match period overlap pattern")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Implicit equals should use exact overlap boundaries")
          .isEqualTo(expectedEqualsOverlapQuery(rawValue));
    }
  }

  // ================================================================================
  // Not Equals (ne) Prefix Tests - Negation of Overlap
  // ================================================================================

  @Nested
  @DisplayName("Not Equals (ne) Prefix Tests")
  class NotEqualsTests {

    @Test
    @DisplayName("Should create negated overlap query for not equals (ne2023)")
    void shouldCreateNegatedOverlapQueryForNotEquals() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.NOT_EQUAL, "2023");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(((BooleanQuery) query).clauses()).as("Query should have two clauses").hasSize(2);
      assertThat(queryString)
          .as("Query should match not-equals pattern")
          .matches(NOT_EQUALS_PATTERN)
          .as("Not-equals should negate exact year overlap boundaries")
          .isEqualTo(expectedNotEqualsOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should create negated overlap query for day precision (ne2023-06-15)")
    void shouldCreateNegatedOverlapQueryForDayPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06-15";
      DateParam param = new DateParam(ParamPrefixEnum.NOT_EQUAL, "2023-06-15");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match not-equals pattern")
          .matches(NOT_EQUALS_PATTERN)
          .as("Not-equals should negate exact day overlap boundaries")
          .isEqualTo(expectedNotEqualsOverlapQuery(rawValue));
    }
  }

  // ================================================================================
  // Less Than (lt) and Less Than or Equals (le) Tests - Period End Comparisons
  // ================================================================================

  @Nested
  @DisplayName("Less Than Comparison Tests")
  class LessThanTests {

    @Test
    @DisplayName("Should create less than query for period end (lt2023)")
    void shouldCreateLessThanQuery() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.LESSTHAN, "2023");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should use overlap structure")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Less-than should use first instant minus 1ms as upper bound")
          .isEqualTo(expectedLessThanOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should create less than or equals query for period end (le2023)")
    void shouldCreateLessThanOrEqualsQuery() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2023");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should use overlap structure")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Less-than-or-equals should use last instant as upper bound")
          .isEqualTo(expectedLessThanOrEqualsOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle less than with month precision (lt2023-06)")
    void shouldHandleLessThanWithMonthPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06";
      DateParam param = new DateParam(ParamPrefixEnum.LESSTHAN, "2023-06");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should use overlap structure")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Less-than month precision should use exact overlap boundaries")
          .isEqualTo(expectedLessThanOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle less than or equals with day precision (le2023-06-15)")
    void shouldHandleLessThanOrEqualsWithDayPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06-15";
      DateParam param = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2023-06-15");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should use overlap structure")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Less-than-or-equals day precision should use exact overlap boundaries")
          .isEqualTo(expectedLessThanOrEqualsOverlapQuery(rawValue));
    }
  }

  // ================================================================================
  // Greater Than (gt) and Greater Than or Equals (ge) Tests - Period Start Comparisons
  // ================================================================================

  @Nested
  @DisplayName("Greater Than Comparison Tests")
  class GreaterThanTests {

    @Test
    @DisplayName("Should create greater than query for period start (gt2023)")
    void shouldCreateGreaterThanQuery() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.GREATERTHAN, "2023");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should reference start field")
          .contains(EXPECTED_START_FIELD)
          .as("Query should reference end field")
          .contains(EXPECTED_END_FIELD)
          .as("Query should use overlap structure")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Greater-than should start at last instant plus 1ms")
          .isEqualTo(expectedGreaterThanOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should create greater than or equals query for period start (ge2023)")
    void shouldCreateGreaterThanOrEqualsQuery() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2023");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should use overlap structure")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Greater-than-or-equals should start at first instant")
          .isEqualTo(expectedGreaterThanOrEqualsOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle greater than with month precision (gt2023-06)")
    void shouldHandleGreaterThanWithMonthPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06";
      DateParam param = new DateParam(ParamPrefixEnum.GREATERTHAN, "2023-06");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should use overlap structure")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Greater-than month precision should use exact overlap boundaries")
          .isEqualTo(expectedGreaterThanOverlapQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle greater than or equals with day precision (ge2023-06-15)")
    void shouldHandleGreaterThanOrEqualsWithDayPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06-15";
      DateParam param = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2023-06-15");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should use overlap structure")
          .matches(PERIOD_OVERLAP_PATTERN)
          .as("Greater-than-or-equals day precision should use exact overlap boundaries")
          .isEqualTo(expectedGreaterThanOrEqualsOverlapQuery(rawValue));
    }
  }

  // ================================================================================
  // Unsupported Prefix Tests
  // ================================================================================

  @Nested
  @DisplayName("Unsupported Prefix Tests")
  class UnsupportedPrefixTests {

    @Test
    @DisplayName("Should throw NotImplementedException for starts after (sa) prefix")
    void shouldThrowExceptionForStartsAfterPrefix() {
      // GIVEN
      DateParam param = new DateParam(ParamPrefixEnum.STARTS_AFTER, "2023");

      // THEN
      assertThatThrownBy(() -> new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param))
          .as("Should throw NotImplementedException for STARTS_AFTER")
          .isInstanceOf(NotImplementedException.class)
          .hasMessageMatching("(?i).*not implemented.*");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for ends before (eb) prefix")
    void shouldThrowExceptionForEndsBeforePrefix() {
      // GIVEN
      DateParam param = new DateParam(ParamPrefixEnum.ENDS_BEFORE, "2023");

      // THEN
      assertThatThrownBy(() -> new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param))
          .as("Should throw NotImplementedException for ENDS_BEFORE")
          .isInstanceOf(NotImplementedException.class)
          .hasMessageMatching("(?i).*not implemented.*");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for approximate (ap) prefix")
    void shouldThrowExceptionForApproximatePrefix() {
      // GIVEN
      DateParam param = new DateParam(ParamPrefixEnum.APPROXIMATE, "2023");

      // THEN
      assertThatThrownBy(() -> new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param))
          .as("Should throw NotImplementedException for APPROXIMATE")
          .isInstanceOf(NotImplementedException.class)
          .hasMessageMatching("(?i).*not implemented.*");
    }
  }

  // ================================================================================
  // Field Name Construction Tests - Start and End Fields
  // ================================================================================

  @Nested
  @DisplayName("Field Name Construction Tests")
  class FieldNameTests {

    @Test
    @DisplayName("Should construct correct start and end field names")
    void shouldConstructCorrectFieldNames() throws NotImplementedException {
      // GIVEN
      DateParam param = new DateParam("2023");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);

      // THEN
      String queryString = query.toString();
      assertThat(queryString)
          .as("Query should contain field with '_date_start' suffix")
          .contains(PARAM_NAME + SEPARATOR + SUFFIX_DATE_START)
          .as("Query should contain field with '_date_end' suffix")
          .contains(PARAM_NAME + SEPARATOR + SUFFIX_DATE_END);
    }

    @Test
    @DisplayName("Should use different parameter names correctly")
    void shouldUseDifferentParameterNames() throws NotImplementedException {
      // GIVEN
      DateParam param = new DateParam("2023");
      String customParamName = "period";
      String expectedStartField = customParamName + SEPARATOR + SUFFIX_DATE_START;
      String expectedEndField = customParamName + SEPARATOR + SUFFIX_DATE_END;

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(customParamName, param);

      // THEN
      String queryString = query.toString();
      assertThat(queryString)
          .as("Query should contain custom start field name")
          .contains(expectedStartField)
          .as("Query should contain custom end field name")
          .contains(expectedEndField);
    }
  }

  // ================================================================================
  // Direct Method Tests (prepareLongRangeQuery) - Overlap Logic
  // ================================================================================

  @Nested
  @DisplayName("Direct Method Tests - Overlap Logic")
  class DirectMethodTests {

    @Test
    @DisplayName("Should create period overlap query with prepareLongRangeQuery")
    void shouldCreatePeriodOverlapQuery() {
      // GIVEN
      long from = 1000000L;
      long to = 2000000L;

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareLongRangeQuery(PARAM_NAME, from, to);

      // THEN
      String queryString = query.toString();
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Direct method should create exact overlap query structure")
          .isEqualTo(expectedPeriodOverlapQuery(from, to));
    }

    @Test
    @DisplayName("Should create valid overlap query when from equals to")
    void shouldCreateValidOverlapQueryWhenFromEqualsTo() {
      // GIVEN
      long timestamp = 1234567890L;

      // WHEN
      Query query =
          new PeriodParameterQueryGenerator()
              .prepareLongRangeQuery(PARAM_NAME, timestamp, timestamp);

      // THEN
      String queryString = query.toString();
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Point-in-time overlap should still use full overlap query")
          .isEqualTo(expectedPeriodOverlapQuery(timestamp, timestamp));
    }

    @Test
    @DisplayName("Should handle very large timestamp ranges")
    void shouldHandleVeryLargeTimestampRanges() {
      // GIVEN
      long from = 0L;
      long to = Long.MAX_VALUE;

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareLongRangeQuery(PARAM_NAME, from, to);

      // THEN
      String queryString = query.toString();
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Extreme ranges should still produce exact overlap query")
          .isEqualTo(expectedPeriodOverlapQuery(from, to));
    }
  }

  // ================================================================================
  // Overlap Query Structure Tests
  // ================================================================================

  @Nested
  @DisplayName("Overlap Query Structure Tests")
  class OverlapQueryStructureTests {

    @Test
    @DisplayName("Should create OR query with three overlap conditions")
    void shouldCreateOrQueryWithThreeConditions() throws NotImplementedException {
      // GIVEN
      DateParam param = new DateParam("2023-06-15");

      // WHEN
      Query query = new PeriodParameterQueryGenerator().prepareQuery(PARAM_NAME, param);

      // THEN
      String queryString = query.toString();

      // Count occurrences of both fields to verify three conditions
      int startFieldCount = countOccurrences(queryString, EXPECTED_START_FIELD);
      int endFieldCount = countOccurrences(queryString, EXPECTED_END_FIELD);

      // Should have start field in condition 1 and condition 3
      assertThat(startFieldCount)
          .as("Start field should appear at least twice (conditions 1 and 3): %s", queryString)
          .isGreaterThanOrEqualTo(2);

      // Should have end field in condition 2 and condition 3
      assertThat(endFieldCount)
          .as("End field should appear at least twice (conditions 2 and 3): %s", queryString)
          .isGreaterThanOrEqualTo(2);
    }

    private int countOccurrences(String text, String substring) {
      int count = 0;
      int index = 0;
      while ((index = text.indexOf(substring, index)) != -1) {
        count++;
        index += substring.length();
      }
      return count;
    }
  }
}

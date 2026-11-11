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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_DATE_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils.DateUtil;
import java.util.stream.Stream;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for InstantParameterQueryGenerator.
 *
 * <p>The InstantParameterQueryGenerator is a static utility class for FHIR instant-type date search
 * parameters. It handles date searches with various prefixes (eq, ne, lt, le, gt, ge) and precision
 * levels (year, month, day, minute, second, millisecond).
 *
 * <p><b>Test Strategy:</b>
 *
 * <ul>
 *   <li>Test equals (eq) prefix with various precisions
 *   <li>Test not equals (ne) prefix
 *   <li>Test less than (lt) and less than or equals (le) prefixes
 *   <li>Test greater than (gt) and greater than or equals (ge) prefixes
 *   <li>Test unsupported prefixes (sa, eb, ap) throw NotImplementedException
 *   <li>Test field name construction
 * </ul>
 */
@DisplayName("InstantParameterQueryGenerator Tests")
class InstantParameterQueryGeneratorTest {

  private static final String PARAM_NAME = "_lastUpdated";
  private static final String EXPECTED_FIELD = PARAM_NAME + SEPARATOR + SUFFIX_DATE_INSTANT;
  private static final String EQUALS_REGEXP_STRING =
      "^" + EXPECTED_FIELD + ":\\[\\d{13} TO \\d{13}\\]$";
  private static final String NOT_EQUALS_REGEXP_STRING =
      "^-" + EXPECTED_FIELD + ":\\[\\d{13} TO \\d{13}\\] \\+\\*\\:\\*$";
  private static final String LESS_THAN_REGEXP_STRING =
      "^" + EXPECTED_FIELD + ":\\[0 TO \\d{13}\\]$";
  private static final String GREATER_THAN_REGEXP_STRING =
      "^" + EXPECTED_FIELD + ":\\[\\d{13} TO " + Long.MAX_VALUE + "\\]$";

  private static String expectedEqualsQuery(String rawValue) {
    DateTimeType dateTime = new DateTimeType(rawValue);
    long from = DateUtil.getFirstInstantOfDateTime(dateTime);
    long to = DateUtil.getLastInstantOfDateTime(dateTime);
    return EXPECTED_FIELD + ":[" + from + " TO " + to + "]";
  }

  private static String expectedNotEqualsQuery(String rawValue) {
    return "-" + expectedEqualsQuery(rawValue) + " +*:*";
  }

  private static String expectedLessThanQuery(String rawValue) {
    DateTimeType dateTime = new DateTimeType(rawValue);
    long from = DateUtil.getFirstInstantOfDateTime(dateTime);
    return EXPECTED_FIELD + ":[0 TO " + (from - 1) + "]";
  }

  private static String expectedLessThanOrEqualsQuery(String rawValue) {
    DateTimeType dateTime = new DateTimeType(rawValue);
    long to = DateUtil.getLastInstantOfDateTime(dateTime);
    return EXPECTED_FIELD + ":[0 TO " + to + "]";
  }

  private static String expectedGreaterThanQuery(String rawValue) {
    DateTimeType dateTime = new DateTimeType(rawValue);
    long to = DateUtil.getLastInstantOfDateTime(dateTime);
    return EXPECTED_FIELD + ":[" + (to + 1) + " TO " + Long.MAX_VALUE + "]";
  }

  private static String expectedGreaterThanOrEqualsQuery(String rawValue) {
    DateTimeType dateTime = new DateTimeType(rawValue);
    long from = DateUtil.getFirstInstantOfDateTime(dateTime);
    return EXPECTED_FIELD + ":[" + from + " TO " + Long.MAX_VALUE + "]";
  }

  // ================================================================================
  // Equals (eq) Prefix Tests
  // ================================================================================

  @Nested
  @DisplayName("Equals (eq) Prefix Tests")
  class EqualsTests {

    private static Stream<Arguments> equalsPrecisionCases() {
      return Stream.of(
          createEqualsPrecisionCase("year", "2023"),
          createEqualsPrecisionCase("month", "2023-06"),
          createEqualsPrecisionCase("day", "2023-06-15"),
          createEqualsPrecisionCase("minute", "2023-06-15T14:30:00"),
          createEqualsPrecisionCase("second", "2023-06-15T14:30:45"),
          createEqualsPrecisionCase("millisecond", "2023-06-15T14:30:45.123"));
    }

    private static Arguments createEqualsPrecisionCase(String precision, String rawValue) {
      DateTimeType dateTime = new DateTimeType(rawValue);
      long expectedFrom = DateUtil.getFirstInstantOfDateTime(dateTime);
      long expectedTo = DateUtil.getLastInstantOfDateTime(dateTime);
      return Arguments.of(precision, rawValue, expectedFrom, expectedTo);
    }

    @DisplayName("Should create exact timestamp range query for each eq precision")
    @ParameterizedTest(name = "precision={0}, value={1}")
    @MethodSource("equalsPrecisionCases")
    void shouldCreateExactRangeQueryForPrecision(
        String precision, String rawValue, long expectedFrom, long expectedTo)
        throws NotImplementedException {
      // GIVEN
      DateParam param = new DateParam(ParamPrefixEnum.EQUAL, rawValue);

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();
      String expectedQuery = EXPECTED_FIELD + ":[" + expectedFrom + " TO " + expectedTo + "]";

      // THEN
      assertThat(query).as("Query should not be null for precision '%s'", precision).isNotNull();
      assertThat(queryString)
          .as("Query should match general equals pattern")
          .matches(EQUALS_REGEXP_STRING)
          .as(
              "Query should use exact boundaries for precision '%s' and value '%s'",
              precision, rawValue)
          .isEqualTo(expectedQuery);

      if ("millisecond".equals(precision)) {
        assertThat(expectedFrom)
            .as("Millisecond precision must use an exact point query")
            .isEqualTo(expectedTo);
      }
    }

    @Test
    @DisplayName("Should handle implicit equals prefix (no prefix)")
    void shouldHandleImplicitEqualsPrefix() throws NotImplementedException {
      // GIVEN - DateParam without explicit prefix defaults to EQUAL
      String rawValue = "2023-06-15";
      DateParam param = new DateParam("2023-06-15");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", EQUALS_REGEXP_STRING)
          .matches(EQUALS_REGEXP_STRING)
          .as("Implicit equals should use exact day boundaries")
          .isEqualTo(expectedEqualsQuery(rawValue));
    }
  }

  // ================================================================================
  // Not Equals (ne) Prefix Tests
  // ================================================================================

  @Nested
  @DisplayName("Not Equals (ne) Prefix Tests")
  class NotEqualsTests {

    @Test
    @DisplayName("Should create negated range query for not equals (ne2023)")
    void shouldCreateNegatedRangeQueryForNotEquals() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.NOT_EQUAL, "2023");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", NOT_EQUALS_REGEXP_STRING)
          .matches(NOT_EQUALS_REGEXP_STRING)
          .as("Not equals query should negate the exact year range")
          .isEqualTo(expectedNotEqualsQuery(rawValue));
    }

    @Test
    @DisplayName("Should create negated query for day precision (ne2023-06-15)")
    void shouldCreateNegatedQueryForDayPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06-15";
      DateParam param = new DateParam(ParamPrefixEnum.NOT_EQUAL, "2023-06-15");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", NOT_EQUALS_REGEXP_STRING)
          .matches(NOT_EQUALS_REGEXP_STRING)
          .as("Not equals query should negate the exact day range")
          .isEqualTo(expectedNotEqualsQuery(rawValue));
    }
  }

  // ================================================================================
  // Less Than (lt) and Less Than or Equals (le) Tests
  // ================================================================================

  @Nested
  @DisplayName("Less Than Comparison Tests")
  class LessThanTests {

    @Test
    @DisplayName("Should create less than query (lt2023)")
    void shouldCreateLessThanQuery() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.LESSTHAN, "2023");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", LESS_THAN_REGEXP_STRING)
          .matches(LESS_THAN_REGEXP_STRING)
          .as("Less-than should end at first instant of year minus 1ms")
          .isEqualTo(expectedLessThanQuery(rawValue));
    }

    @Test
    @DisplayName("Should create less than or equals query (le2023)")
    void shouldCreateLessThanOrEqualsQuery() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2023");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", LESS_THAN_REGEXP_STRING)
          .matches(LESS_THAN_REGEXP_STRING)
          .as("Less-than-or-equals should end at last instant of year")
          .isEqualTo(expectedLessThanOrEqualsQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle less than with month precision (lt2023-06)")
    void shouldHandleLessThanWithMonthPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06";
      DateParam param = new DateParam(ParamPrefixEnum.LESSTHAN, "2023-06");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", LESS_THAN_REGEXP_STRING)
          .matches(LESS_THAN_REGEXP_STRING)
          .as("Less-than should end at first instant of month minus 1ms")
          .isEqualTo(expectedLessThanQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle less than or equals with day precision (le2023-06-15)")
    void shouldHandleLessThanOrEqualsWithDayPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06-15";
      DateParam param = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2023-06-15");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", LESS_THAN_REGEXP_STRING)
          .matches(LESS_THAN_REGEXP_STRING)
          .as("Less-than-or-equals should end at last instant of day")
          .isEqualTo(expectedLessThanOrEqualsQuery(rawValue));
    }
  }

  // ================================================================================
  // Greater Than (gt) and Greater Than or Equals (ge) Tests
  // ================================================================================

  @Nested
  @DisplayName("Greater Than Comparison Tests")
  class GreaterThanTests {

    @Test
    @DisplayName("Should create greater than query (gt2023)")
    void shouldCreateGreaterThanQuery() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.GREATERTHAN, "2023");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", GREATER_THAN_REGEXP_STRING)
          .matches(GREATER_THAN_REGEXP_STRING)
          .as("Greater-than should start at last instant of year plus 1ms")
          .isEqualTo(expectedGreaterThanQuery(rawValue));
    }

    @Test
    @DisplayName("Should create greater than or equals query (ge2023)")
    void shouldCreateGreaterThanOrEqualsQuery() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023";
      DateParam param = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2023");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", GREATER_THAN_REGEXP_STRING)
          .matches(GREATER_THAN_REGEXP_STRING)
          .as("Greater-than-or-equals should start at first instant of year")
          .isEqualTo(expectedGreaterThanOrEqualsQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle greater than with month precision (gt2023-06)")
    void shouldHandleGreaterThanWithMonthPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06";
      DateParam param = new DateParam(ParamPrefixEnum.GREATERTHAN, "2023-06");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", GREATER_THAN_REGEXP_STRING)
          .matches(GREATER_THAN_REGEXP_STRING)
          .as("Greater-than should start at last instant of month plus 1ms")
          .isEqualTo(expectedGreaterThanQuery(rawValue));
    }

    @Test
    @DisplayName("Should handle greater than or equals with day precision (ge2023-06-15)")
    void shouldHandleGreaterThanOrEqualsWithDayPrecision() throws NotImplementedException {
      // GIVEN
      String rawValue = "2023-06-15";
      DateParam param = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2023-06-15");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);
      String queryString = query.toString();

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(queryString)
          .as("Query should match %s", GREATER_THAN_REGEXP_STRING)
          .matches(GREATER_THAN_REGEXP_STRING)
          .as("Greater-than-or-equals should start at first instant of day")
          .isEqualTo(expectedGreaterThanOrEqualsQuery(rawValue));
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
      assertThatThrownBy(() -> new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param))
          .as("Should throw NotImplementedException for STARTS_AFTER")
          .isInstanceOf(NotImplementedException.class)
          .hasMessageMatching("(?i).*(sa|starts_after|not implemented).*");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for ends before (eb) prefix")
    void shouldThrowExceptionForEndsBeforePrefix() {
      // GIVEN
      DateParam param = new DateParam(ParamPrefixEnum.ENDS_BEFORE, "2023");

      // THEN
      assertThatThrownBy(() -> new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param))
          .as("Should throw NotImplementedException for ENDS_BEFORE")
          .isInstanceOf(NotImplementedException.class)
          .hasMessageMatching("(?i).*(eb|ends_before|not implemented).*");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for approximate (ap) prefix")
    void shouldThrowExceptionForApproximatePrefix() {
      // GIVEN
      DateParam param = new DateParam(ParamPrefixEnum.APPROXIMATE, "2023");

      // THEN
      assertThatThrownBy(() -> new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param))
          .as("Should throw NotImplementedException for APPROXIMATE")
          .isInstanceOf(NotImplementedException.class)
          .hasMessageMatching("(?i).*(ap|approximate|not implemented).*");
    }
  }

  // ================================================================================
  // Field Name Construction Tests
  // ================================================================================

  @Nested
  @DisplayName("Field Name Construction Tests")
  class FieldNameTests {

    @Test
    @DisplayName("Should construct correct field name with _date_instant suffix")
    void shouldConstructCorrectFieldName() throws NotImplementedException {
      // GIVEN
      DateParam param = new DateParam("2023");

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(PARAM_NAME, param);

      // THEN
      String queryString = query.toString();
      assertThat(queryString)
          .as("Query should contain field with '_date_instant' suffix")
          .contains(PARAM_NAME + SEPARATOR + SUFFIX_DATE_INSTANT);
    }

    @Test
    @DisplayName("Should use different parameter names correctly")
    void shouldUseDifferentParameterNames() throws NotImplementedException {
      // GIVEN
      DateParam param = new DateParam("2023");
      String customParamName = "_lastUpdated";
      String expectedCustomField = customParamName + SEPARATOR + SUFFIX_DATE_INSTANT;

      // WHEN
      Query query = new InstantParameterQueryGenerator().prepareQuery(customParamName, param);

      // THEN
      String queryString = query.toString();
      assertThat(queryString)
          .as("Query should contain custom field name")
          .contains(expectedCustomField);
    }
  }

  // ================================================================================
  // Direct Method Tests (prepareLongRangeQuery)
  // ================================================================================

  @Nested
  @DisplayName("Direct Method Tests")
  class DirectMethodTests {

    @Test
    @DisplayName("Should create LongPoint range query with prepareLongRangeQuery")
    void shouldCreateLongPointRangeQuery() {
      // GIVEN
      long from = 1000000L;
      long to = 2000000L;

      // WHEN
      Query query =
          new InstantParameterQueryGenerator().prepareLongRangeQuery(PARAM_NAME, from, to);

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(query.toString())
          .as("Query should be correct")
          .contains(EXPECTED_FIELD + ":[" + from + " TO " + to + "]");
    }

    @Test
    @DisplayName("Should handle same from and to values")
    void shouldHandleSameFromAndToValues() {
      // GIVEN
      long timestamp = 1234567890L;

      // WHEN
      Query query =
          new InstantParameterQueryGenerator()
              .prepareLongRangeQuery(PARAM_NAME, timestamp, timestamp);

      // THEN
      assertThat(query).as("Query should not be null").isNotNull();
      assertThat(query.toString())
          .as("Query should be correct")
          .contains(EXPECTED_FIELD + ":[" + timestamp + " TO " + timestamp + "]");
    }
  }
}

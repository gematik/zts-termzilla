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
package de.gematik.zts.termzilla.common.metadatasearch.integration;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.integration.helper.InMemoryLuceneTestHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.PeriodParameterQueryGenerator;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Lucene DateTime date parameter-based indexing and searching.
 *
 * <p>These tests verify the complete cycle of DateTime date-based searches in the Lucene index:
 *
 * <ol>
 *   <li>Indexing FHIR DateTime date parameters using {@link ResourceFieldGeneratorHelper}
 *   <li>Generating Lucene queries using {@link PeriodParameterQueryGenerator}
 *   <li>Searching the index and retrieving matching documents
 * </ol>
 *
 * <p>Unlike unit tests, these integration tests use real Lucene components (in-memory via {@link
 * InMemoryLuceneTestHelper}) and production code to ensure that indexing and querying work
 * correctly together.
 *
 * <p><b>Key Characteristic:</b> DateTimeType supports variable precision (year, month, day, time)
 * and is internally represented as a period with start and end timestamps. This enables period
 * overlap logic for searching.
 *
 * <p><b>Period Overlap Logic:</b> A DateTime [start, end] overlaps with search range [from, to] if:
 *
 * <ul>
 *   <li>Condition 1: DateTime start falls within search range: start IN [from, to]
 *   <li>Condition 2: DateTime end falls within search range: end IN [from, to]
 *   <li>Condition 3: DateTime encompasses search range: (start <= to) AND (end >= from)
 * </ul>
 *
 * <p><b>Test Structure:</b>
 *
 * <ul>
 *   <li><b>Standard DateTime Search</b> - Tests for basic date search functionality including
 *       overlap matches and non-matches
 *   <li><b>DateTime Overlap Tests</b> - Tests for different overlap scenarios with variable
 *       precision
 *   <li><b>DateTime Comparison Tests</b> - Tests for comparison operators (lt, le, gt, ge) applied
 *       to DateTimes
 *   <li><b>DateTime Precision Tests</b> - Tests for different date precision levels (year, month,
 *       day, time)
 *   <li><b>Edge Cases</b> - Tests for boundary conditions and special scenarios
 * </ul>
 *
 * <p><b>Current Test Coverage:</b>
 *
 * <ul>
 *   <li>Exact DateTime overlap search ({@code date=2023-06-15})
 *   <li>DateTime overlap scenarios with different precisions
 *   <li>Less than DateTime search ({@code date=lt2023-06-15})
 *   <li>Less than or equals search ({@code date=le2023-06-15})
 *   <li>Greater than DateTime search ({@code date=gt2023-06-15})
 *   <li>Greater than or equals search ({@code date=ge2023-06-15})
 *   <li>Not equals DateTime search ({@code date=ne2023-06-15})
 *   <li>Date precision handling (year, month, day levels)
 *   <li>Multiple DateTimes with different precisions
 *   <li>Boundary DateTime handling (adjacent, overlapping precisions)
 *   <li>Test multiple DateTime parameters with OR combination (comma-separated values)
 *   <li>Test multiple DateTime parameters with AND combination (multiple parameters)
 *   <li>Test timezone-aware DateTime searches
 *   <li>Test millisecond precision searches
 *   <li>Test :missing modifier for DateTime parameters
 *   <li>Test edge cases: very old dates, future dates, date overflow scenarios
 * </ul>
 *
 * @see PeriodParameterQueryGenerator
 * @see ResourceFieldGeneratorHelper
 * @see InMemoryLuceneTestHelper
 */
@DisplayName("Lucene Index-Search Integration Tests for DateTime Date Parameters")
class LuceneIndexSearchByDateTimeDateIntegrationTest {

  // ================================================================================
  // Helper Methods
  // ================================================================================

  /**
   * Prepares and indexes a document with the specified DateTime.
   *
   * @param helper the in-memory Lucene test helper
   * @param dateTimeString the DateTime string (e.g., "2023", "2023-06", "2023-06-15")
   * @throws IOException if indexing fails
   */
  private void prepareDocument(InMemoryLuceneTestHelper helper, String dateTimeString)
      throws IOException {
    Document doc = new Document();
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(doc, NAME_DATE, false);
    ResourceFieldGeneratorHelper.addDateTimeDateParamFieldToDocument(
        doc, NAME_DATE, new DateTimeType(dateTimeString));
    helper.indexDocument(doc);
  }

  /**
   * Prepares and indexes a document where the DateTime parameter is missing.
   *
   * @param helper the in-memory Lucene test helper
   * @throws IOException if indexing fails
   */
  private void prepareDocumentWithMissingDateTime(InMemoryLuceneTestHelper helper)
      throws IOException {
    Document doc = new Document();
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(doc, NAME_DATE, true);
    helper.indexDocument(doc);
  }

  /**
   * Creates a search query for an exact date match (DateTime overlap).
   *
   * @param date the date to search for (e.g., "2023-06-15")
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createExactDateQuery(String date) throws NotImplementedException {
    DateAndListParam dateParam =
        new DateAndListParam().addAnd(new DateOrListParam().addOr(new DateParam(date)));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_DATE)
            .paramQueryType(ParamData.PERIOD_DATE_PARAM)
            .param(dateParam)
            .build();

    return new PeriodParameterQueryGenerator().prepareQuery(paramData);
  }

  /**
   * Creates a search query with a date comparison prefix.
   *
   * @param prefix the comparison prefix (e.g., LESSTHAN, GREATERTHAN)
   * @param date the date to compare against
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createDateComparisonQuery(ParamPrefixEnum prefix, String date)
      throws NotImplementedException {
    DateParam dateParam = new DateParam(prefix, date);
    DateAndListParam dateAndListParam =
        new DateAndListParam().addAnd(new DateOrListParam().addOr(dateParam));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_DATE)
            .paramQueryType(ParamData.PERIOD_DATE_PARAM)
            .param(dateAndListParam)
            .build();

    return new PeriodParameterQueryGenerator().prepareQuery(paramData);
  }

  /**
   * Creates a search query with an empty date parameter.
   *
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createEmptyDateQuery() throws NotImplementedException {
    DateAndListParam dateParam =
        new DateAndListParam().addAnd(new DateOrListParam().addOr(new DateParam()));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_DATE)
            .paramQueryType(ParamData.PERIOD_DATE_PARAM)
            .param(dateParam)
            .build();

    return new PeriodParameterQueryGenerator().prepareQuery(paramData);
  }

  /**
   * Creates a search query with OR logic for multiple DateTime values.
   *
   * @param dates the DateTime values to combine with OR
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createOrDateQuery(String... dates) throws NotImplementedException {
    DateOrListParam orListParam = new DateOrListParam();
    for (String date : dates) {
      orListParam.addOr(new DateParam(date));
    }

    DateAndListParam dateAndListParam = new DateAndListParam().addAnd(orListParam);

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_DATE)
            .paramQueryType(ParamData.PERIOD_DATE_PARAM)
            .param(dateAndListParam)
            .build();

    return new PeriodParameterQueryGenerator().prepareQuery(paramData);
  }

  /**
   * Creates a search query with AND logic for multiple DateTime parameters.
   *
   * @param params the DateTime parameters to combine with AND
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createAndDateQuery(DateParam... params) throws NotImplementedException {
    DateAndListParam dateAndListParam = new DateAndListParam();
    for (DateParam param : params) {
      dateAndListParam.addAnd(new DateOrListParam().addOr(param));
    }

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_DATE)
            .paramQueryType(ParamData.PERIOD_DATE_PARAM)
            .param(dateAndListParam)
            .build();

    return new PeriodParameterQueryGenerator().prepareQuery(paramData);
  }

  /**
   * Creates a search query with the :missing modifier for DateTime parameters.
   *
   * @param missing true to search for missing values, false for present values
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createMissingDateQuery(boolean missing) throws NotImplementedException {
    DateParam dateParam = new DateParam();
    dateParam.setMissing(missing);
    DateAndListParam dateAndListParam =
        new DateAndListParam().addAnd(new DateOrListParam().addOr(dateParam));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_DATE)
            .paramQueryType(ParamData.PERIOD_DATE_PARAM)
            .param(dateAndListParam)
            .build();

    return new PeriodParameterQueryGenerator().prepareQuery(paramData);
  }

  // ================================================================================
  // Standard DateTime Search Tests
  // ================================================================================

  @Nested
  @DisplayName("Standard DateTime Search")
  class StandardDateTimeSearchTests {

    @Test
    @DisplayName("Should find Document by DateTime overlap with exact date")
    void shouldFindDocumentByDateTimeOverlap() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents indexed with different DateTimes
        // DateTime 1: June 2023 (entire month - should match search for June 15)
        prepareDocument(helper, "2023-06");
        // DateTime 2: July 2023 (should NOT match search for June 15)
        prepareDocument(helper, "2023-07");
        // DateTime 3: May 2023 (should NOT match search for June 15)
        prepareDocument(helper, "2023-05");

        // WHEN: Search for date=2023-06-15 (looking for DateTimes overlapping this day)
        Query query = createExactDateQuery("2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document (DateTime covering June 2023)
        assertEquals(
            1, results.size(), "Should find exactly one Document with overlapping DateTime");
      }
    }

    @Test
    @DisplayName("Should not find Document when DateTime does not overlap")
    void shouldNotFindDocumentWhenDateTimeDoesNotOverlap()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents with DateTimes not overlapping with search date
        prepareDocument(helper, "2023-05"); // May 2023
        prepareDocument(helper, "2023-07"); // July 2023

        // WHEN: Search for date in between (June 15)
        Query query = createExactDateQuery("2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find no documents
        assertEquals(
            0, results.size(), "Should not find any Document with non-overlapping DateTime");
      }
    }

    @Test
    @DisplayName("Should find multiple Documents with overlapping DateTimes")
    void shouldFindMultipleDocumentsWithOverlappingDateTimes()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with DateTimes all overlapping June 15
        prepareDocument(helper, "2023-06"); // Entire June
        prepareDocument(helper, "2023"); // Entire 2023
        prepareDocument(helper, "2023-06-15"); // Exact day

        // WHEN: Search for date=2023-06-15
        Query query = createExactDateQuery("2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find all three documents
        assertEquals(3, results.size(), "Should find all Documents with overlapping DateTimes");
      }
    }
  }

  // ================================================================================
  // DateTime Overlap Scenario Tests
  // ================================================================================

  @Nested
  @DisplayName("DateTime Overlap Scenarios")
  class DateTimeOverlapScenarioTests {

    @Test
    @DisplayName("Should find DateTime when search date is contained within DateTime period")
    void shouldFindDateTimeContainingSearchDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: DateTime with year precision (entire year 2023)
        prepareDocument(helper, "2023");

        // WHEN: Search for date in the middle of the year
        Query query = createExactDateQuery("2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find the DateTime (Condition 3: DateTime encompasses search range)
        assertEquals(1, results.size(), "Should find DateTime containing search date");
      }
    }

    @Test
    @DisplayName("Should find DateTime when search range encompasses DateTime")
    void shouldFindDateTimeWhenSearchRangeEncompasses()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: DateTime with day precision (single day)
        prepareDocument(helper, "2023-06-15");

        // WHEN: Search for entire month (encompasses the single day)
        Query query = createExactDateQuery("2023-06");
        List<Document> results = helper.search(query);

        // THEN: Should find the DateTime (Condition 1 or 2: day falls within month range)
        assertEquals(1, results.size(), "Should find DateTime within search range");
      }
    }
  }

  // ================================================================================
  // DateTime Comparison Tests (lt, le, gt, ge)
  // ================================================================================

  @Nested
  @DisplayName("DateTime Comparison Tests")
  class DateTimeComparisonTests {

    @Test
    @DisplayName("Should find DateTimes with less than (lt) date search")
    void shouldFindDateTimesWithLessThanDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different DateTimes
        prepareDocument(helper, "2023-06-01");
        prepareDocument(helper, "2023-06-15");
        prepareDocument(helper, "2023-06-30");

        // WHEN: Search for date=lt2023-06-15
        Query query = createDateComparisonQuery(ParamPrefixEnum.LESSTHAN, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find DateTimes overlapping with dates before 2023-06-15
        assertEquals(1, results.size(), "Should find DateTimes before target date");
      }
    }

    @Test
    @DisplayName("Should find DateTimes with less than or equals (le) date search")
    void shouldFindDateTimesWithLessThanOrEqualsDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different DateTimes
        prepareDocument(helper, "2023-06-01");
        prepareDocument(helper, "2023-06-15");
        prepareDocument(helper, "2023-06-30");

        // WHEN: Search for date=le2023-06-15
        Query query = createDateComparisonQuery(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find DateTimes on or before 2023-06-15
        assertEquals(2, results.size(), "Should find DateTimes on or before target date");
      }
    }

    @Test
    @DisplayName("Should find DateTimes with greater than (gt) date search")
    void shouldFindDateTimesWithGreaterThanDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different DateTimes
        prepareDocument(helper, "2023-06-01");
        prepareDocument(helper, "2023-06-15");
        prepareDocument(helper, "2023-06-30");

        // WHEN: Search for date=gt2023-06-15
        Query query = createDateComparisonQuery(ParamPrefixEnum.GREATERTHAN, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find DateTimes after 2023-06-15
        assertEquals(1, results.size(), "Should find DateTimes after target date");
      }
    }

    @Test
    @DisplayName("Should find DateTimes with greater than or equals (ge) date search")
    void shouldFindDateTimesWithGreaterThanOrEqualsDate()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different DateTimes
        prepareDocument(helper, "2023-06-01");
        prepareDocument(helper, "2023-06-15");
        prepareDocument(helper, "2023-06-30");

        // WHEN: Search for date=ge2023-06-15
        Query query =
            createDateComparisonQuery(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find DateTimes on or after 2023-06-15
        assertEquals(2, results.size(), "Should find DateTimes on or after target date");
      }
    }

    @Test
    @DisplayName("Should find DateTimes with not equals (ne) date search")
    void shouldFindDateTimesWithNotEqualsDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different DateTimes
        // DateTime covering June 15 (should be excluded)
        prepareDocument(helper, "2023-06-15");
        // DateTime NOT covering June 15 (should be included)
        prepareDocument(helper, "2023-06-01");
        // DateTime NOT covering June 15 (should be included)
        prepareDocument(helper, "2023-06-30");

        // WHEN: Search for date=ne2023-06-15
        Query query = createDateComparisonQuery(ParamPrefixEnum.NOT_EQUAL, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find DateTimes NOT overlapping with 2023-06-15
        assertEquals(2, results.size(), "Should find DateTimes not overlapping with target date");
      }
    }
  }

  // ================================================================================
  // DateTime Precision Tests
  // ================================================================================

  @Nested
  @DisplayName("DateTime Precision Tests")
  class DateTimePrecisionTests {

    @Test
    @DisplayName("Should find DateTimes matching year precision search")
    void shouldFindDateTimesWithYearPrecision() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: DateTimes from different years
        prepareDocument(helper, "2023-01");
        prepareDocument(helper, "2023-06");
        prepareDocument(helper, "2023-12");
        prepareDocument(helper, "2024-01");

        // WHEN: Search for date=2023 (year precision)
        Query query = createExactDateQuery("2023");
        List<Document> results = helper.search(query);

        // THEN: Should find all three DateTimes from 2023
        assertEquals(3, results.size(), "Should find all DateTimes overlapping with year 2023");
      }
    }

    @Test
    @DisplayName("Should find DateTimes matching month precision search")
    void shouldFindDateTimesWithMonthPrecision() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: DateTimes from different months
        prepareDocument(helper, "2023-06-01");
        prepareDocument(helper, "2023-06-15");
        prepareDocument(helper, "2023-06-30");
        prepareDocument(helper, "2023-07-01");

        // WHEN: Search for date=2023-06 (month precision)
        Query query = createExactDateQuery("2023-06");
        List<Document> results = helper.search(query);

        // THEN: Should find all three DateTimes from June 2023
        assertEquals(3, results.size(), "Should find all DateTimes overlapping with June 2023");
      }
    }

    @Test
    @DisplayName("Should handle different precision levels correctly")
    void shouldHandleDifferentPrecisionLevels() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: DateTimes with different precision levels
        prepareDocument(helper, "2023"); // Year precision
        prepareDocument(helper, "2023-06"); // Month precision
        prepareDocument(helper, "2023-06-15"); // Day precision

        // WHEN: Search for specific day
        Query query = createExactDateQuery("2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find all DateTimes that overlap with June 15, 2023
        assertEquals(3, results.size(), "Should find DateTimes at all precision levels");
      }
    }

    @Test
    @DisplayName("Should distinguish between adjacent months")
    void shouldDistinguishBetweenAdjacentMonths() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: DateTimes from adjacent months
        prepareDocument(helper, "2023-05");
        prepareDocument(helper, "2023-06");
        prepareDocument(helper, "2023-07");

        // WHEN: Search for June
        Query query = createExactDateQuery("2023-06");
        List<Document> results = helper.search(query);

        // THEN: Should find only June DateTime
        assertEquals(1, results.size(), "Should find only DateTime for June 2023");
      }
    }

    @Test
    @DisplayName("Should find Document by exact millisecond DateTime search")
    void shouldFindDocumentByExactMillisecondPrecision() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two DateTimes differing by 1 millisecond
        prepareDocument(helper, "2023-06-15T10:30:45.123Z");
        prepareDocument(helper, "2023-06-15T10:30:45.124Z");

        // WHEN: Search for exact millisecond timestamp
        Query query = createExactDateQuery("2023-06-15T10:30:45.123Z");
        List<Document> results = helper.search(query);

        // THEN: Should find only the exact millisecond match
        assertEquals(1, results.size(), "Should find exactly one DateTime with millisecond value");
      }
    }
  }

  // ================================================================================
  // DateTime Parameter Combination Tests
  // ================================================================================

  @Nested
  @DisplayName("DateTime Parameter Combination Tests")
  class DateTimeParameterCombinationTests {

    @Test
    @DisplayName("Should find DateTimes with OR combination of multiple DateTime values")
    void shouldFindDateTimesWithOrCombination() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents on different days
        prepareDocument(helper, "2023-06-15");
        prepareDocument(helper, "2023-06-16");
        prepareDocument(helper, "2023-06-17");

        // WHEN: Search using OR logic (equivalent to comma-separated values)
        Query query = createOrDateQuery("2023-06-15", "2023-06-17");
        List<Document> results = helper.search(query);

        // THEN: Should find documents matching either DateTime
        assertEquals(2, results.size(), "Should find DateTimes for both OR values");
      }
    }

    @Test
    @DisplayName("Should find DateTimes with AND combination of multiple DateTime parameters")
    void shouldFindDateTimesWithAndCombination() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Four documents across a date range
        prepareDocument(helper, "2023-06-14");
        prepareDocument(helper, "2023-06-15");
        prepareDocument(helper, "2023-06-16");
        prepareDocument(helper, "2023-06-17");

        // WHEN: Search using AND logic via multiple DateTime parameters
        Query query =
            createAndDateQuery(
                new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2023-06-15"),
                new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2023-06-16"));
        List<Document> results = helper.search(query);

        // THEN: Should find documents satisfying both constraints
        assertEquals(2, results.size(), "Should find DateTimes in the AND-constrained range");
      }
    }
  }

  // ================================================================================
  // Timezone-Aware DateTime Search Tests
  // ================================================================================

  @Nested
  @DisplayName("Timezone-Aware DateTime Search Tests")
  class TimezoneAwareDateTimeSearchTests {

    @Test
    @DisplayName("Should match equivalent DateTimes across different timezone offsets")
    void shouldMatchEquivalentDateTimesAcrossTimezones()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: One DateTime with +02:00 offset and one with a different instant
        prepareDocument(helper, "2023-06-15T12:30:00.000+02:00");
        prepareDocument(helper, "2023-06-15T12:31:00.000+02:00");

        // WHEN: Search for the same instant in UTC
        Query query = createExactDateQuery("2023-06-15T10:30:00.000Z");
        List<Document> results = helper.search(query);

        // THEN: Should find only the equivalent instant
        assertEquals(1, results.size(), "Should match same DateTime instant independent of offset");
      }
    }
  }

  // ================================================================================
  // Edge Cases
  // ================================================================================

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle empty date parameter")
    void shouldHandleEmptyDateParameter() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents with DateTimes
        prepareDocument(helper, "2023-06-15");
        prepareDocument(helper, "2023-06-16");

        // WHEN: Search with empty date parameter
        Query query = createEmptyDateQuery();
        List<Document> results = helper.search(query);

        // THEN: Empty parameter should match all documents
        assertEquals(2, results.size(), "Empty date parameter should match all Documents");
      }
    }

    @Test
    @DisplayName("Should handle year boundaries correctly")
    void shouldHandleYearBoundariesCorrectly() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: DateTimes at year boundaries
        prepareDocument(helper, "2022-12");
        prepareDocument(helper, "2023-01");
        prepareDocument(helper, "2023-12");
        prepareDocument(helper, "2024-01");

        // WHEN: Search for year 2023
        Query query = createExactDateQuery("2023");
        List<Document> results = helper.search(query);

        // THEN: Should find only DateTimes within 2023
        assertEquals(2, results.size(), "Should find DateTimes within year 2023");
      }
    }

    @Test
    @DisplayName("Should handle month boundaries correctly")
    void shouldHandleMonthBoundariesCorrectly() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: DateTimes at month boundaries
        prepareDocument(helper, "2023-05-31");
        prepareDocument(helper, "2023-06-01");
        prepareDocument(helper, "2023-06-30");
        prepareDocument(helper, "2023-07-01");

        // WHEN: Search for June 2023
        Query query = createExactDateQuery("2023-06");
        List<Document> results = helper.search(query);

        // THEN: Should find DateTimes within June
        assertEquals(2, results.size(), "Should find DateTimes within June 2023");
      }
    }

    @Test
    @DisplayName("Should handle :missing=true modifier for DateTime parameters")
    void shouldHandleMissingTrueModifier() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: One document with date and one with missing date
        prepareDocument(helper, "2023-06-15");
        prepareDocumentWithMissingDateTime(helper);

        // WHEN: Search for missing DateTime values
        Query query = createMissingDateQuery(true);
        List<Document> results = helper.search(query);

        // THEN: Should find only the document without DateTime
        assertEquals(1, results.size(), "Should find exactly one Document with missing DateTime");
      }
    }

    @Test
    @DisplayName("Should handle :missing=false modifier for DateTime parameters")
    void shouldHandleMissingFalseModifier() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: One document with date and one with missing date
        prepareDocument(helper, "2023-06-15");
        prepareDocumentWithMissingDateTime(helper);

        // WHEN: Search for present DateTime values
        Query query = createMissingDateQuery(false);
        List<Document> results = helper.search(query);

        // THEN: Should find only the document with DateTime
        assertEquals(1, results.size(), "Should find exactly one Document with present DateTime");
      }
    }

    @Test
    @DisplayName("Should handle very old DateTimes")
    void shouldHandleVeryOldDateTimes() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: One old DateTime and one modern DateTime
        prepareDocument(helper, "1900-01-01T00:00:00.000Z");
        prepareDocument(helper, "2023-06-15T10:00:00.000Z");

        // WHEN: Search for very old DateTime
        Query query = createExactDateQuery("1900-01-01");
        List<Document> results = helper.search(query);

        // THEN: Should find the old DateTime document
        assertEquals(1, results.size(), "Should find Document with very old DateTime");
      }
    }

    @Test
    @DisplayName("Should handle far future DateTimes")
    void shouldHandleFarFutureDateTimes() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: One current-ish DateTime and one far future DateTime
        prepareDocument(helper, "2023-06-15T10:00:00.000Z");
        prepareDocument(helper, "2099-12-31T23:59:59.999Z");

        // WHEN: Search for future DateTimes
        Query query = createDateComparisonQuery(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2099");
        List<Document> results = helper.search(query);

        // THEN: Should find future documents
        assertEquals(1, results.size(), "Should find Document with far future DateTime");
      }
    }

    @Test
    @DisplayName("Should handle epoch boundary without range overflow")
    void shouldHandleEpochBoundaryWithoutRangeOverflow() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: One DateTime before and one DateTime at unix epoch
        prepareDocument(helper, "1969-12-31T23:59:59.999Z");
        prepareDocument(helper, "1970-01-01T00:00:00.000Z");

        // WHEN: Search for DateTimes on or after exact epoch instant
        Query query =
            createDateComparisonQuery(
                ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "1970-01-01T00:00:00.000Z");
        List<Document> results = helper.search(query);

        // THEN: Should include only epoch and later documents
        assertEquals(1, results.size(), "Should correctly handle epoch boundary comparisons");
      }
    }
  }
}

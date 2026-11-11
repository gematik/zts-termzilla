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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_LASTUPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.integration.helper.InMemoryLuceneTestHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.InstantParameterQueryGenerator;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.InstantType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Lucene instant date parameter-based indexing and searching.
 *
 * <p>These tests verify the complete cycle of instant date-based searches in the Lucene index:
 *
 * <ol>
 *   <li>Indexing FHIR instant date parameters using {@link ResourceFieldGeneratorHelper}
 *   <li>Generating Lucene queries using {@link InstantParameterQueryGenerator}
 *   <li>Searching the index and retrieving matching documents
 * </ol>
 *
 * <p>Unlike unit tests, these integration tests use real Lucene components (in-memory via {@link
 * InMemoryLuceneTestHelper}) and production code to ensure that indexing and querying work
 * correctly together.
 *
 * <p><b>Test Structure:</b>
 *
 * <ul>
 *   <li><b>Standard Date Search</b> - Tests for basic date search functionality including exact
 *       matches, range queries, and non-matches
 *   <li><b>Date Comparison Tests</b> - Tests for comparison operators (lt, le, gt, ge)
 *   <li><b>Date Precision Tests</b> - Tests for different date precision levels (year, month, day,
 *       time)
 *   <li><b>Edge Cases</b> - Tests for boundary conditions and special scenarios
 * </ul>
 *
 * <p><b>Current Test Coverage:</b>
 *
 * <ul>
 *   <li>Exact date match search ({@code _lastUpdated=2023-06-15T10:30:00.000Z})
 *   <li>Less than date search ({@code _lastUpdated=lt2023-06-15})
 *   <li>Less than or equals search ({@code _lastUpdated=le2023-06-15})
 *   <li>Greater than date search ({@code _lastUpdated=gt2023-06-15})
 *   <li>Greater than or equals search ({@code _lastUpdated=ge2023-06-15})
 *   <li>Not equals date search ({@code _lastUpdated=ne2023-06-15})
 *   <li>Date precision handling (year, month, day levels)
 *   <li>Multiple documents with different dates
 *   <li>Boundary date handling (first/last instant of periods)
 * </ul>
 *
 * <p><b>TODO - Additional Test Cases (to be implemented):</b>
 *
 * <ul>
 *   <li>Test multiple date parameters with OR combination (comma-separated values)
 *   <li>Test multiple date parameters with AND combination (multiple parameters)
 *   <li>Test timezone-aware date searches
 *   <li>Test millisecond precision searches
 *   <li>Test :missing modifier for date parameters
 *   <li>Test edge cases: very old dates, future dates, date overflow scenarios
 * </ul>
 *
 * @see InstantParameterQueryGenerator
 * @see ResourceFieldGeneratorHelper
 * @see InMemoryLuceneTestHelper
 */
@DisplayName("Lucene Index-Search Integration Tests for Instant Date Parameters")
class LuceneIndexSearchByInstantDateIntegrationTest {

  // ================================================================================
  // Helper Methods
  // ================================================================================

  /**
   * Prepares and indexes a document with the specified instant date.
   *
   * @param helper the in-memory Lucene test helper
   * @param instantString the instant date string
   * @throws IOException if indexing fails
   */
  private void prepareDocument(InMemoryLuceneTestHelper helper, String instantString)
      throws IOException {
    Document doc = new Document();
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(doc, NAME_LASTUPDATED, false);
    ResourceFieldGeneratorHelper.addInstantDateParamFieldToDocument(
        doc, NAME_LASTUPDATED, new InstantType(instantString));
    helper.indexDocument(doc);
  }

  /**
   * Converts a local date-time string to a zoned date-time string with system timezone.
   *
   * @param input the local date-time string (e.g., "2023-06-15T10:30:00.000")
   * @return the zoned date-time string with timezone info
   */
  private String withZoneInfo(String input) {
    LocalDateTime ldt = LocalDateTime.parse(input);
    ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    return zdt.format(formatter);
  }

  /**
   * Creates a search query for an exact date match.
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
            .paramName(NAME_LASTUPDATED)
            .paramQueryType(ParamData.INSTANT_DATE_PARAM)
            .param(dateParam)
            .build();

    return new InstantParameterQueryGenerator().prepareQuery(paramData);
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
            .paramName(NAME_LASTUPDATED)
            .paramQueryType(ParamData.INSTANT_DATE_PARAM)
            .param(dateAndListParam)
            .build();

    return new InstantParameterQueryGenerator().prepareQuery(paramData);
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
            .paramName(NAME_LASTUPDATED)
            .paramQueryType(ParamData.INSTANT_DATE_PARAM)
            .param(dateParam)
            .build();

    return new InstantParameterQueryGenerator().prepareQuery(paramData);
  }

  // ================================================================================
  // Standard Date Search Tests
  // ================================================================================

  @Nested
  @DisplayName("Standard Date Search")
  class StandardDateSearchTests {

    @Test
    @DisplayName("Should find Document by exact instant date")
    void shouldFindDocumentByExactInstantDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents indexed with different lastUpdated dates
        prepareDocument(helper, withZoneInfo("2023-06-15T10:30:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-16T10:30:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-17T10:30:00.000"));

        // WHEN: Search for _lastUpdated=2023-06-15
        Query query = createExactDateQuery("2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document (the one from 2023-06-15)
        assertEquals(1, results.size(), "Should find exactly one Document");
      }
    }

    @Test
    @DisplayName("Should not find Document with non-matching date")
    void shouldNotFindDocumentWithNonMatchingDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with different lastUpdated dates
        prepareDocument(helper, withZoneInfo("2023-06-15T10:30:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-16T10:30:00.000"));

        // WHEN: Search for non-existing date
        Query query = createExactDateQuery("2023-06-20");
        List<Document> results = helper.search(query);

        // THEN: Should find no documents
        assertEquals(0, results.size(), "Should not find any Document");
      }
    }

    @Test
    @DisplayName("Should distinguish between dates on different days")
    void shouldDistinguishBetweenDatesOnDifferentDays()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Documents with dates on consecutive days
        prepareDocument(helper, withZoneInfo("2023-06-15T23:59:59.999"));
        prepareDocument(helper, withZoneInfo("2023-06-16T00:00:00.000"));

        // WHEN: Search for first day
        Query query = createExactDateQuery("2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find only the first document
        assertEquals(1, results.size(), "Should find exactly one Document");
      }
    }
  }

  // ================================================================================
  // Date Comparison Tests (lt, le, gt, ge)
  // ================================================================================

  @Nested
  @DisplayName("Date Comparison Tests")
  class DateComparisonTests {

    @Test
    @DisplayName("Should find Documents with less than (lt) date search")
    void shouldFindDocumentsWithLessThanDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different dates
        prepareDocument(helper, withZoneInfo("2023-06-10T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-20T10:00:00.000"));

        // WHEN: Search for _lastUpdated=lt2023-06-15
        Query query = createDateComparisonQuery(ParamPrefixEnum.LESSTHAN, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find only doc1 (before 2023-06-15)
        assertEquals(1, results.size(), "Should find exactly one Document before 2023-06-15");
      }
    }

    @Test
    @DisplayName("Should find Documents with less than or equals (le) date search")
    void shouldFindDocumentsWithLessThanOrEqualsDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different dates
        prepareDocument(helper, withZoneInfo("2023-06-10T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-20T10:00:00.000"));

        // WHEN: Search for _lastUpdated=le2023-06-15
        Query query = createDateComparisonQuery(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find doc1 and doc2 (before or on 2023-06-15)
        assertEquals(2, results.size(), "Should find two Documents before or on 2023-06-15");
      }
    }

    @Test
    @DisplayName("Should find Documents with greater than (gt) date search")
    void shouldFindDocumentsWithGreaterThanDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different dates
        prepareDocument(helper, withZoneInfo("2023-06-10T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-20T10:00:00.000"));

        // WHEN: Search for _lastUpdated=gt2023-06-15
        Query query = createDateComparisonQuery(ParamPrefixEnum.GREATERTHAN, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find only doc3 (after 2023-06-15)
        assertEquals(1, results.size(), "Should find exactly one Document after 2023-06-15");
      }
    }

    @Test
    @DisplayName("Should find Documents with greater than or equals (ge) date search")
    void shouldFindDocumentsWithGreaterThanOrEqualsDate()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different dates
        prepareDocument(helper, withZoneInfo("2023-06-10T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-20T10:00:00.000"));

        // WHEN: Search for _lastUpdated=ge2023-06-15
        Query query =
            createDateComparisonQuery(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find doc2 and doc3 (on or after 2023-06-15)
        assertEquals(2, results.size(), "Should find two Documents on or after 2023-06-15");
      }
    }

    @Test
    @DisplayName("Should find Documents with not equals (ne) date search")
    void shouldFindDocumentsWithNotEqualsDate() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents with different dates
        prepareDocument(helper, withZoneInfo("2023-06-10T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-20T10:00:00.000"));

        // WHEN: Search for _lastUpdated=ne2023-06-15
        Query query = createDateComparisonQuery(ParamPrefixEnum.NOT_EQUAL, "2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find doc1 and doc3 (not on 2023-06-15)
        assertEquals(2, results.size(), "Should find two Documents not on 2023-06-15");
      }
    }
  }

  // ================================================================================
  // Date Precision Tests
  // ================================================================================

  @Nested
  @DisplayName("Date Precision Tests")
  class DatePrecisionTests {

    @Test
    @DisplayName("Should find Documents matching year precision search")
    void shouldFindDocumentsWithYearPrecision() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Documents from different years
        prepareDocument(helper, withZoneInfo("2023-01-01T00:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-12-31T23:59:59.999"));
        prepareDocument(helper, withZoneInfo("2024-01-01T00:00:00.000"));

        // WHEN: Search for _lastUpdated=2023 (year precision)
        Query query = createExactDateQuery("2023");
        List<Document> results = helper.search(query);

        // THEN: Should find all three documents from 2023
        assertEquals(3, results.size(), "Should find all Documents from 2023");
      }
    }

    @Test
    @DisplayName("Should find Documents matching month precision search")
    void shouldFindDocumentsWithMonthPrecision() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Documents from different months
        prepareDocument(helper, withZoneInfo("2023-06-01T00:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-30T23:59:59.999"));
        prepareDocument(helper, withZoneInfo("2023-07-01T00:00:00.000"));

        // WHEN: Search for _lastUpdated=2023-06 (month precision)
        Query query = createExactDateQuery("2023-06");
        List<Document> results = helper.search(query);

        // THEN: Should find all three documents from June 2023
        assertEquals(3, results.size(), "Should find all Documents from June 2023");
      }
    }

    @Test
    @DisplayName("Should handle boundary dates correctly")
    void shouldHandleBoundaryDatesCorrectly() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Documents at exact boundaries
        prepareDocument(helper, withZoneInfo("2023-06-15T00:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-15T23:59:59.999"));
        prepareDocument(helper, withZoneInfo("2023-06-16T00:00:00.000"));

        // WHEN: Search for _lastUpdated=2023-06-15
        Query query = createExactDateQuery("2023-06-15");
        List<Document> results = helper.search(query);

        // THEN: Should find both documents on 2023-06-15 (including boundary times)
        assertEquals(2, results.size(), "Should find both Documents on 2023-06-15");
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

        // GIVEN: Two documents with dates
        prepareDocument(helper, withZoneInfo("2023-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2023-06-16T10:00:00.000"));

        // WHEN: Search with empty date parameter
        Query query = createEmptyDateQuery();
        List<Document> results = helper.search(query);

        // THEN: Empty parameter should match all documents
        assertEquals(2, results.size(), "Empty date parameter should match all Documents");
      }
    }

    @Test
    @DisplayName("Should handle date range spanning multiple years")
    void shouldHandleDateRangeSpanningMultipleYears() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Documents from different years
        prepareDocument(helper, withZoneInfo("2020-01-01T00:00:00.000"));
        prepareDocument(helper, withZoneInfo("2022-06-15T10:00:00.000"));
        prepareDocument(helper, withZoneInfo("2024-12-31T23:59:59.999"));

        // WHEN: Search for dates after 2021
        Query query = createDateComparisonQuery(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2022");
        List<Document> results = helper.search(query);

        // THEN: Should find documents from 2022 and later
        assertEquals(2, results.size(), "Should find Documents from 2022 onwards");
      }
    }
  }
}

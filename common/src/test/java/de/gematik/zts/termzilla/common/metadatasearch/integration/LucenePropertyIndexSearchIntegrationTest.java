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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_PREFIX_PROPERTY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_DATE_END;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_DATE_START;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_DOUBLE_POINT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_LONG_POINT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_EXACT;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.common.metadatasearch.integration.helper.InMemoryLuceneTestHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ConceptComponentFieldGeneratorHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils.DateUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests verifying the full round-trip of indexing and querying concept properties
 * through the Lucene index.
 *
 * <p>Each test indexes one or more documents via {@link ConceptComponentFieldGeneratorHelper} and
 * then queries the in-memory index to verify correct retrieval.
 */
@DisplayName("Lucene Property Index Search Integration Tests")
class LucenePropertyIndexSearchIntegrationTest {

  private InMemoryLuceneTestHelper luceneHelper;

  @BeforeEach
  void setUp() throws IOException {
    luceneHelper = new InMemoryLuceneTestHelper();
  }

  @AfterEach
  void tearDown() throws IOException {
    luceneHelper.close();
  }

  // ========================================================================
  // Helper Methods
  // ========================================================================

  private static String propertyFieldName(String propertyName, String suffix) {
    return NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + suffix;
  }

  private Document createDocumentWithCode(String code) {
    Document document = new Document();
    ConceptComponentFieldGeneratorHelper.addCodeFieldToDocument(document, code, Field.Store.YES);
    return document;
  }

  // ========================================================================
  // Boolean Property Tests
  // ========================================================================

  @Nested
  @DisplayName("Boolean Property Round-Trip")
  class BooleanPropertyTests {

    @Test
    @DisplayName("Should find concept by exact boolean property match")
    void shouldFindByExactBooleanMatch() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addBooleanPropertyFieldToDocument(
          document, "active", true, Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("active", SUFFIX_STRING_EXACT);
      Query query = new TermQuery(new Term(fieldName, "true"));
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results).as("Should find the concept with active=true").hasSize(1);
    }

    @Test
    @DisplayName("Should not find concept when boolean value does not match")
    void shouldNotFindWhenBooleanDoesNotMatch() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addBooleanPropertyFieldToDocument(
          document, "active", true, Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("active", SUFFIX_STRING_EXACT);
      Query query = new TermQuery(new Term(fieldName, "false"));
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results)
          .as("Should not find concept when querying for false but value is true")
          .isEmpty();
    }
  }

  // ========================================================================
  // Integer Property Tests
  // ========================================================================

  @Nested
  @DisplayName("Integer Property Round-Trip")
  class IntegerPropertyTests {

    @Test
    @DisplayName("Should find concept by exact integer property match")
    void shouldFindByExactIntegerMatch() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, "weight", 42, Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("weight", SUFFIX_LONG_POINT);
      Query query = LongPoint.newExactQuery(fieldName, 42);
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results).as("Should find the concept with weight=42").hasSize(1);
    }

    @Test
    @DisplayName("Should find concept by integer range query")
    void shouldFindByIntegerRangeQuery() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, "weight", 42, Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("weight", SUFFIX_LONG_POINT);
      Query query = LongPoint.newRangeQuery(fieldName, 40, 50);
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results).as("Should find concept with weight=42 in range [40, 50]").hasSize(1);
    }

    @Test
    @DisplayName("Should not find concept when integer value is outside range")
    void shouldNotFindWhenIntegerOutsideRange() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, "weight", 42, Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("weight", SUFFIX_LONG_POINT);
      Query query = LongPoint.newExactQuery(fieldName, 99);
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results)
          .as("Should not find concept when querying for 99 but value is 42")
          .isEmpty();
    }
  }

  // ========================================================================
  // Decimal Property Tests
  // ========================================================================

  @Nested
  @DisplayName("Decimal Property Round-Trip")
  class DecimalPropertyTests {

    @Test
    @DisplayName("Should find concept by exact decimal property match via DoublePoint")
    void shouldFindByExactDecimalMatch() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, "score", new BigDecimal("3.14"), Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("score", SUFFIX_DOUBLE_POINT);
      Query query = DoublePoint.newExactQuery(fieldName, 3.14);
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results).as("Should find the concept with score=3.14").hasSize(1);
    }

    @Test
    @DisplayName("Should find concept by exact decimal string match for lossless comparison")
    void shouldFindByExactDecimalStringMatch() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, "score", new BigDecimal("1.23456789012345"), Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("score", SUFFIX_STRING_EXACT);
      Query query = new TermQuery(new Term(fieldName, "1.23456789012345"));
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results).as("Should find concept by exact BigDecimal string match").hasSize(1);
    }

    @Test
    @DisplayName("Should find concept by decimal range query")
    void shouldFindByDecimalRangeQuery() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, "score", new BigDecimal("3.14"), Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("score", SUFFIX_DOUBLE_POINT);
      Query query = DoublePoint.newRangeQuery(fieldName, 3.0, 4.0);
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results).as("Should find concept with score=3.14 in range [3.0, 4.0]").hasSize(1);
    }

    @Test
    @DisplayName("Should not find concept when decimal value does not match")
    void shouldNotFindWhenDecimalDoesNotMatch() throws IOException {
      // GIVEN
      Document document = createDocumentWithCode("concept-1");
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, "score", new BigDecimal("3.14"), Field.Store.YES);
      luceneHelper.indexDocument(document);

      // WHEN
      String fieldName = propertyFieldName("score", SUFFIX_DOUBLE_POINT);
      Query query = DoublePoint.newExactQuery(fieldName, 99.99);
      List<Document> results = luceneHelper.search(query);

      // THEN
      assertThat(results)
          .as("Should not find concept when querying for 99.99 but value is 3.14")
          .isEmpty();
    }
  }

  // ========================================================================
  // DateTime Property Tests — Equals Filter Semantics
  // ========================================================================

  @Nested
  @DisplayName("DateTime Property Equals Filter Round-Trip")
  class DateTimePropertyEqualsFilterTests {

    /**
     * Builds an overlap query simulating the FHIR property equals filter for dateTime values.
     *
     * <p>Two dateTime periods overlap when: {@code indexed_start <= filter_end AND indexed_end >=
     * filter_start}
     */
    private Query createDateTimeEqualsQuery(String propertyName, String filterValue) {
      DateTimeType filterDate = new DateTimeType(filterValue);
      long filterStart = DateUtil.getFirstInstantOfDateTime(filterDate);
      long filterEnd = DateUtil.getLastInstantOfDateTime(filterDate);

      String startFieldName = propertyFieldName(propertyName, SUFFIX_DATE_START);
      String endFieldName = propertyFieldName(propertyName, SUFFIX_DATE_END);

      return new BooleanQuery.Builder()
          .add(
              LongPoint.newRangeQuery(startFieldName, Long.MIN_VALUE, filterEnd),
              BooleanClause.Occur.MUST)
          .add(
              LongPoint.newRangeQuery(endFieldName, filterStart, Long.MAX_VALUE),
              BooleanClause.Occur.MUST)
          .build();
    }

    private void indexConceptWithDateTime(String code, String dateTimeValue) throws IOException {
      Document document = createDocumentWithCode(code);
      ConceptComponentFieldGeneratorHelper.addDateTimePropertyFieldToDocument(
          document, "effective", new DateTimeType(dateTimeValue), Field.Store.YES);
      luceneHelper.indexDocument(document);
    }

    // --- Same-Precision Equals ---

    @Test
    @DisplayName("Equals filter should match concept with identical day-precision value")
    void shouldMatchSameDayPrecision() throws IOException {
      // GIVEN
      indexConceptWithDateTime("concept-1", "2024-06-15");

      // WHEN — filter: effective = 2024-06-15
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06-15"));

      // THEN
      assertThat(results).as("Same day-precision value should match").hasSize(1);
    }

    @Test
    @DisplayName("Equals filter should not match concept with different day value")
    void shouldNotMatchDifferentDay() throws IOException {
      // GIVEN
      indexConceptWithDateTime("concept-1", "2024-06-15");

      // WHEN — filter: effective = 2024-06-16
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06-16"));

      // THEN
      assertThat(results).as("Different day should not match").isEmpty();
    }

    // --- Cross-Precision Equals ---

    @Test
    @DisplayName(
        "Equals filter with day value should match month-precision property that contains the day")
    void shouldMatchMonthPropertyWithDayFilter() throws IOException {
      // GIVEN — property has month precision "2024-06" (covers June 1–30)
      indexConceptWithDateTime("june-concept", "2024-06");
      indexConceptWithDateTime("july-concept", "2024-07");

      // WHEN — filter: effective = 2024-06-15 (day within June)
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06-15"));

      // THEN — only June overlaps with June 15
      assertThat(results).as("Month-precision June should overlap with day June 15").hasSize(1);
    }

    @Test
    @DisplayName(
        "Equals filter with month value should match day-precision property within that month")
    void shouldMatchDayPropertyWithMonthFilter() throws IOException {
      // GIVEN — properties at day precision
      indexConceptWithDateTime("may-concept", "2024-05-31");
      indexConceptWithDateTime("june-concept", "2024-06-15");
      indexConceptWithDateTime("july-concept", "2024-07-01");

      // WHEN — filter: effective = 2024-06 (entire June)
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06"));

      // THEN — only the June 15 concept falls within the June period
      assertThat(results)
          .as("Only the day within June should match a month-precision filter")
          .hasSize(1);
    }

    @Test
    @DisplayName(
        "Equals filter with day value should match year-precision property that contains the day")
    void shouldMatchYearPropertyWithDayFilter() throws IOException {
      // GIVEN
      indexConceptWithDateTime("year-2024", "2024");
      indexConceptWithDateTime("year-2023", "2023");

      // WHEN — filter: effective = 2024-06-15
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06-15"));

      // THEN — only year 2024 contains June 15, 2024
      assertThat(results)
          .as("Year 2024 should overlap with day 2024-06-15, year 2023 should not")
          .hasSize(1);
    }

    @Test
    @DisplayName(
        "Equals filter with year value should match all day-precision properties within that year")
    void shouldMatchDayPropertiesWithYearFilter() throws IOException {
      // GIVEN
      indexConceptWithDateTime("dec-2023", "2023-12-31");
      indexConceptWithDateTime("jan-2024", "2024-01-15");
      indexConceptWithDateTime("jun-2024", "2024-06-15");
      indexConceptWithDateTime("jan-2025", "2025-01-01");

      // WHEN — filter: effective = 2024
      List<Document> results = luceneHelper.search(createDateTimeEqualsQuery("effective", "2024"));

      // THEN — only the two days within 2024 should match
      assertThat(results).as("Only concepts within year 2024 should match").hasSize(2);
    }

    // --- Mixed Precision Overlap ---

    @Test
    @DisplayName(
        "Equals filter should find all concepts whose periods overlap with the filter value")
    void shouldFindAllOverlappingPrecisionLevels() throws IOException {
      // GIVEN — concepts at year, month, day precision, plus a non-overlapping month
      indexConceptWithDateTime("year-concept", "2024");
      indexConceptWithDateTime("month-concept", "2024-06");
      indexConceptWithDateTime("day-concept", "2024-06-15");
      indexConceptWithDateTime("other-month", "2024-08");

      // WHEN — filter: effective = 2024-06-15
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06-15"));

      // THEN — year 2024, month June, and day June 15 all overlap; August does not
      assertThat(results)
          .as("Year, month, and day concepts should match; non-overlapping August should not")
          .hasSize(3);
    }

    // --- Adjacent Boundary Tests ---

    @Test
    @DisplayName("Equals filter should not match adjacent months")
    void shouldNotMatchAdjacentMonths() throws IOException {
      // GIVEN
      indexConceptWithDateTime("may", "2024-05");
      indexConceptWithDateTime("june", "2024-06");
      indexConceptWithDateTime("july", "2024-07");

      // WHEN — filter: effective = 2024-06
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06"));

      // THEN — only June matches; May and July are adjacent but do not overlap
      assertThat(results)
          .as("Only the matching month should be found, not adjacent months")
          .hasSize(1);
    }

    @Test
    @DisplayName("Equals filter should not match adjacent days")
    void shouldNotMatchAdjacentDays() throws IOException {
      // GIVEN
      indexConceptWithDateTime("day-14", "2024-06-14");
      indexConceptWithDateTime("day-15", "2024-06-15");
      indexConceptWithDateTime("day-16", "2024-06-16");

      // WHEN — filter: effective = 2024-06-15
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06-15"));

      // THEN — only June 15 matches
      assertThat(results).as("Only the exact day should match, not adjacent days").hasSize(1);
    }

    // --- Month Boundary Edge Case ---

    @Test
    @DisplayName("Equals filter for last day of month should not match next month")
    void shouldNotMatchNextMonthAtBoundary() throws IOException {
      // GIVEN
      indexConceptWithDateTime("june-30", "2024-06-30");
      indexConceptWithDateTime("july-01", "2024-07-01");

      // WHEN — filter: effective = 2024-06
      List<Document> results =
          luceneHelper.search(createDateTimeEqualsQuery("effective", "2024-06"));

      // THEN — June 30 is in June; July 1 is not
      assertThat(results)
          .as("Last day of June should match month filter, first day of July should not")
          .hasSize(1);
    }
  }

  // ========================================================================
  // Mixed Property Types Isolation Test
  // ========================================================================

  @Nested
  @DisplayName("Mixed Property Types Isolation")
  class MixedPropertyTypesTests {

    @Test
    @DisplayName("Should correctly isolate different property types across multiple concepts")
    void shouldIsolatePropertyTypes() throws IOException {
      // GIVEN — Index three concepts with different property types
      Document doc1 = createDocumentWithCode("boolean-concept");
      ConceptComponentFieldGeneratorHelper.addBooleanPropertyFieldToDocument(
          doc1, "active", true, Field.Store.YES);
      luceneHelper.indexDocument(doc1);

      Document doc2 = createDocumentWithCode("integer-concept");
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          doc2, "weight", 42, Field.Store.YES);
      luceneHelper.indexDocument(doc2);

      Document doc3 = createDocumentWithCode("decimal-concept");
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          doc3, "score", new BigDecimal("3.14"), Field.Store.YES);
      luceneHelper.indexDocument(doc3);

      // WHEN — Query for boolean property
      String booleanFieldName = propertyFieldName("active", SUFFIX_STRING_EXACT);
      Query booleanQuery = new TermQuery(new Term(booleanFieldName, "true"));
      List<Document> booleanResults = luceneHelper.search(booleanQuery);

      // WHEN — Query for integer property
      String intFieldName = propertyFieldName("weight", SUFFIX_LONG_POINT);
      Query intQuery = LongPoint.newExactQuery(intFieldName, 42);
      List<Document> intResults = luceneHelper.search(intQuery);

      // WHEN — Query for decimal property
      String decimalFieldName = propertyFieldName("score", SUFFIX_DOUBLE_POINT);
      Query decimalQuery = DoublePoint.newExactQuery(decimalFieldName, 3.14);
      List<Document> decimalResults = luceneHelper.search(decimalQuery);

      // THEN — Each query should return exactly one result
      assertThat(booleanResults).as("Boolean query should match exactly one concept").hasSize(1);
      assertThat(intResults).as("Integer query should match exactly one concept").hasSize(1);
      assertThat(decimalResults).as("Decimal query should match exactly one concept").hasSize(1);
    }
  }
}

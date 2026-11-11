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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.StringParam;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for QueryCombinators utility class.
 *
 * <p>Tests verify correct construction of Lucene Boolean queries using different combination
 * patterns (FILTER, MUST, SHOULD) and FHIR-specific query helpers.
 */
@DisplayName("QueryCombinators Tests")
class QueryCombinatorsTest {

  // ================================================================================
  // Basic Query Combination Tests
  // ================================================================================

  @Nested
  @DisplayName("Filter Query Combination")
  class FilterQueryTests {

    @Test
    @DisplayName("Should combine multiple queries with FILTER clauses")
    void shouldCombineQueriesWithFilterClauses() {
      // GIVEN
      Query query1 = new TermQuery(new Term("field1", "value1"));
      Query query2 = new TermQuery(new Term("field2", "value2"));
      List<Query> queries = Arrays.asList(query1, query2);

      // WHEN
      Query result = QueryCombinators.prepareFilterQuery(queries);

      // THEN
      assertNotNull(result);
      assertInstanceOf(BooleanQuery.class, result);

      BooleanQuery booleanQuery = (BooleanQuery) result;
      assertEquals(2, booleanQuery.clauses().size());

      // All clauses should be FILTER
      assertTrue(
          booleanQuery.clauses().stream()
              .allMatch(clause -> clause.occur() == BooleanClause.Occur.FILTER),
          "All clauses should be FILTER");
    }

    @Test
    @DisplayName("Should handle empty query list")
    void shouldHandleEmptyQueryList() {
      // GIVEN
      List<Query> queries = Collections.emptyList();

      // WHEN
      Query result = QueryCombinators.prepareFilterQuery(queries);

      // THEN
      assertNotNull(result);
      assertInstanceOf(BooleanQuery.class, result);
      assertEquals(0, ((BooleanQuery) result).clauses().size());
    }

    @Test
    @DisplayName("Should throw exception for null query list")
    void shouldThrowExceptionForNullQueryList() {
      // WHEN & THEN
      assertThrows(IllegalArgumentException.class, () -> QueryCombinators.prepareFilterQuery(null));
    }

    @Test
    @DisplayName("Should skip null queries in list")
    void shouldSkipNullQueriesInList() {
      // GIVEN
      Query query1 = new TermQuery(new Term("field1", "value1"));
      List<Query> queries = Arrays.asList(query1, null, null);

      // WHEN
      Query result = QueryCombinators.prepareFilterQuery(queries);

      // THEN
      BooleanQuery booleanQuery = (BooleanQuery) result;
      assertEquals(1, booleanQuery.clauses().size(), "Should only include non-null queries");
    }
  }

  @Nested
  @DisplayName("AND Query Combination")
  class AndQueryTests {

    @Test
    @DisplayName("Should combine multiple queries with MUST clauses")
    void shouldCombineQueriesWithMustClauses() {
      // GIVEN
      Query query1 = new TermQuery(new Term("name", "snomed"));
      Query query2 = new TermQuery(new Term("status", "active"));
      List<Query> queries = Arrays.asList(query1, query2);

      // WHEN
      Query result = QueryCombinators.prepareAndQuery(queries);

      // THEN
      assertNotNull(result);
      assertInstanceOf(BooleanQuery.class, result);

      BooleanQuery booleanQuery = (BooleanQuery) result;
      assertEquals(2, booleanQuery.clauses().size());

      // All clauses should be MUST
      assertTrue(
          booleanQuery.clauses().stream()
              .allMatch(clause -> clause.occur() == BooleanClause.Occur.MUST),
          "All clauses should be MUST");
    }

    @Test
    @DisplayName("Should throw exception for null query list")
    void shouldThrowExceptionForNullQueryList() {
      assertThrows(IllegalArgumentException.class, () -> QueryCombinators.prepareAndQuery(null));
    }
  }

  @Nested
  @DisplayName("OR Query Combination")
  class OrQueryTests {

    @Test
    @DisplayName("Should combine multiple queries with SHOULD clauses")
    void shouldCombineQueriesWithShouldClauses() {
      // GIVEN
      Query query1 = new TermQuery(new Term("name", "snomed"));
      Query query2 = new TermQuery(new Term("name", "loinc"));
      List<Query> queries = Arrays.asList(query1, query2);

      // WHEN
      Query result = QueryCombinators.prepareOrQuery(queries);

      // THEN
      assertNotNull(result);
      assertInstanceOf(BooleanQuery.class, result);

      BooleanQuery booleanQuery = (BooleanQuery) result;
      assertEquals(2, booleanQuery.clauses().size());

      // All clauses should be SHOULD
      assertTrue(
          booleanQuery.clauses().stream()
              .allMatch(clause -> clause.occur() == BooleanClause.Occur.SHOULD),
          "All clauses should be SHOULD");
    }

    @Test
    @DisplayName("Should throw exception for null query list")
    void shouldThrowExceptionForNullQueryList() {
      assertThrows(IllegalArgumentException.class, () -> QueryCombinators.prepareOrQuery(null));
    }
  }

  // ================================================================================
  // Combined Filter + Must Query Tests
  // ================================================================================

  @Nested
  @DisplayName("Combined Filter and Must Query")
  class CombinedFilterMustQueryTests {

    @Test
    @DisplayName("Should combine filter and must queries correctly")
    void shouldCombineFilterAndMustQueries() {
      // GIVEN
      Query filterQuery = new TermQuery(new Term("scope", "default"));
      Query mustQuery = new TermQuery(new Term("name", "snomed"));

      // WHEN
      Query result = QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, mustQuery);

      // THEN
      assertNotNull(result);
      assertInstanceOf(BooleanQuery.class, result);

      BooleanQuery booleanQuery = (BooleanQuery) result;
      assertEquals(2, booleanQuery.clauses().size());

      // Should have one FILTER and one MUST clause
      long filterCount =
          booleanQuery.clauses().stream()
              .filter(clause -> clause.occur() == BooleanClause.Occur.FILTER)
              .count();
      long mustCount =
          booleanQuery.clauses().stream()
              .filter(clause -> clause.occur() == BooleanClause.Occur.MUST)
              .count();

      assertEquals(1, filterCount, "Should have exactly one FILTER clause");
      assertEquals(1, mustCount, "Should have exactly one MUST clause");
    }

    @Test
    @DisplayName("Should handle empty must query by not adding it")
    void shouldHandleEmptyMustQuery() {
      // GIVEN
      Query filterQuery = new TermQuery(new Term("scope", "default"));
      Query emptyMustQuery = new BooleanQuery.Builder().build(); // Empty query

      // WHEN
      Query result = QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, emptyMustQuery);

      // THEN
      BooleanQuery booleanQuery = (BooleanQuery) result;
      assertEquals(
          1, booleanQuery.clauses().size(), "Should only have FILTER clause, not empty MUST");

      assertEquals(
          BooleanClause.Occur.FILTER,
          booleanQuery.clauses().get(0).occur(),
          "Single clause should be FILTER");
    }

    @Test
    @DisplayName("Should handle null must query")
    void shouldHandleNullMustQuery() {
      // GIVEN
      Query filterQuery = new TermQuery(new Term("scope", "default"));

      // WHEN
      Query result = QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, null);

      // THEN
      BooleanQuery booleanQuery = (BooleanQuery) result;
      assertEquals(1, booleanQuery.clauses().size(), "Should only have FILTER clause");
    }

    @Test
    @DisplayName("Should throw exception for null filter query")
    void shouldThrowExceptionForNullFilterQuery() {
      // GIVEN
      Query mustQuery = new TermQuery(new Term("name", "snomed"));

      // WHEN & THEN
      assertThrows(
          IllegalArgumentException.class,
          () -> QueryCombinators.prepareCombinedFilterMustQuery(null, mustQuery));
    }
  }

  // ================================================================================
  // FHIR-Specific Query Tests
  // ================================================================================

  @Nested
  @DisplayName("Missing Modifier Query")
  class MissingModifierQueryTests {

    @Test
    @DisplayName("Should create query for missing=true")
    void shouldCreateQueryForMissingTrue() {
      // GIVEN
      String paramName = "description";
      StringParam param = new StringParam();
      param.setMissing(true);

      // WHEN
      Query result = QueryCombinators.prepareMissingModifierQuery(paramName, param);

      // THEN
      assertNotNull(result);
      assertInstanceOf(TermQuery.class, result);

      TermQuery termQuery = (TermQuery) result;
      assertEquals("description_missing", termQuery.getTerm().field());
      assertEquals("true", termQuery.getTerm().text());
    }

    @Test
    @DisplayName("Should create query for missing=false")
    void shouldCreateQueryForMissingFalse() {
      // GIVEN
      String paramName = "name";
      StringParam param = new StringParam();
      param.setMissing(false);

      // WHEN
      Query result = QueryCombinators.prepareMissingModifierQuery(paramName, param);

      // THEN
      TermQuery termQuery = (TermQuery) result;
      assertEquals("name_missing", termQuery.getTerm().field());
      assertEquals("false", termQuery.getTerm().text());
    }

    @Test
    @DisplayName("Should throw exception for null parameter name")
    void shouldThrowExceptionForNullParameterName() {
      // GIVEN
      StringParam param = new StringParam();
      param.setMissing(true);

      // WHEN & THEN
      assertThrows(
          IllegalArgumentException.class,
          () -> QueryCombinators.prepareMissingModifierQuery(null, param));
    }

    @Test
    @DisplayName("Should throw exception for null missing flag")
    void shouldThrowExceptionForNullMissingFlag() {
      // GIVEN
      String paramName = "name";
      StringParam param = new StringParam();
      // Missing flag not set (null)

      // WHEN & THEN
      assertThrows(
          NullPointerException.class,
          () -> QueryCombinators.prepareMissingModifierQuery(paramName, param));
    }
  }

  @Nested
  @DisplayName("Resource Type Filtering Query")
  class ResourceTypeFilteringQueryTests {

    @Test
    @DisplayName("Should create resource type filter query")
    void shouldCreateResourceTypeFilterQuery() {
      // GIVEN
      String resourceType = "CodeSystem";
      Query mainQuery = new TermQuery(new Term("name", "snomed"));

      // WHEN
      Query result = QueryCombinators.prepareResourceTypeFilteringQuery(resourceType, mainQuery);

      // THEN
      assertNotNull(result);
      assertInstanceOf(BooleanQuery.class, result);

      BooleanQuery booleanQuery = (BooleanQuery) result;
      assertEquals(2, booleanQuery.clauses().size());

      // Both should be MUST clauses
      assertTrue(
          booleanQuery.clauses().stream()
              .allMatch(clause -> clause.occur() == BooleanClause.Occur.MUST));
    }

    @Test
    @DisplayName("Should lowercase resource type")
    void shouldLowercaseResourceType() {
      // GIVEN
      String resourceType = "CodeSystem";
      Query mainQuery = new TermQuery(new Term("name", "snomed"));

      // WHEN
      Query result = QueryCombinators.prepareResourceTypeFilteringQuery(resourceType, mainQuery);

      // THEN
      BooleanQuery booleanQuery = (BooleanQuery) result;

      // Find the resource type query
      boolean hasLowercaseType =
          booleanQuery.clauses().stream()
              .anyMatch(
                  clause -> {
                    if (clause.query() instanceof TermQuery) {
                      TermQuery tq = (TermQuery) clause.query();
                      return tq.getTerm().field().equals("_type")
                          && tq.getTerm().text().equals("codesystem");
                    }
                    return false;
                  });

      assertTrue(hasLowercaseType, "Resource type should be lowercased to 'codesystem'");
    }

    @Test
    @DisplayName("Should throw exception for null resource type")
    void shouldThrowExceptionForNullResourceType() {
      // GIVEN
      Query mainQuery = new TermQuery(new Term("name", "snomed"));

      // WHEN & THEN
      assertThrows(
          IllegalArgumentException.class,
          () -> QueryCombinators.prepareResourceTypeFilteringQuery(null, mainQuery));
    }

    @Test
    @DisplayName("Should throw exception for null query")
    void shouldThrowExceptionForNullQuery() {
      assertThrows(
          IllegalArgumentException.class,
          () -> QueryCombinators.prepareResourceTypeFilteringQuery("CodeSystem", null));
    }
  }

  // ================================================================================
  // Utility Method Tests
  // ================================================================================

  @Nested
  @DisplayName("Utility Methods")
  class UtilityMethodTests {

    @Test
    @DisplayName("isQueryEmpty should return true for null query")
    void isQueryEmptyShouldReturnTrueForNull() {
      assertTrue(QueryCombinators.isQueryEmpty(null));
    }

    @Test
    @DisplayName("isQueryEmpty should return true for empty BooleanQuery")
    void isQueryEmptyShouldReturnTrueForEmptyBooleanQuery() {
      Query emptyQuery = new BooleanQuery.Builder().build();
      assertTrue(QueryCombinators.isQueryEmpty(emptyQuery));
    }

    @Test
    @DisplayName("isQueryEmpty should return false for non-empty query")
    void isQueryEmptyShouldReturnFalseForNonEmptyQuery() {
      Query query = new TermQuery(new Term("field", "value"));
      assertFalse(QueryCombinators.isQueryEmpty(query));
    }

    @Test
    @DisplayName("createTermQuery should create correct TermQuery")
    void createTermQueryShouldCreateCorrectTermQuery() {
      // WHEN
      Query result = QueryCombinators.createTermQuery("fieldName", "fieldValue");

      // THEN
      assertNotNull(result);
      assertInstanceOf(TermQuery.class, result);

      TermQuery termQuery = (TermQuery) result;
      assertEquals("fieldName", termQuery.getTerm().field());
      assertEquals("fieldValue", termQuery.getTerm().text());
    }

    @Test
    @DisplayName("createTermQuery should throw exception for null field name")
    void createTermQueryShouldThrowExceptionForNullFieldName() {
      assertThrows(
          IllegalArgumentException.class, () -> QueryCombinators.createTermQuery(null, "value"));
    }

    @Test
    @DisplayName("createTermQuery should throw exception for null value")
    void createTermQueryShouldThrowExceptionForNullValue() {
      assertThrows(
          IllegalArgumentException.class, () -> QueryCombinators.createTermQuery("field", null));
    }
  }

  // ================================================================================
  // Instantiation Prevention Test
  // ================================================================================

  @Nested
  @DisplayName("Utility Class Instantiation")
  class InstantiationTests {

    @Test
    @DisplayName("Should throw AssertionError when attempting to instantiate")
    void shouldThrowAssertionErrorWhenInstantiating() throws Exception {
      // Use reflection to call private constructor
      var constructor = QueryCombinators.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      // Reflection wraps the AssertionError in InvocationTargetException
      var exception =
          assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);

      // Verify the cause is the expected AssertionError
      assertNotNull(exception.getCause(), "Should have a cause");
      assertInstanceOf(
          AssertionError.class,
          exception.getCause(),
          "Cause should be AssertionError, but was: " + exception.getCause().getClass());
      assertTrue(
          exception.getCause().getMessage().contains("utility class"),
          "Message should mention utility class");
    }
  }
}

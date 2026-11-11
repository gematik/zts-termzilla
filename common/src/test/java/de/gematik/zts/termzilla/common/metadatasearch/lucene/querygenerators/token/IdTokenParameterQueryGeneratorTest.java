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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for IdTokenParameterQueryGenerator.
 *
 * <p>The IdTokenParameterQueryGenerator is a static utility class used specifically for the '_id'
 * search parameter, which has special behavior compared to regular token parameters (e.g., no
 * :missing modifier support).
 *
 * <p><b>Test Strategy:</b>
 *
 * <ul>
 *   <li>Test standard ID search (exact match)
 *   <li>Test :not modifier (negation)
 *   <li>Test that unsupported modifiers throw NotImplementedException
 *   <li>Test edge cases (empty params, null values)
 *   <li>Test utility class instantiation prevention
 * </ul>
 */
@DisplayName("IdTokenParameterQueryGenerator Tests")
class IdTokenParameterQueryGeneratorTest {

  // ================================================================================
  // Standard Search Tests
  // ================================================================================

  @Nested
  @DisplayName("Standard ID Search")
  class StandardSearchTests {

    @Test
    @DisplayName("Should create TermQuery for simple ID search")
    void shouldCreateTermQueryForSimpleIdSearch() throws NotImplementedException {
      // GIVEN
      String resourceId = "icd-10-gm-2025";
      String searchParam = "_id";
      TokenParam param = new TokenParam(resourceId);

      // WHEN
      Query query = IdTokenParameterQueryGenerator.prepareQuery(searchParam, param);

      // THEN
      assertNotNull(query, "Query should not be null");
      assertInstanceOf(TermQuery.class, query, "Query should be a TermQuery");

      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          "_id_tokenexact", termQuery.getTerm().field(), "Field name should be _id_tokenexact");
      assertEquals(resourceId, termQuery.getTerm().text(), "Term value should be icd-10-gm-2025");
    }

    @Test
    @DisplayName("Should handle ID with special characters")
    void shouldHandleIdWithSpecialCharacters() throws NotImplementedException {
      // GIVEN
      String searchParam = "_id";
      String resourceId = "CodeSystem-with-dash_and_underscore.123";
      TokenParam param = new TokenParam(resourceId);

      // WHEN
      Query query = IdTokenParameterQueryGenerator.prepareQuery(searchParam, param);

      // THEN
      assertNotNull(query);
      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          resourceId, termQuery.getTerm().text(), "Should preserve special characters in ID");
    }

    @Test
    @DisplayName("Should throw NotImplementedException when value is null")
    void shouldThrowExceptionWhenValueIsNull() {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam();
      param.setValue(null);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> IdTokenParameterQueryGenerator.prepareStandardQuery(paramName, param),
              "Should throw NotImplementedException for null ID value");

      assertTrue(
          exception.getMessage().contains("You have to provide at least an 'id'"),
          "Exception message should mention missing ID");
    }
  }

  // ================================================================================
  // Empty Parameter Tests
  // ================================================================================

  @Nested
  @DisplayName("Empty Parameter Handling")
  class EmptyParameterTests {

    @Test
    @DisplayName("Should return MatchAllDocsQuery for empty parameter")
    void shouldReturnMatchAllDocsQueryForEmptyParameter() throws NotImplementedException {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam();
      // Empty param: no value, no missing modifier

      // WHEN
      Query query = IdTokenParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertInstanceOf(
          MatchAllDocsQuery.class, query, "Empty parameter should return MatchAllDocsQuery");
    }
  }

  // ================================================================================
  // :not Modifier Tests
  // ================================================================================

  @Nested
  @DisplayName(":not Modifier")
  class NotModifierTests {

    @Test
    @DisplayName("Should create negated query for :not modifier")
    void shouldCreateNegatedQueryForNotModifier() throws NotImplementedException {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("CodeSystem-123");
      param.setModifier(TokenParamModifier.NOT);

      // WHEN
      Query query = IdTokenParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertInstanceOf(BooleanQuery.class, query, "Query should be a BooleanQuery");

      BooleanQuery booleanQuery = (BooleanQuery) query;
      assertEquals(2, booleanQuery.clauses().size(), "Should have 2 clauses");

      // Check for MUST_NOT clause (the ID we want to exclude)
      assertTrue(
          booleanQuery.clauses().stream()
              .anyMatch(clause -> clause.occur() == BooleanClause.Occur.MUST_NOT),
          "Should have MUST_NOT clause");

      // Check for MUST clause (MatchAllDocsQuery to get all other docs)
      assertTrue(
          booleanQuery.clauses().stream()
              .anyMatch(clause -> clause.occur() == BooleanClause.Occur.MUST),
          "Should have MUST clause");
    }
  }

  // ================================================================================
  // Unsupported Modifiers Tests
  // ================================================================================

  @Nested
  @DisplayName("Unsupported Modifiers")
  class UnsupportedModifiersTests {

    @Test
    @DisplayName("Should throw NotImplementedException for :missing modifier")
    void shouldRejectMissingModifier() {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("CodeSystem-123");
      param.setMissing(true);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> IdTokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :missing modifier");

      assertTrue(
          exception.getMessage().contains("not combinable with the ':missing' modifier"),
          "Exception should mention :missing incompatibility");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :text modifier")
    void shouldRejectTextModifier() {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("CodeSystem-123");
      param.setModifier(TokenParamModifier.TEXT);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> IdTokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :text modifier");

      assertTrue(
          exception.getMessage().contains("not combinable with the ':text' modifier"),
          "Exception should mention :text incompatibility");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :above modifier")
    void shouldRejectAboveModifier() {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("CodeSystem-123");
      param.setModifier(TokenParamModifier.ABOVE);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> IdTokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :above modifier");

      assertTrue(
          exception.getMessage().contains("not combinable with the ':above' modifier"),
          "Exception should mention :above incompatibility");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :below modifier")
    void shouldRejectBelowModifier() {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("CodeSystem-123");
      param.setModifier(TokenParamModifier.BELOW);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> IdTokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :below modifier");

      assertTrue(
          exception.getMessage().contains("not combinable with the ':below' modifier"),
          "Exception should mention :below incompatibility");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :in modifier")
    void shouldRejectInModifier() {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("CodeSystem-123");
      param.setModifier(TokenParamModifier.IN);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> IdTokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :in modifier");

      assertTrue(
          exception.getMessage().contains("not combinable with the ':in' modifier"),
          "Exception should mention :in incompatibility");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :not-in modifier")
    void shouldRejectNotInModifier() {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("CodeSystem-123");
      param.setModifier(TokenParamModifier.NOT_IN);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> IdTokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :not-in modifier");

      assertTrue(
          exception.getMessage().contains("not combinable with the ':not-in' modifier"),
          "Exception should mention :not-in incompatibility");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :of-type modifier")
    void shouldRejectOfTypeModifier() {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("CodeSystem-123");
      param.setModifier(TokenParamModifier.OF_TYPE);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> IdTokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :of-type modifier");

      assertTrue(
          exception.getMessage().contains("not combinable with the ':of-type' modifier"),
          "Exception should mention :of-type incompatibility");
    }
  }

  // ================================================================================
  // Field Name Construction Tests
  // ================================================================================

  @Nested
  @DisplayName("Field Name Construction")
  class FieldNameConstructionTests {

    @Test
    @DisplayName("Should construct field name with SEPARATOR and SUFFIX_TOKEN_EXACT")
    void shouldConstructCorrectFieldName() throws NotImplementedException {
      // GIVEN
      String paramName = "_id";
      TokenParam param = new TokenParam("test-id");

      // WHEN
      Query query = IdTokenParameterQueryGenerator.prepareStandardQuery(paramName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      String expectedFieldName = "_id_tokenexact"; // SEPARATOR="_", SUFFIX_TOKEN_EXACT="tokenexact"
      assertEquals(
          expectedFieldName,
          termQuery.getTerm().field(),
          "Field name should follow pattern: _id_tokenexact");
    }

    @Test
    @DisplayName("Should work with different parameter names")
    void shouldWorkWithDifferentParameterNames() throws NotImplementedException {
      // GIVEN
      String customParamName = "customId";
      TokenParam param = new TokenParam("value-123");

      // WHEN
      Query query = IdTokenParameterQueryGenerator.prepareStandardQuery(customParamName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          "customId_tokenexact",
          termQuery.getTerm().field(),
          "Should work with any parameter name");
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
      var constructor = IdTokenParameterQueryGenerator.class.getDeclaredConstructor();
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

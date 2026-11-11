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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_TOKEN_LOWERCASE_EXACT;
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
 * Unit tests for TokenParameterQueryGenerator.
 *
 * <p>The TokenParameterQueryGenerator is a static utility class for FHIR token-type search
 * parameters such as identifier, boolean, code, coding, and codeableConcept. Token parameters
 * support system|value pair matching with various search modifiers.
 *
 * <p><b>Test Strategy:</b>
 *
 * <ul>
 *   <li>Test standard token search (exact match with system|value and value only)
 *   <li>Test :not modifier (negation)
 *   <li>Test :missing modifier (presence/absence)
 *   <li>Test that unsupported modifiers throw NotImplementedException
 *   <li>Test edge cases (empty params, null values, case-insensitive matching)
 *   <li>Test utility class instantiation prevention
 * </ul>
 */
@DisplayName("TokenParameterQueryGenerator Tests")
class TokenParameterQueryGeneratorTest {

  // ================================================================================
  // Standard Search Tests
  // ================================================================================

  @Nested
  @DisplayName("Standard Token Search")
  class StandardSearchTests {

    @Test
    @DisplayName("Should create TermQuery for token with value only")
    void shouldCreateTermQueryForValueOnly() throws NotImplementedException {
      // GIVEN
      String paramName = "status";
      TokenParam param = new TokenParam("active");

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query, "Query should not be null");
      assertInstanceOf(TermQuery.class, query, "Query should be a TermQuery");

      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT,
          termQuery.getTerm().field(),
          "Field name should be " + paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT);
      assertEquals("active", termQuery.getTerm().text(), "Term value should be 'active'");
    }

    @Test
    @DisplayName("Should create TermQuery for token with system and value")
    void shouldCreateTermQueryForSystemAndValue() throws NotImplementedException {
      // GIVEN
      String paramName = "identifier";
      TokenParam param = new TokenParam("http://hospital.org", "12345");

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertInstanceOf(TermQuery.class, query, "Query should be a TermQuery");

      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT,
          termQuery.getTerm().field(),
          "Field name should be " + paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT);
      assertEquals(
          "http://hospital.org|12345",
          termQuery.getTerm().text(),
          "Term value should be 'http://hospital.org|12345'");
    }

    // TODO: check behavior against FHIR spec - case insensitivity for url might be a problem
    @Test
    @DisplayName("Should perform case-insensitive matching by lowercasing values")
    void shouldLowercaseValues() throws NotImplementedException {
      // GIVEN
      String paramName = "code";
      TokenParam param = new TokenParam("http://SNOMED.info/sct", "12345ABC");

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareStandardQuery(paramName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          "http://snomed.info/sct|12345abc",
          termQuery.getTerm().text(),
          "System and value should be lowercased");
    }

    // TODO: check behavior against FHIR spec
    @Test
    @DisplayName("Should lowercase value-only token")
    void shouldLowercaseValueOnlyToken() throws NotImplementedException {
      // GIVEN
      String paramName = "status";
      TokenParam param = new TokenParam("ACTIVE");

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareStandardQuery(paramName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      assertEquals("active", termQuery.getTerm().text(), "Value should be lowercased");
    }

    // TODO: check behavior against FHIR spec
    @Test
    @DisplayName("Should throw NotImplementedException when both system and value are null")
    void shouldThrowExceptionWhenBothNull() {
      // GIVEN
      String paramName = "identifier";
      TokenParam param = new TokenParam();
      param.setSystem(null);
      param.setValue(null);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> TokenParameterQueryGenerator.prepareStandardQuery(paramName, param),
              "Should throw NotImplementedException when both system and value are null");

      assertTrue(
          exception.getMessage().contains("Scenario not implemented yet"),
          "Exception message should mention scenario not implemented");
    }

    @Test
    @DisplayName("Should throw NotImplementedException when only system is provided")
    void shouldThrowExceptionWhenOnlySystemProvided() {
      // GIVEN
      String paramName = "identifier";
      TokenParam param = new TokenParam();
      param.setSystem("http://example.org");
      param.setValue(null);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> TokenParameterQueryGenerator.prepareStandardQuery(paramName, param),
              "Should throw NotImplementedException when only system is provided");

      assertTrue(
          exception.getMessage().contains("Scenario not implemented yet"),
          "Exception message should mention scenario not implemented");
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
      String paramName = "status";
      TokenParam param = new TokenParam();
      // Empty param: no value, no missing modifier

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertInstanceOf(
          MatchAllDocsQuery.class, query, "Empty parameter should return MatchAllDocsQuery");
    }

    @Test
    @DisplayName("Should return MatchAllDocsQuery for null parameter")
    void shouldReturnMatchAllDocsQueryForNullParameter() throws NotImplementedException {
      // GIVEN
      String paramName = "status";
      TokenParam param = null;

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertInstanceOf(
          MatchAllDocsQuery.class, query, "Null parameter should return MatchAllDocsQuery");
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
      String paramName = "status";
      TokenParam param = new TokenParam("retired");
      param.setModifier(TokenParamModifier.NOT);

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertInstanceOf(BooleanQuery.class, query, "Query should be a BooleanQuery");

      BooleanQuery booleanQuery = (BooleanQuery) query;
      assertEquals(2, booleanQuery.clauses().size(), "Should have 2 clauses");

      // Check for MUST_NOT clause (the value we want to exclude)
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

    @Test
    @DisplayName("Should create negated query with system|value")
    void shouldCreateNegatedQueryWithSystemAndValue() throws NotImplementedException {
      // GIVEN
      String paramName = "identifier";
      TokenParam param = new TokenParam("http://hospital.org", "12345");
      param.setModifier(TokenParamModifier.NOT);

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertInstanceOf(BooleanQuery.class, query, "Query should be a BooleanQuery");

      BooleanQuery booleanQuery = (BooleanQuery) query;
      assertEquals(2, booleanQuery.clauses().size(), "Should have 2 clauses");

      // Verify MUST_NOT clause contains the correct term
      boolean hasMustNotWithCorrectTerm =
          booleanQuery.clauses().stream()
              .anyMatch(
                  clause -> {
                    if (clause.occur() == BooleanClause.Occur.MUST_NOT
                        && clause.query() instanceof TermQuery) {
                      TermQuery termQuery = (TermQuery) clause.query();
                      return "http://hospital.org|12345".equals(termQuery.getTerm().text());
                    }
                    return false;
                  });

      assertTrue(
          hasMustNotWithCorrectTerm,
          "MUST_NOT clause should contain term with system|value format");
    }
  }

  // ================================================================================
  // Unsupported Modifiers Tests
  // ================================================================================

  @Nested
  @DisplayName("Unsupported Modifiers")
  class UnsupportedModifiersTests {

    @Test
    @DisplayName("Should throw NotImplementedException for :text modifier")
    void shouldRejectTextModifier() {
      // GIVEN
      String paramName = "code";
      TokenParam param = new TokenParam("12345");
      param.setModifier(TokenParamModifier.TEXT);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> TokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :text modifier");

      assertTrue(
          exception.getMessage().contains(":text modifier not implemented"),
          "Exception should mention :text not implemented");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :above modifier")
    void shouldRejectAboveModifier() {
      // GIVEN
      String paramName = "code";
      TokenParam param = new TokenParam("12345");
      param.setModifier(TokenParamModifier.ABOVE);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> TokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :above modifier");

      assertTrue(
          exception.getMessage().contains(":above modifier not implemented"),
          "Exception should mention :above not implemented");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :below modifier")
    void shouldRejectBelowModifier() {
      // GIVEN
      String paramName = "code";
      TokenParam param = new TokenParam("12345");
      param.setModifier(TokenParamModifier.BELOW);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> TokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :below modifier");

      assertTrue(
          exception.getMessage().contains(":below modifier not implemented"),
          "Exception should mention :below not implemented");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :in modifier")
    void shouldRejectInModifier() {
      // GIVEN
      String paramName = "code";
      TokenParam param = new TokenParam("12345");
      param.setModifier(TokenParamModifier.IN);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> TokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :in modifier");

      assertTrue(
          exception.getMessage().contains(":in modifier not implemented"),
          "Exception should mention :in not implemented");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :not-in modifier")
    void shouldRejectNotInModifier() {
      // GIVEN
      String paramName = "code";
      TokenParam param = new TokenParam("12345");
      param.setModifier(TokenParamModifier.NOT_IN);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> TokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :not-in modifier");

      assertTrue(
          exception.getMessage().contains(":not-in modifier not implemented"),
          "Exception should mention :not-in not implemented");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :of-type modifier")
    void shouldRejectOfTypeModifier() {
      // GIVEN
      String paramName = "identifier";
      TokenParam param = new TokenParam("12345");
      param.setModifier(TokenParamModifier.OF_TYPE);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> TokenParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :of-type modifier");

      assertTrue(
          exception.getMessage().contains(":of-type modifier not implemented"),
          "Exception should mention :of-type not implemented");
    }
  }

  // ================================================================================
  // Field Name Construction Tests
  // ================================================================================

  @Nested
  @DisplayName("Field Name Construction")
  class FieldNameConstructionTests {

    @Test
    @DisplayName("Should construct field name with SEPARATOR and SUFFIX_TOKEN_LOWERCASE_EXACT")
    void shouldConstructCorrectFieldName() throws NotImplementedException {
      // GIVEN
      String paramName = "status";
      TokenParam param = new TokenParam("active");

      // WHEN
      Query query = TokenParameterQueryGenerator.prepareStandardQuery(paramName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      String expectedFieldName =
          paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT; // "status_lowercaseexact"
      assertEquals(
          expectedFieldName,
          termQuery.getTerm().field(),
          "Field name should follow pattern: "
              + paramName
              + SEPARATOR
              + SUFFIX_TOKEN_LOWERCASE_EXACT);
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
      var constructor = TokenParameterQueryGenerator.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      // Reflection wraps the AssertionError in InvocationTargetException
      var exception =
          assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);

      // Verify the cause is the expected AssertionError
      assertNotNull(exception.getCause(), "Should have a cause");
      assertTrue(
          exception.getCause() instanceof AssertionError,
          "Cause should be AssertionError, but was: " + exception.getCause().getClass());
      assertTrue(
          exception.getCause().getMessage().contains("utility class"),
          "Message should mention utility class");
    }
  }
}

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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.uri;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_URI_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_URL_PATH_BELOW_EXACT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.param.UriParamQualifierEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import java.util.stream.Stream;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for UriParameterQueryGenerator.
 *
 * <p>The UriParameterQueryGenerator is a static utility class for FHIR URI-type search parameters
 * such as url, identifier systems, and other canonical references. URI parameters support both
 * exact matching and hierarchical prefix matching.
 *
 * <p><b>Test Strategy:</b>
 *
 * <ul>
 *   <li>Test standard URI search (exact match for HTTP/HTTPS and other URI schemes)
 *   <li>Test :below qualifier (hierarchical prefix matching for HTTP/HTTPS)
 *   <li>Test :above qualifier (throws NotImplementedException)
 *   <li>Test URI normalization behavior
 *   <li>Test edge cases (empty params, null values, invalid URIs)
 *   <li>Test utility class instantiation prevention
 * </ul>
 */
@DisplayName("UriParameterQueryGenerator Tests")
// TODO: URI/URL normalization tests sind noch nicht vollständig und funktionsfähig

class UriParameterQueryGeneratorTest {

  // ================================================================================
  // Standard Search Tests
  // ================================================================================

  @Nested
  @DisplayName("Standard URI Search")
  class StandardSearchTests {

    @Test
    @DisplayName("Should create TermQuery for HTTP URL")
    void shouldCreateTermQueryForHttpUrl() throws NotImplementedException {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("http://example.com/CodeSystem/test");

      // WHEN
      Query query = UriParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query, "Query should not be null");
      assertTrue(query instanceof TermQuery, "Query should be a TermQuery");

      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          paramName + SEPARATOR + SUFFIX_URI_EXACT,
          termQuery.getTerm().field(),
          "Field name should be url_uriexact");
      assertEquals(
          "http://example.com/CodeSystem/test",
          termQuery.getTerm().text(),
          "Term value should be the normalized URL");
    }

    @Test
    @DisplayName("Should create TermQuery for HTTPS URL")
    void shouldCreateTermQueryForHttpsUrl() throws NotImplementedException {
      // GIVEN
      String paramName = "system";
      UriParam param = new UriParam("https://example.com/CodeSystem/test");

      // WHEN
      Query query = UriParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertTrue(query instanceof TermQuery, "Query should be a TermQuery");

      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          paramName + SEPARATOR + SUFFIX_URI_EXACT,
          termQuery.getTerm().field(),
          "Field name should be system_uriexact");
      assertEquals(
          "https://example.com/CodeSystem/test",
          termQuery.getTerm().text(),
          "Term value should be the URL");
    }

    @Test
    @DisplayName("Should create TermQuery for URN")
    void shouldCreateTermQueryForUrn() throws NotImplementedException {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("urn:oid:2.16.840.1.113883.6.96");

      // WHEN
      Query query = UriParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertTrue(query instanceof TermQuery, "Query should be a TermQuery");

      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          paramName + SEPARATOR + SUFFIX_URI_EXACT,
          termQuery.getTerm().field(),
          "Field name should be url_uriexact");
      assertEquals(
          "urn:oid:2.16.840.1.113883.6.96",
          termQuery.getTerm().text(),
          "Term value should be the URN");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid URI")
    void shouldThrowExceptionForInvalidUri() {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("not a valid uri with spaces");

      // WHEN & THEN
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> UriParameterQueryGenerator.prepareStandardQuery(paramName, param),
              "Should throw IllegalArgumentException for invalid URI");

      assertTrue(
          exception.getMessage().contains("Invalid URI"),
          "Exception message should mention invalid URI");
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
      String paramName = "url";
      UriParam param = new UriParam();
      // Empty param: no value, no missing modifier

      // WHEN
      Query query = UriParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertTrue(
          query instanceof MatchAllDocsQuery, "Empty parameter should return MatchAllDocsQuery");
    }

    @Test
    @DisplayName("Should return MatchAllDocsQuery for null parameter")
    void shouldReturnMatchAllDocsQueryForNullParameter() throws NotImplementedException {
      // GIVEN
      String paramName = "url";
      UriParam param = null;

      // WHEN
      Query query = UriParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertTrue(
          query instanceof MatchAllDocsQuery, "Null parameter should return MatchAllDocsQuery");
    }
  }

  // ================================================================================
  // :below Qualifier Tests
  // ================================================================================

  @Nested
  @DisplayName(":below Qualifier")
  class BelowQualifierTests {

    @Test
    @DisplayName("Should create query for :below with HTTP URL")
    void shouldCreateBelowQueryForHttpUrl() throws NotImplementedException {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("http://hl7.org/fhir");
      param.setQualifier(UriParamQualifierEnum.BELOW);

      // WHEN
      Query query = UriParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertTrue(query instanceof TermQuery, "Query should be a TermQuery");

      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          paramName + SEPARATOR + SUFFIX_URL_PATH_BELOW_EXACT,
          termQuery.getTerm().field(),
          "Field name should be " + paramName + SEPARATOR + SUFFIX_URL_PATH_BELOW_EXACT);
      assertEquals(
          "http://hl7.org/fhir", termQuery.getTerm().text(), "Term value should be the URL");
    }

    @Test
    @DisplayName("Should create query for :below with HTTPS URL")
    void shouldCreateBelowQueryForHttpsUrl() throws NotImplementedException {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("https://terminology.hl7.org/CodeSystem");
      param.setQualifier(UriParamQualifierEnum.BELOW);

      // WHEN
      Query query = UriParameterQueryGenerator.prepareQuery(paramName, param);

      // THEN
      assertNotNull(query);
      assertTrue(query instanceof TermQuery, "Query should be a TermQuery");

      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          paramName + SEPARATOR + SUFFIX_URL_PATH_BELOW_EXACT,
          termQuery.getTerm().field(),
          "Field name should be " + paramName + SEPARATOR + SUFFIX_URL_PATH_BELOW_EXACT);
      assertEquals(
          "https://terminology.hl7.org/CodeSystem",
          termQuery.getTerm().text(),
          "Term value should be the HTTPS URL");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :below with URN")
    void shouldRejectBelowQualifierForUrn() {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("urn:oid:2.16.840.1.113883.6.96");
      param.setQualifier(UriParamQualifierEnum.BELOW);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> UriParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :below qualifier for URN");

      assertTrue(
          exception.getMessage().contains(":below modifier can only be used with http/https-URLs"),
          "Exception should mention HTTP/HTTPS requirement");
    }

    @Test
    @DisplayName("Should throw NotImplementedException for :below with FTP URL")
    void shouldRejectBelowQualifierForFtpUrl() {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("ftp://example.org/resource");
      param.setQualifier(UriParamQualifierEnum.BELOW);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> UriParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :below qualifier for FTP URL");

      assertTrue(
          exception.getMessage().contains(":below modifier can only be used with http/https-URLs"),
          "Exception should mention HTTP/HTTPS requirement");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for :below with invalid URI")
    void shouldThrowExceptionForBelowWithInvalidUri() {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("not a valid uri");
      param.setQualifier(UriParamQualifierEnum.BELOW);

      // WHEN & THEN
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> UriParameterQueryGenerator.prepareQuery(paramName, param),
              "Should throw IllegalArgumentException for invalid URI");

      assertTrue(
          exception.getMessage().contains("Invalid URI"),
          "Exception message should mention invalid URI");
    }
  }

  // ================================================================================
  // :above Qualifier Tests (Not Implemented)
  // ================================================================================

  @Nested
  @DisplayName(":above Qualifier")
  class AboveQualifierTests {

    @Test
    @DisplayName("Should throw NotImplementedException for :above qualifier")
    void shouldRejectAboveQualifier() {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("http://hl7.org/fhir/ValueSet/example");
      param.setQualifier(UriParamQualifierEnum.ABOVE);

      // WHEN & THEN
      NotImplementedException exception =
          assertThrows(
              NotImplementedException.class,
              () -> UriParameterQueryGenerator.prepareQuery(paramName, param),
              "Should reject :above qualifier");

      assertTrue(
          exception.getMessage().contains(":above modifier not implemented"),
          "Exception should mention :above not implemented");
    }
  }

  // ================================================================================
  // Field Name Construction Tests
  // ================================================================================

  @Nested
  @DisplayName("Field Name Construction")
  class FieldNameConstructionTests {

    @Test
    @DisplayName(
        "Should construct field name with SEPARATOR and SUFFIX_URI_EXACT for standard query")
    void shouldConstructCorrectFieldNameForStandardQuery() {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("http://example.org");

      // WHEN
      Query query = UriParameterQueryGenerator.prepareStandardQuery(paramName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      String expectedFieldName = paramName + SEPARATOR + SUFFIX_URI_EXACT; // "url_uriexact"
      assertEquals(
          expectedFieldName,
          termQuery.getTerm().field(),
          "Field name should follow pattern: url_uriexact");
    }

    @Test
    @DisplayName(
        "Should construct field name with SEPARATOR and SUFFIX_URL_PATH_BELOW_EXACT for :below")
    void shouldConstructCorrectFieldNameForBelowQuery() throws NotImplementedException {
      // GIVEN
      String paramName = "url";
      UriParam param = new UriParam("http://example.org");
      param.setQualifier(UriParamQualifierEnum.BELOW);

      // WHEN
      Query query = UriParameterQueryGenerator.prepareBelowModifierQuery(paramName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      String expectedFieldName =
          paramName + SEPARATOR + SUFFIX_URL_PATH_BELOW_EXACT; // "url_urlpathbelowexact"
      assertEquals(
          expectedFieldName,
          termQuery.getTerm().field(),
          "Field name should follow pattern: url_urlpathbelowexact");
    }

    @Test
    @DisplayName("Should work with different parameter names")
    void shouldWorkWithDifferentParameterNames() {
      // GIVEN
      String customParamName = "system";
      UriParam param = new UriParam("http://snomed.info/sct");

      // WHEN
      Query query = UriParameterQueryGenerator.prepareStandardQuery(customParamName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      assertEquals(
          "system_uriexact", termQuery.getTerm().field(), "Should work with any parameter name");
    }
  }

  // ================================================================================
  // URI Scheme Handling Tests
  // ================================================================================

  @Nested
  @DisplayName("URI Scheme Handling")
  class UriSchemeHandlingTests {

    private static Stream<Arguments> uriSchemeCases() {
      return Stream.of(
          Arguments.of("url", "http://example.org/resource", "http://", false),
          Arguments.of("url", "https://example.org/resource", "https://", false),
          Arguments.of("url", "urn:isbn:0451450523", "urn:", false),
          Arguments.of(
              "system", "urn:oid:2.16.840.1.113883.6.96", "urn:oid:2.16.840.1.113883.6.96", true),
          Arguments.of("url", "urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6", "urn:uuid:", false));
    }

    @DisplayName("Should handle URI schemes")
    @ParameterizedTest(name = "param={0}, uri={1}")
    @MethodSource("uriSchemeCases")
    void shouldHandleUriSchemes(
        String paramName,
        String uriValue,
        String expectedPrefixOrExact,
        boolean shouldMatchExactly) {
      // GIVEN
      UriParam param = new UriParam(uriValue);

      // WHEN
      Query query = UriParameterQueryGenerator.prepareStandardQuery(paramName, param);

      // THEN
      TermQuery termQuery = (TermQuery) query;
      if (shouldMatchExactly) {
        assertEquals(
            expectedPrefixOrExact, termQuery.getTerm().text(), "Should preserve full URI value");
      } else {
        assertTrue(
            termQuery.getTerm().text().startsWith(expectedPrefixOrExact),
            "Should preserve URI scheme prefix");
      }
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
      var constructor = UriParameterQueryGenerator.class.getDeclaredConstructor();
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

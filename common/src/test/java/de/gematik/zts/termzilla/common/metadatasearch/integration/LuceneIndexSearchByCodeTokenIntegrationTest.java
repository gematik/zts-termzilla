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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.integration.helper.InMemoryLuceneTestHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.token.TokenParameterQueryGenerator;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Lucene code token-based indexing and searching.
 *
 * <p>These tests verify the complete cycle of code token-based searches in the Lucene index:
 *
 * <ol>
 *   <li>Indexing FHIR resource code fields using {@link
 *       ResourceFieldGeneratorHelper#addCodeTokenParamFieldToDocument}
 *   <li>Generating Lucene queries using {@link TokenParameterQueryGenerator}
 *   <li>Searching the index and retrieving matching documents
 * </ol>
 *
 * <p>Unlike unit tests, these integration tests use real Lucene components (in-memory via {@link
 * InMemoryLuceneTestHelper}) and production code to ensure that indexing and querying work
 * correctly together.
 *
 * <p><b>Test Field:</b> This test class uses the {@code status} field ({@code NAME_STATUS}) to test
 * code token functionality, as it represents a typical use case for FHIR code values (e.g.,
 * "active", "draft", "retired" from publication-status code system).
 *
 * <p><b>Parameter Type:</b> All tests use {@code ParamData.CODE_TOKEN_PARAM} as the parameter query
 * type, which routes to {@link TokenParameterQueryGenerator}.
 *
 * <p><b>Important Note:</b> Currently, there is no specialized {@code
 * CodeTokenParameterQueryGenerator} implementation. These tests use the generic {@link
 * TokenParameterQueryGenerator} which handles all token types including code tokens.
 *
 * <p><b>Test Structure:</b>
 *
 * <ul>
 *   <li><b>Standard Code Search</b> - Tests for basic code search functionality including exact
 *       matches (code-only and system|code), non-matches, case-insensitive matching, and
 *       cross-system searches
 *   <li><b>Code :not Modifier Search</b> - Tests for negation queries that exclude specific codes
 *       (both code-only and system|code formats)
 *   <li><b>Mixed Code Search Scenarios</b> - Tests for edge cases including empty parameters and
 *       special characters in codes (dashes, underscores, dots)
 * </ul>
 *
 * <p><b>Current Test Coverage:</b>
 *
 * <ul>
 *   <li>Exact code match (code-only: {@code status=active})
 *   <li>Exact code match with system ({@code status=http://hl7.org/fhir/publication-status|active})
 *   <li>Non-matching code search (returns 0 results)
 *   <li>Case-insensitive code matching (search with "ACTIVE" finds "active")
 *   <li>System differentiation (same code from different systems, search with specific system)
 *   <li>Cross-system search (code-only search finds all codes across different systems)
 *   <li>Negation with {@code :not} modifier on code-only ({@code status:not=retired})
 *   <li>Negation with {@code :not} modifier on system|code ({@code
 *       status:not=http://hl7.org/fhir/publication-status|active})
 *   <li>Empty code parameter handling (matches all documents)
 *   <li>Special characters in codes (dashes, underscores: {@code code-with-dash_123})
 *   <li>Dots and numbers in codes (SNOMED-style: {@code 123456.789})
 * </ul>
 *
 * <p><b>Known Limitations & Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Case-Insensitivity:</b> All code values are lowercased for indexing and searching. While
 *       this enables flexible matching, it may not align with code system-specific case sensitivity
 *       rules (see TODO comment in test at line ~228). This is a known trade-off in generic FHIR
 *       search implementation.
 * </ul>
 *
 * <p><b>TODO - Additional Test Cases (to be implemented after REST operation support):</b>
 *
 * <ul>
 *   <li>Test multiple codes with OR combination (e.g., {@code ?status=active,draft})
 *   <li>Test multiple codes with AND combination (multiple {@code status} parameters)
 *   <li>Test {@code :missing} modifier for code presence/absence
 *   <li>Test exception handling: verify {@code NotImplementedException} for unsupported modifiers
 *       ({@code :text}, {@code :above}, {@code :below}, {@code :in}, {@code :not-in}, {@code
 *       :of-type})
 *   <li>Test edge cases: very long codes, URL-encoded codes, unicode characters
 *   <li>Test sorting by code values
 *   <li>Test search for system-only (e.g., {@code status=http://hl7.org/fhir/publication-status|})
 * </ul>
 *
 * @see TokenParameterQueryGenerator
 * @see ResourceFieldGeneratorHelper#addCodeTokenParamFieldToDocument
 * @see InMemoryLuceneTestHelper
 */
@DisplayName("Lucene Index-Search Integration Tests for Code Token Parameters")
class LuceneIndexSearchByCodeTokenIntegrationTest {

  private static final String PUBLICATION_STATUS_SYSTEM = "http://hl7.org/fhir/publication-status";
  private static final String CODE_SYSTEM_STATUS_SYSTEM = "http://hl7.org/fhir/codesystem-status";

  // ================================================================================
  // Helper Methods
  // ================================================================================

  /**
   * Prepares and indexes a document with the specified code token.
   *
   * @param helper the in-memory Lucene test helper
   * @param system the code system
   * @param code the code value
   * @throws IOException if indexing fails
   */
  private void prepareDocument(InMemoryLuceneTestHelper helper, String system, String code)
      throws IOException {
    Document doc = new Document();
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(doc, NAME_STATUS, false);
    ResourceFieldGeneratorHelper.addCodeTokenParamFieldToDocument(
        doc, NAME_STATUS, system, code, Field.Store.YES);
    helper.indexDocument(doc);
  }

  /**
   * Creates a search query for a code-only match (no system specified).
   *
   * @param code the code to search for
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createCodeOnlyQuery(String code) throws NotImplementedException {
    TokenParam tokenParam = new TokenParam(code);
    TokenAndListParam statusParam =
        new TokenAndListParam().addAnd(new TokenOrListParam().addOr(tokenParam));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_STATUS)
            .paramQueryType(ParamData.CODE_TOKEN_PARAM)
            .param(statusParam)
            .build();

    return TokenParameterQueryGenerator.prepareQuery(paramData);
  }

  /**
   * Creates a search query for a system|code match.
   *
   * @param system the code system
   * @param code the code value
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createSystemCodeQuery(String system, String code) throws NotImplementedException {
    TokenParam tokenParam = new TokenParam(system, code);
    TokenAndListParam statusParam =
        new TokenAndListParam().addAnd(new TokenOrListParam().addOr(tokenParam));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_STATUS)
            .paramQueryType(ParamData.CODE_TOKEN_PARAM)
            .param(statusParam)
            .build();

    return TokenParameterQueryGenerator.prepareQuery(paramData);
  }

  /**
   * Creates a search query with the :not modifier for code-only.
   *
   * @param code the code to exclude
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createNotCodeQuery(String code) throws NotImplementedException {
    TokenParam notParam = new TokenParam(code);
    notParam.setModifier(TokenParamModifier.NOT);

    TokenAndListParam statusParam =
        new TokenAndListParam().addAnd(new TokenOrListParam().addOr(notParam));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_STATUS)
            .paramQueryType(ParamData.CODE_TOKEN_PARAM)
            .param(statusParam)
            .build();

    return TokenParameterQueryGenerator.prepareQuery(paramData);
  }

  /**
   * Creates a search query with the :not modifier for system|code.
   *
   * @param system the code system
   * @param code the code to exclude
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createNotSystemCodeQuery(String system, String code)
      throws NotImplementedException {
    TokenParam notParam = new TokenParam(system, code);
    notParam.setModifier(TokenParamModifier.NOT);

    TokenAndListParam statusParam =
        new TokenAndListParam().addAnd(new TokenOrListParam().addOr(notParam));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_STATUS)
            .paramQueryType(ParamData.CODE_TOKEN_PARAM)
            .param(statusParam)
            .build();

    return TokenParameterQueryGenerator.prepareQuery(paramData);
  }

  /**
   * Creates a search query with an empty token parameter.
   *
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createEmptyTokenQuery() throws NotImplementedException {
    TokenAndListParam statusParam =
        new TokenAndListParam().addAnd(new TokenOrListParam().addOr(new TokenParam()));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_STATUS)
            .paramQueryType(ParamData.CODE_TOKEN_PARAM)
            .param(statusParam)
            .build();

    return TokenParameterQueryGenerator.prepareQuery(paramData);
  }

  // ================================================================================
  // Standard Code Search Tests
  // ================================================================================

  @Nested
  @DisplayName("Standard Code Search")
  class StandardCodeSearchTests {

    @Test
    @DisplayName("Should find Document by exact code (code-only search)")
    void shouldFindDocumentByExactCodeOnly() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents indexed with different status codes
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "draft");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "retired");

        // WHEN: Search for status=active (code-only, no system)
        Query query = createCodeOnlyQuery("active");
        List<Document> results = helper.search(query);

        // THEN: Exactly one result found (active status)
        assertEquals(1, results.size(), "Should find exactly one Document with status=active");
      }
    }

    @Test
    @DisplayName("Should find Document by exact code with system (system|code search)")
    void shouldFindDocumentByExactCodeWithSystem() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents indexed with different status codes
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "draft");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "retired");

        // WHEN: Search for status=http://hl7.org/fhir/publication-status|active
        Query query = createSystemCodeQuery(PUBLICATION_STATUS_SYSTEM, "active");
        List<Document> results = helper.search(query);

        // THEN: Exactly one result found
        assertEquals(1, results.size(), "Should find exactly one Document");
      }
    }

    @Test
    @DisplayName("Should not find Document with wrong code")
    void shouldNotFindDocumentWithWrongCode() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with different status codes
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "draft");

        // WHEN: Search for non-existent code
        Query query = createCodeOnlyQuery("unknown");
        List<Document> results = helper.search(query);

        // THEN: No results found
        assertEquals(0, results.size(), "Should not find any Documents");
      }
    }

    // TODO: Mhmm, eigentlich kommt es aber auf das dahinterliegende CS an, ob case-sensitive oder
    //  nicht... --> Das ist aber gar nicht richtig abbildbar, wenn man das generisch bauen möchte
    //  (Spezifikationsproblem?!)
    @Test
    @DisplayName("Should perform case-insensitive code matching")
    void shouldPerformCaseInsensitiveMatching() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Document with lowercase code
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");

        // WHEN: Search with uppercase code
        Query query = createCodeOnlyQuery("ACTIVE");
        List<Document> results = helper.search(query);

        // THEN: Should find the document (case-insensitive match)
        assertEquals(1, results.size(), "Should find document with case-insensitive match");
      }
    }

    @Test
    @DisplayName("Should differentiate codes from different systems")
    void shouldDifferentiateCodesFromDifferentSystems()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents with same code but different systems
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");
        prepareDocument(helper, CODE_SYSTEM_STATUS_SYSTEM, "active");

        // WHEN: Search with specific system
        Query query = createSystemCodeQuery(PUBLICATION_STATUS_SYSTEM, "active");
        List<Document> results = helper.search(query);

        // THEN: Should find only the document from the specified system
        assertEquals(1, results.size(), "Should find exactly one Document from specified system");
      }
    }

    @Test
    @DisplayName("Should find all documents when searching by code-only across systems")
    void shouldFindAllDocumentsByCodeOnlyAcrossSystems()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents with same code but different systems
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");
        prepareDocument(helper, CODE_SYSTEM_STATUS_SYSTEM, "active");

        // WHEN: Search with code-only (no system)
        Query query = createCodeOnlyQuery("active");
        List<Document> results = helper.search(query);

        // THEN: Should find both documents
        assertEquals(2, results.size(), "Should find both documents with code 'active'");
      }
    }
  }

  // ================================================================================
  // Code :not Modifier Tests
  // ================================================================================

  @Nested
  @DisplayName("Code :not Modifier Search")
  class NotModifierSearchTests {

    @Test
    @DisplayName("Should support :not modifier on code search")
    void shouldSupportNotModifierOnCodeSearch() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents indexed with different status codes
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "draft");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "retired");

        // WHEN: Search with :not modifier (status:not=retired)
        Query query = createNotCodeQuery("retired");
        List<Document> results = helper.search(query);

        // THEN: Two results found (all except retired)
        assertEquals(2, results.size(), "Should find all documents except retired");

        // Verify that 'retired' is not in the results
        List<String> resultCodes =
            results.stream()
                .map(doc -> List.of(doc.getValues("status_lowercaseexact")))
                .flatMap(List::stream)
                .toList();

        assertTrue(resultCodes.contains("active"), "Should include active");
        assertTrue(
            resultCodes.contains(PUBLICATION_STATUS_SYSTEM.toLowerCase() + "|active"),
            "Should include " + PUBLICATION_STATUS_SYSTEM.toLowerCase() + "|active");
        assertTrue(resultCodes.contains("draft"), "Should include draft");
        assertTrue(
            resultCodes.contains(PUBLICATION_STATUS_SYSTEM.toLowerCase() + "|draft"),
            "Should include " + PUBLICATION_STATUS_SYSTEM.toLowerCase() + "|draft");
        assertFalse(resultCodes.contains("retired"), "Should not include retired");
        assertFalse(
            resultCodes.contains(PUBLICATION_STATUS_SYSTEM.toLowerCase() + "|retired"),
            "Should not include " + PUBLICATION_STATUS_SYSTEM.toLowerCase() + "|retired");
      }
    }

    @Test
    @DisplayName("Should support :not modifier with system|code")
    void shouldSupportNotModifierWithSystemCode() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Documents with same code from different systems
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");
        prepareDocument(helper, CODE_SYSTEM_STATUS_SYSTEM, "active");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "draft");

        // WHEN: Search with :not modifier on specific system|code
        Query query = createNotSystemCodeQuery(PUBLICATION_STATUS_SYSTEM, "active");
        List<Document> results = helper.search(query);

        // THEN: Two results found (all except publication-status|active)
        assertEquals(
            2, results.size(), "Should find all documents except publication-status|active");

        List<String> resultCodes =
            results.stream()
                .map(doc -> List.of(doc.getValues("status_lowercaseexact")))
                .flatMap(List::stream)
                .toList();

        assertTrue(
            resultCodes.contains(CODE_SYSTEM_STATUS_SYSTEM.toLowerCase() + "|active"),
            "Should include codesystem-status|active");
        assertTrue(
            resultCodes.contains(PUBLICATION_STATUS_SYSTEM.toLowerCase() + "|draft"),
            "Should include publication-status|draft");
        assertFalse(
            resultCodes.contains(PUBLICATION_STATUS_SYSTEM.toLowerCase() + "|active"),
            "Should not include publication-status|active");
      }
    }
  }

  // ================================================================================
  // Mixed Code Search Scenarios
  // ================================================================================

  @Nested
  @DisplayName("Mixed Code Search Scenarios")
  class MixedCodeSearchScenarioTests {

    @Test
    @DisplayName("Should handle empty code parameter")
    void shouldHandleEmptyCodeParameter() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents with different codes
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "active");
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "draft");

        // WHEN: Search with empty parameter
        Query query = createEmptyTokenQuery();
        List<Document> results = helper.search(query);

        // THEN: Empty parameter should match all documents
        assertEquals(2, results.size(), "Empty parameter should match all Documents");
      }
    }

    @Test
    @DisplayName("Should handle codes with special characters")
    void shouldHandleCodesWithSpecialCharacters() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Document with special characters in code
        prepareDocument(helper, PUBLICATION_STATUS_SYSTEM, "code-with-dash_123");

        // WHEN: Search for code with special characters
        Query query = createCodeOnlyQuery("code-with-dash_123");
        List<Document> results = helper.search(query);

        // THEN: Should find the document
        assertEquals(1, results.size(), "Should find document with special characters in code");
      }
    }

    @Test
    @DisplayName("Should handle codes with dots and numbers (e.g., SNOMED-style codes)")
    void shouldHandleCodesWithDotsAndNumbers() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Document with code containing numbers and dots
        prepareDocument(helper, "http://snomed.info/sct", "123456.789");

        // WHEN: Search for code with dots and numbers
        Query query = createSystemCodeQuery("http://snomed.info/sct", "123456.789");
        List<Document> results = helper.search(query);

        // THEN: Should find the document
        assertEquals(1, results.size(), "Should find document with dots and numbers in code");
      }
    }
  }
}

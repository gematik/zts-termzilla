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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.param.UriOrListParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.param.UriParamQualifierEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.integration.helper.InMemoryLuceneTestHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.uri.UriParameterQueryGenerator;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Lucene URI parameter-based indexing and searching.
 *
 * <p>These tests verify the complete cycle of URI-based searches in the Lucene index:
 *
 * <ol>
 *   <li>Indexing FHIR resource URLs using {@link ResourceFieldGeneratorHelper}
 *   <li>Generating Lucene queries using {@link UriParameterQueryGenerator}
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
 *   <li><b>Standard URL Search</b> - Tests for basic URL search functionality including exact
 *       matches (HTTP, HTTPS, URN), non-matches, and URL distinction
 *   <li><b>URL :below Modifier Search</b> - Tests for hierarchical URL path searches including
 *       namespace matching and deeper path levels
 *   <li><b>Mixed Search Scenarios</b> - Tests for edge cases including empty parameters and special
 *       characters in URLs
 *   <li><b>URL Component Indexing</b> - Verifies separation of URL path and query fields
 * </ul>
 *
 * <p><b>Current Test Coverage:</b>
 *
 * <ul>
 *   <li>Exact URL match search (HTTP: {@code url=http://example.org/cs/test-1})
 *   <li>Exact URL match search (HTTPS: {@code url=https://example.org/cs/test-1})
 *   <li>URN-based search ({@code url=urn:oid:1.2.3.4.5.6789})
 *   <li>Non-matching URL search (returns 0 results)
 *   <li>URL distinction (similar URLs are properly differentiated)
 *   <li>Hierarchical search with {@code :below} modifier ({@code url:below=http://example.org/})
 *   <li>Deep path {@code :below} search ({@code url:below=http://example.org/cs/A})
 *   <li>HTTPS {@code :below} search ({@code url:below=https://example.org/})
 *   <li>Empty URL parameter handling (matches all documents)
 *   <li>Special characters in URLs (e.g., {@code test-1_ABCäöü})
 *   <li>Canonical URL exact matching
 *   <li>Case normalization of URL hostnames
 *   <li>Separate indexing of URL query and path components
 * </ul>
 *
 * <p><b>TODO - Additional Test Cases (to be implemented after REST operation support):</b>
 *
 * <ul>
 *   <li>Test multiple URLs with OR combination (e.g., {@code
 *       ?url=http://example.org/cs1,http://example.org/cs2})
 *   <li>Test multiple URLs with AND combination (multiple {@code url} parameters)
 *   <li>Test URL normalization edge cases (trailing slashes, case sensitivity, URL encoding)
 *   <li>Test :missing modifier for URLs
 *   <li>Test exception handling: verify proper error messages for malformed URIs
 *   <li>Test performance with large number of indexed CodeSystems
 * </ul>
 *
 * @see UriParameterQueryGenerator
 * @see ResourceFieldGeneratorHelper
 * @see InMemoryLuceneTestHelper
 */
@DisplayName("Lucene Index-Search Integration Tests for URI Parameters")
class LuceneIndexSearchByUriIntegrationTest {

  private static final String HTTP_NAMESPACE = "http://example.org/fhir/CodeSystem/";
  private static final String HTTP_OTHER_NAMESPACE = "http://example.org/other/CodeSystem/";
  private static final String HTTPS_NAMESPACE = "https://example.org/fhir/CodeSystem/";

  // ================================================================================
  // Helper Methods
  // ================================================================================

  /**
   * Prepares and indexes a document with the specified URL.
   *
   * @param helper the in-memory Lucene test helper
   * @param url the URL to index
   * @throws IOException if indexing fails
   */
  private void prepareDocument(InMemoryLuceneTestHelper helper, String url) throws IOException {
    Document doc = new Document();
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(doc, NAME_URL, false);
    ResourceFieldGeneratorHelper.addUriParamFieldToDocument(doc, NAME_URL, url, Field.Store.YES);
    helper.indexDocument(doc);
  }

  /**
   * Creates a search query for an exact URL match.
   *
   * @param url the URL to search for
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createExactUrlQuery(String url) throws NotImplementedException {
    UriAndListParam urlParam =
        new UriAndListParam().addAnd(new UriOrListParam().addOr(new UriParam(url)));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_URL)
            .paramQueryType(ParamData.URI_PARAM)
            .param(urlParam)
            .build();

    return UriParameterQueryGenerator.prepareQuery(paramData);
  }

  /**
   * Creates a search query for a URL with the :below modifier.
   *
   * @param baseUrl the base URL to search below
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createBelowUrlQuery(String baseUrl) throws NotImplementedException {
    UriParam belowParam = new UriParam(baseUrl);
    belowParam.setQualifier(UriParamQualifierEnum.BELOW);

    UriAndListParam urlParam = new UriAndListParam().addAnd(new UriOrListParam().addOr(belowParam));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_URL)
            .paramQueryType(ParamData.URI_PARAM)
            .param(urlParam)
            .build();

    return UriParameterQueryGenerator.prepareQuery(paramData);
  }

  // ================================================================================
  // URI Standard Search Tests
  // ================================================================================

  @Nested
  @DisplayName("Standard URL Search")
  class StandardUrlSearchTests {

    @Test
    @DisplayName("Should find Document by exact HTTP URL")
    void shouldFindDocumentByExactHttpUrl() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with different URLs
        prepareDocument(helper, HTTP_NAMESPACE + "test-1");
        prepareDocument(helper, HTTP_NAMESPACE + "test-2");

        // WHEN: Search for url=...
        Query query = createExactUrlQuery(HTTP_NAMESPACE + "test-1");
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document (test-1)
        assertEquals(1, results.size(), "Should find exactly one Document");
      }
    }

    @Test
    @DisplayName("Should find Document by exact HTTPS URL")
    void shouldFindDocumentByExactHttpsUrl() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with different URLs
        prepareDocument(helper, HTTPS_NAMESPACE + "test-1");
        prepareDocument(helper, HTTPS_NAMESPACE + "test-2");

        // WHEN: Search for url=...
        Query query = createExactUrlQuery(HTTPS_NAMESPACE + "test-1");
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document (test-1)
        assertEquals(1, results.size(), "Should find exactly one Document");
      }
    }

    @Test
    @DisplayName("Should find Document by URN")
    void shouldFindCodeSystemByUrn() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with different URNs
        prepareDocument(helper, "urn:oid:1.2.3.4.5.6789");
        prepareDocument(helper, "urn:oid:1.2.3.4.5.6790");

        // WHEN: Search for url=...
        Query query = createExactUrlQuery("urn:oid:1.2.3.4.5.6789");
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document
        assertEquals(1, results.size(), "Should find exactly one Document");
      }
    }

    @Test
    @DisplayName("Should not find Document with wrong URL")
    void shouldNotFindCodeSystemWithWrongUrl() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with different URLs
        prepareDocument(helper, HTTP_NAMESPACE + "test-1");
        prepareDocument(helper, HTTP_NAMESPACE + "test-2");

        // WHEN: Search for non-existing URL
        Query query = createExactUrlQuery(HTTP_NAMESPACE + "test-3");
        List<Document> results = helper.search(query);

        // THEN: Should not find any document
        assertEquals(0, results.size(), "Should not find Document");
      }
    }

    @Test
    @DisplayName("Should distinguish between similar URLs")
    void shouldDistinguishBetweenSimilarUrls() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents with similar URLs
        prepareDocument(helper, HTTP_NAMESPACE + "test-1");
        prepareDocument(helper, HTTP_NAMESPACE + "test-1/a");

        // WHEN: Search for exact URL
        Query query = createExactUrlQuery(HTTP_NAMESPACE + "test-1");
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document (test-1, not test-1/a)
        assertEquals(1, results.size(), "Should find exactly one Document");
      }
    }

    @Test
    @DisplayName("Should find CodeSystem by canonical URL")
    void shouldFindCodeSystemByCanonicalUrl() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Document indexed with a canonical URL
        String canonicalUrl = "http://snomed.info/sct";
        prepareDocument(helper, canonicalUrl);

        // WHEN: Search for the same canonical URL
        Query query = createExactUrlQuery(canonicalUrl);
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document
        assertEquals(1, results.size(), "Should find CodeSystem by canonical URL");
      }
    }

    @Test
    @DisplayName("Should find Document with normalized URL host")
    void shouldFindNormalizedCanonicalUrl() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Document indexed with an uppercase hostname
        String indexedUrl = "http://EXAMPLE.ORG/CodeSystem/test";
        String searchUrl = "http://example.org/CodeSystem/test";
        prepareDocument(helper, indexedUrl);

        // WHEN: Search for the normalized URL
        Query query = createExactUrlQuery(searchUrl);
        List<Document> results = helper.search(query);

        // THEN: Hostname normalization should allow an exact match
        assertEquals(1, results.size(), "Should find Document despite hostname casing");
      }
    }
  }

  // ================================================================================
  // URI :below Modifier Tests
  // ================================================================================

  @Nested
  @DisplayName("URL :below Modifier Search")
  class BelowModifierSearchTests {

    @Test
    @DisplayName("Should find all Documents under a URL path with :below")
    void shouldFindAllCodeSystemsUnderUrlPath() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Four documents with different URL namespaces
        prepareDocument(helper, HTTP_NAMESPACE + "A/1/" + "test-1");
        prepareDocument(helper, HTTP_NAMESPACE + "A/2/" + "test-2");
        prepareDocument(helper, HTTP_NAMESPACE + "B/1/" + "test-3");
        prepareDocument(helper, HTTP_OTHER_NAMESPACE + "B/2/" + "test-4");

        // WHEN: Search with :below modifier
        Query query = createBelowUrlQuery(HTTP_NAMESPACE);
        List<Document> results = helper.search(query);

        // THEN: Should find all documents under HTTP_NAMESPACE (3 out of 4)
        assertEquals(3, results.size(), "Should find all Documents under " + HTTP_NAMESPACE);

        List<String> resultIds = results.stream().map(doc -> doc.get("url_uriexact")).toList();
        assertTrue(resultIds.contains(HTTP_NAMESPACE + "A/1/" + "test-1"), "Should include test-1");
        assertTrue(resultIds.contains(HTTP_NAMESPACE + "A/2/" + "test-2"), "Should include test-2");
        assertTrue(resultIds.contains(HTTP_NAMESPACE + "B/1/" + "test-3"), "Should include test-3");
        assertFalse(
            resultIds.contains(HTTP_OTHER_NAMESPACE + "B/2/" + "test-4"),
            "Should not include test-4");
      }
    }

    @Test
    @DisplayName("Should find CodeSystems with :below at deeper path level")
    void shouldFindCodeSystemsAtDeeperPathLevel() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Four documents with different URL paths
        prepareDocument(helper, HTTP_NAMESPACE + "A/1/" + "test-1");
        prepareDocument(helper, HTTP_NAMESPACE + "A/2/" + "test-2");
        prepareDocument(helper, HTTP_NAMESPACE + "B/1/" + "test-3");
        prepareDocument(helper, HTTP_OTHER_NAMESPACE + "B/2/" + "test-4");

        // WHEN: Search with :below at deeper path (A only)
        Query query = createBelowUrlQuery(HTTP_NAMESPACE + "A");
        List<Document> results = helper.search(query);

        // THEN: Should find only documents under /A/ path (2 out of 4)
        assertEquals(2, results.size(), "Should find all Documents under " + HTTP_NAMESPACE + "A");

        List<String> resultIds = results.stream().map(doc -> doc.get("url_uriexact")).toList();
        assertTrue(resultIds.contains(HTTP_NAMESPACE + "A/1/" + "test-1"), "Should include test-1");
        assertTrue(resultIds.contains(HTTP_NAMESPACE + "A/2/" + "test-2"), "Should include test-2");
        assertFalse(
            resultIds.contains(HTTP_NAMESPACE + "B/1/" + "test-3"), "Should not include test-3");
        assertFalse(
            resultIds.contains(HTTP_OTHER_NAMESPACE + "B/2/" + "test-4"),
            "Should not include test-4");
      }
    }

    @Test
    @DisplayName("Should work with :below on HTTPS URLs")
    void shouldWorkWithBelowOnHttpsUrls() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Four documents with HTTPS and HTTP URLs
        prepareDocument(helper, HTTPS_NAMESPACE + "A/1/" + "test-1");
        prepareDocument(helper, HTTPS_NAMESPACE + "A/2/" + "test-2");
        prepareDocument(helper, HTTPS_NAMESPACE + "B/1/" + "test-3");
        prepareDocument(helper, HTTP_OTHER_NAMESPACE + "B/2/" + "test-4");

        // WHEN: Search with :below on HTTPS namespace
        Query query = createBelowUrlQuery(HTTPS_NAMESPACE);
        List<Document> results = helper.search(query);

        // THEN: Should find all documents under HTTPS_NAMESPACE (3 out of 4)
        assertEquals(3, results.size(), "Should find all Documents under " + HTTPS_NAMESPACE);

        List<String> resultIds = results.stream().map(doc -> doc.get("url_uriexact")).toList();
        assertTrue(
            resultIds.contains(HTTPS_NAMESPACE + "A/1/" + "test-1"), "Should include test-1");
        assertTrue(
            resultIds.contains(HTTPS_NAMESPACE + "A/2/" + "test-2"), "Should include test-2");
        assertTrue(
            resultIds.contains(HTTPS_NAMESPACE + "B/1/" + "test-3"), "Should include test-3");
        assertFalse(
            resultIds.contains(HTTP_OTHER_NAMESPACE + "B/2/" + "test-4"),
            "Should not include test-4");
      }
    }
  }

  // ================================================================================
  // Mixed Scenarios
  // ================================================================================

  @Nested
  @DisplayName("Mixed Search Scenarios")
  class MixedSearchScenarioTests {

    @Test
    @DisplayName("Should handle empty URL parameter")
    void shouldHandleEmptyUrlParameter() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with URLs
        prepareDocument(helper, HTTP_NAMESPACE + "test-1");
        prepareDocument(helper, HTTP_NAMESPACE + "test-2");

        // WHEN: Search with empty URL parameter
        UriAndListParam urlParam =
            new UriAndListParam().addAnd(new UriOrListParam().addOr(new UriParam()));

        ParamData paramData =
            ParamData.builder()
                .paramName(NAME_URL)
                .paramQueryType(ParamData.URI_PARAM)
                .param(urlParam)
                .build();

        Query query = UriParameterQueryGenerator.prepareQuery(paramData);
        List<Document> results = helper.search(query);

        // THEN: Empty parameter should match all documents
        assertEquals(2, results.size(), "Empty Parameter should match all Documents");
      }
    }

    @Test
    @DisplayName("Should find Document with URL containing special characters")
    void shouldFindDocumentWithSpecialCharactersInUrl()
        throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Document with special characters in URL
        prepareDocument(helper, HTTP_NAMESPACE + "test-1_ABCäöü");

        // WHEN: Search for URL with special characters
        Query query = createExactUrlQuery(HTTP_NAMESPACE + "test-1_ABCäöü");
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document
        assertEquals(1, results.size(), "Should find exactly one Document");
      }
    }
  }

  // ================================================================================
  // URI Component Indexing Tests
  // ================================================================================

  @Nested
  @DisplayName("URL Component Indexing")
  class UrlComponentIndexingTests {

    @Test
    @DisplayName("Should index URL query separately from path")
    void shouldIndexUrlQueryComponent() {
      // GIVEN: URL with path and query parameters
      String url = "https://example.org/CodeSystem/test?version=1";
      Document document = new Document();

      // WHEN: Index the URI
      ResourceFieldGeneratorHelper.addUriParamFieldToDocument(
          document, NAME_URL, url, Field.Store.YES);

      // THEN: Query and path should be indexed in separate fields
      assertEquals(
          "version=1",
          document.get(NAME_URL + "_urlqueryexact"),
          "Should index the query component");
      assertEquals(
          "/CodeSystem/test",
          document.get(NAME_URL + "_urlpathexact"),
          "Should index the path component");
    }
  }
}

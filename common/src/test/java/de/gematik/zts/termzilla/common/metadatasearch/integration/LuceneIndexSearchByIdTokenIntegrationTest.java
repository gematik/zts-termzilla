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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_ID;
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
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.token.IdTokenParameterQueryGenerator;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Lucene ID token-based indexing and searching.
 *
 * <p>These tests verify the complete cycle of ID-based searches in the Lucene index:
 *
 * <ol>
 *   <li>Indexing FHIR resource IDs using {@link ResourceFieldGeneratorHelper}
 *   <li>Generating Lucene queries using {@link IdTokenParameterQueryGenerator}
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
 *   <li><b>ID Search Integration</b> - Tests for basic ID search functionality including exact
 *       matches, non-matches, and {@code :not} modifier
 * </ul>
 *
 * <p><b>Current Test Coverage:</b>
 *
 * <ul>
 *   <li>Exact ID match search ({@code _id=id-123})
 *   <li>Non-matching ID search (returns 0 results)
 *   <li>Negation with {@code :not} modifier ({@code _id:not=id-123})
 * </ul>
 *
 * <p><b>TODO - Additional Test Cases (to be implemented after REST operation support):</b>
 *
 * <ul>
 *   <li>Test multiple IDs with OR combination (e.g., {@code _id=id-123,id-456})
 *   <li>Test multiple IDs with AND combination (multiple {@code _id} parameters)
 *   <li>Test combination of multiple {@code :not} modifiers
 *   <li>Test edge cases: empty IDs, special characters in IDs, very long IDs
 *   <li>Test exception handling: verify {@code NotImplementedException} for unsupported modifiers
 *       ({@code :missing}, {@code :text}, {@code :above}, {@code :below}, {@code :in}, {@code
 *       :not-in}, {@code :of-type})
 * </ul>
 *
 * @see IdTokenParameterQueryGenerator
 * @see ResourceFieldGeneratorHelper
 * @see InMemoryLuceneTestHelper
 */
@DisplayName("Lucene Index-Search Integration Tests")
class LuceneIndexSearchByIdTokenIntegrationTest {

  // ================================================================================
  // Helper Methods
  // ================================================================================

  /**
   * Prepares and indexes a document with the specified ID.
   *
   * @param helper the in-memory Lucene test helper
   * @param id the ID to index
   * @throws IOException if indexing fails
   */
  private void prepareDocument(InMemoryLuceneTestHelper helper, String id) throws IOException {
    Document doc = new Document();
    ResourceFieldGeneratorHelper.addIdTokenParamFieldToDocument(doc, NAME_ID, id, false);
    helper.indexDocument(doc);
  }

  /**
   * Creates a search query for an exact ID match.
   *
   * @param id the ID to search for
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createIdQuery(String id) throws NotImplementedException {
    TokenAndListParam idParam =
        new TokenAndListParam().addAnd(new TokenOrListParam().addOr(new TokenParam(id)));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_ID)
            .paramQueryType(ParamData.ID_TOKEN_PARAM)
            .param(idParam)
            .build();

    return IdTokenParameterQueryGenerator.prepareQuery(paramData);
  }

  /**
   * Creates a search query with the :not modifier for ID.
   *
   * @param id the ID to exclude
   * @return the prepared Lucene query
   * @throws NotImplementedException if the query generator encounters an unsupported feature
   */
  private Query createNotIdQuery(String id) throws NotImplementedException {
    TokenParam notParam = new TokenParam();
    notParam.setValue(id);
    notParam.setModifier(TokenParamModifier.NOT);

    TokenAndListParam idParam =
        new TokenAndListParam().addAnd(new TokenOrListParam().addOr(notParam));

    ParamData paramData =
        ParamData.builder()
            .paramName(NAME_ID)
            .paramQueryType(ParamData.ID_TOKEN_PARAM)
            .param(idParam)
            .build();

    return IdTokenParameterQueryGenerator.prepareQuery(paramData);
  }

  // ================================================================================
  // ID Token Parameter Tests
  // ================================================================================

  @Nested
  @DisplayName("ID Search Integration")
  class IdSearchIntegrationTests {

    @Test
    @DisplayName("Should find Document by exact ID")
    void shouldFindDocumentByExactId() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with different IDs
        prepareDocument(helper, "id-123");
        prepareDocument(helper, "id-321");

        // WHEN: Search for _id=id-123
        Query query = createIdQuery("id-123");
        List<Document> results = helper.search(query);

        // THEN: Should find exactly one document
        assertEquals(1, results.size());
        assertEquals("id-123", results.get(0).get("_id_tokenexact"));
      }
    }

    @Test
    @DisplayName("Should not find Document with wrong ID")
    void shouldNotFindDocumentWithWrongId() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Two documents indexed with different IDs
        prepareDocument(helper, "id-123");
        prepareDocument(helper, "id-321");

        // WHEN: Search for non-existing ID
        Query query = createIdQuery("id-999");
        List<Document> results = helper.search(query);

        // THEN: Should not find any documents
        assertEquals(0, results.size());
      }
    }

    @Test
    @DisplayName("Should support :not modifier on ID search")
    void shouldSupportNotModifierOnIdSearch() throws IOException, NotImplementedException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Three documents indexed with different IDs
        prepareDocument(helper, "id-123");
        prepareDocument(helper, "id-321");
        prepareDocument(helper, "id-789");

        // WHEN: Search with :not modifier (_id:not=id-123)
        Query query = createNotIdQuery("id-123");
        List<Document> results = helper.search(query);

        // THEN: Should find two documents (all except id-123)
        assertEquals(2, results.size());

        // Verify that id-123 is not in the results
        List<String> resultIds = results.stream().map(doc -> doc.get("_id_tokenexact")).toList();
        assertTrue(resultIds.contains("id-321"));
        assertTrue(resultIds.contains("id-789"));
        assertFalse(resultIds.contains("id-123"));
      }
    }
  }
}

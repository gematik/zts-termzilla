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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_PROVISION_MODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import de.gematik.zts.termzilla.common.metadatasearch.helper.FhirSearchHelper;
import de.gematik.zts.termzilla.common.metadatasearch.integration.helper.InMemoryLuceneTestHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for FHIR resource filtering by scope, resource type, and ID pre-filter.
 *
 * <p>These tests verify the complete cycle of FHIR filter query construction and execution:
 *
 * <ol>
 *   <li>Indexing FHIR resources with scope, resource type, and ID fields using {@link
 *       ResourceFieldGeneratorHelper}
 *   <li>Generating Lucene filter queries using {@link FhirSearchHelper}
 *   <li>Searching the index and retrieving matching documents
 * </ol>
 *
 * <p>Unlike unit tests, these integration tests use real Lucene components (in-memory via {@link
 * InMemoryLuceneTestHelper}) and production code to ensure that indexing and filtering work
 * correctly together.
 *
 * <p><b>Test Structure:</b>
 *
 * <ul>
 *   <li><b>Scope Filtering</b> - Tests for tenant/scope-based filtering
 *   <li><b>Resource Type Filtering</b> - Tests for filtering by FHIR resource type
 *   <li><b>ID Pre-Filter</b> - Tests for filtering by specific resource IDs
 *   <li><b>Combined Filtering</b> - Tests for combinations of scope, type, and ID filters
 * </ul>
 *
 * <p><b>Current Test Coverage:</b>
 *
 * <ul>
 *   <li>Scope-only filtering (multi-tenancy isolation)
 *   <li>Scope + resource type filtering
 *   <li>Scope + resource type + single ID filtering
 *   <li>Scope + resource type + multiple ID filtering (OR combination)
 *   <li>Empty ID list handling (should be ignored)
 * </ul>
 *
 * <p><b>Filter Query Structure:</b>
 *
 * <pre>
 * +(scope:tenant1 +resourceType_tokenexact:CodeSystem +(id_tokenexact:cs-1 id_tokenexact:cs-2))
 * </pre>
 *
 * <p><b>TODO - Additional Test Cases:</b>
 *
 * <ul>
 *   <li>Test error handling for invalid scope values
 *   <li>Test performance with large ID pre-filter lists
 *   <li>Test special characters in scope names
 *   <li>Test case sensitivity of resource types
 * </ul>
 *
 * @see FhirSearchHelper
 * @see ResourceFieldGeneratorHelper
 * @see InMemoryLuceneTestHelper
 */
@DisplayName("Lucene Index Filter Integration Tests")
class LuceneIndexFilterByScopeTypeAndIdTest {

  // ================================================================================
  // Helper Methods
  // ================================================================================

  /**
   * Prepares and indexes a document with the specified scope, resource type, and ID.
   *
   * @param helper the in-memory Lucene test helper
   * @param scope the tenant/scope identifier
   * @param resourceType the FHIR resource type (e.g., "CodeSystem", "ValueSet")
   * @param id the resource ID
   * @throws IOException if indexing fails
   */
  private void prepareDocument(
      InMemoryLuceneTestHelper helper, String scope, String resourceType, String id)
      throws IOException {
    Document doc = new Document();

    // Add scope field
    doc.add(new StringField(NAME_SCOPE, scope, Field.Store.YES));

    // add predefined provision mode field
    doc.add(new StringField(NAME_PROVISION_MODE, ProvisionMode.PREDEFINED.name(), Field.Store.YES));

    // Add resource type field
    ResourceFieldGeneratorHelper.addIdTokenParamFieldToDocument(
        doc, NAME_TYPE, resourceType, false);

    // Add ID field
    ResourceFieldGeneratorHelper.addIdTokenParamFieldToDocument(doc, NAME_ID, id, false);

    helper.indexDocument(doc);
  }

  /**
   * Creates a filter query with scope only.
   *
   * @param scope the tenant/scope identifier
   * @return the prepared Lucene filter query
   * @throws InvalidRequestException if the filter parameters are invalid
   */
  private Query createScopeOnlyQuery(String scope) throws InvalidRequestException {
    return FhirSearchHelper.prepareFilterQuery(scope, null, null);
  }

  /**
   * Creates a filter query with scope and resource type.
   *
   * @param scope the tenant/scope identifier
   * @param resourceType the FHIR resource type
   * @return the prepared Lucene filter query
   * @throws InvalidRequestException if the filter parameters are invalid
   */
  private Query createScopeAndTypeQuery(String scope, String resourceType)
      throws InvalidRequestException {
    return FhirSearchHelper.prepareFilterQuery(scope, resourceType, null);
  }

  /**
   * Creates a filter query with scope, resource type, and ID pre-filter.
   *
   * @param scope the tenant/scope identifier
   * @param resourceType the FHIR resource type
   * @param ids list of resource IDs to filter by
   * @return the prepared Lucene filter query
   * @throws InvalidRequestException if the filter parameters are invalid
   */
  private Query createFullFilterQuery(String scope, String resourceType, List<IdType> ids)
      throws InvalidRequestException {
    return FhirSearchHelper.prepareFilterQuery(scope, resourceType, ids);
  }

  // ================================================================================
  // Scope Filtering Tests
  // ================================================================================

  @Nested
  @DisplayName("Scope Filtering Tests")
  class ScopeFilteringTests {

    @Test
    @DisplayName("Should filter documents by scope (tenant isolation)")
    void shouldFilterDocumentsByScope() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Documents in different scopes/tenants
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-2");
        prepareDocument(helper, "bfarm.internal.test-1.0.0", "CodeSystem", "cs-3");
        prepareDocument(helper, "bfarm.internal.test-1.0.0", "ValueSet", "vs-1");

        // WHEN: Filter by scope "fhir"
        Query query = createScopeOnlyQuery("fhir");
        List<Document> results = helper.search(query);

        // THEN: Should find only documents from "fhir" scope
        assertEquals(2, results.size(), "Should find 2 documents from scope 'fhir'");

        List<String> resultIds =
            results.stream().map(doc -> doc.get(FhirSearchHelper.RESOURCE_ID_FIELD)).toList();
        assertTrue(resultIds.contains("cs-1"), "Should contain cs-1");
        assertTrue(resultIds.contains("cs-2"), "Should contain cs-2");
        assertFalse(
            resultIds.contains("cs-3"), "Should not contain cs-3 from 'bfarm.internal.test-1.0.0'");
        assertFalse(
            resultIds.contains("vs-1"), "Should not contain vs-1 from 'bfarm.internal.test-1.0.0'");
      }
    }

    @Test
    @DisplayName("Should return empty results for non-existing scope")
    void shouldReturnEmptyForNonExistingScope() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Documents in tenant1
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "ValueSet", "vs-1");

        // WHEN: Filter by non-existing scope
        Query query = createScopeOnlyQuery("bfarm.internal.unknown-1.0.0");
        List<Document> results = helper.search(query);

        // THEN: Should find no documents
        assertEquals(0, results.size(), "Should find no documents for non-existing scope");
      }
    }

    @Test
    @DisplayName("Should isolate tenants completely")
    void shouldIsolateTenantsCompletely() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Same resource IDs in different tenants
        prepareDocument(helper, "bfarm.internal.test-1.0.0", "CodeSystem", "cs-1");
        prepareDocument(helper, "bfarm.internal.test-2.0.0", "CodeSystem", "cs-2");
        prepareDocument(helper, "bfarm.internal.test-3.0.0", "CodeSystem", "cs-3");

        // WHEN: Filter by bfarm.internal.test-2.0.0
        Query query = createScopeOnlyQuery("bfarm.internal.test-2.0.0");
        List<Document> results = helper.search(query);

        // THEN: Should find only the document from bfarm.internal.test-2.0.0
        assertEquals(
            1, results.size(), "Should find exactly 1 document from 'bfarm.internal.test-2.0.0'");
        assertEquals(
            "cs-2",
            results.getFirst().get(FhirSearchHelper.RESOURCE_ID_FIELD),
            "Should find cs-1 from tenant2");
      }
    }
  }

  // ================================================================================
  // Resource Type Filtering Tests
  // ================================================================================

  @Nested
  @DisplayName("Resource Type Filtering Tests")
  class ResourceTypeFilteringTests {

    @Test
    @DisplayName("Should filter documents by scope and resource type")
    void shouldFilterByScopeAndResourceType() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Different resource types in same scope
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-2");
        prepareDocument(helper, "fhir", "ValueSet", "vs-1");
        prepareDocument(helper, "fhir", "ConceptMap", "cm-1");

        // WHEN: Filter by scope and resource type "CodeSystem"
        Query query = createScopeAndTypeQuery("fhir", "CodeSystem");
        List<Document> results = helper.search(query);

        // THEN: Should find only CodeSystem resources
        assertEquals(2, results.size(), "Should find 2 CodeSystem resources");

        List<String> resultIds =
            results.stream().map(doc -> doc.get(FhirSearchHelper.RESOURCE_ID_FIELD)).toList();
        assertTrue(resultIds.contains("cs-1"), "Should contain cs-1");
        assertTrue(resultIds.contains("cs-2"), "Should contain cs-2");
      }
    }

    @Test
    @DisplayName("Should return empty results for non-existing resource type")
    void shouldReturnEmptyForNonExistingResourceType() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: CodeSystem and ValueSet resources
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "ValueSet", "vs-1");

        // WHEN: Filter by non-existing resource type
        Query query = createScopeAndTypeQuery("fhir", "StructureDefinition");
        List<Document> results = helper.search(query);

        // THEN: Should find no documents
        assertEquals(0, results.size(), "Should find no StructureDefinition resources");
      }
    }

    @Test
    @DisplayName("Should combine scope and type filtering correctly")
    void shouldCombineScopeAndTypeFiltering() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Same resource type across different scopes
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "tenant2", "CodeSystem", "cs-2");
        prepareDocument(helper, "fhir", "ValueSet", "vs-1");

        // WHEN: Filter by fhir and CodeSystem
        Query query = createScopeAndTypeQuery("fhir", "CodeSystem");
        List<Document> results = helper.search(query);

        // THEN: Should find only CodeSystems from tenant1
        assertEquals(1, results.size(), "Should find 1 CodeSystem from fhir");
        assertEquals(
            "cs-1", results.getFirst().get(FhirSearchHelper.RESOURCE_ID_FIELD), "Should find cs-1");
      }
    }
  }

  // ================================================================================
  // ID Pre-Filter Tests
  // ================================================================================

  @Nested
  @DisplayName("ID Pre-Filter Tests")
  class IdPreFilterTests {

    @Test
    @DisplayName("Should filter by single ID")
    void shouldFilterBySingleId() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Multiple CodeSystems
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-2");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-3");

        // WHEN: Filter by scope, type, and single ID
        Query query = createFullFilterQuery("fhir", "CodeSystem", List.of(new IdType("cs-2")));
        List<Document> results = helper.search(query);

        // THEN: Should find only the specified ID
        assertEquals(1, results.size(), "Should find 1 CodeSystem with ID cs-2");
        assertEquals(
            "cs-2", results.getFirst().get(FhirSearchHelper.RESOURCE_ID_FIELD), "Should find cs-2");
      }
    }

    @Test
    @DisplayName("Should filter by multiple IDs (OR combination)")
    void shouldFilterByMultipleIds() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Five CodeSystems
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-2");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-3");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-4");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-5");

        // WHEN: Filter by scope, type, and multiple IDs (cs-2, cs-4)
        Query query =
            createFullFilterQuery(
                "fhir", "CodeSystem", List.of(new IdType("cs-2"), new IdType("cs-4")));
        List<Document> results = helper.search(query);

        // THEN: Should find both specified IDs
        assertEquals(2, results.size(), "Should find 2 CodeSystems (cs-2 and cs-4)");

        List<String> resultIds =
            results.stream().map(doc -> doc.get(FhirSearchHelper.RESOURCE_ID_FIELD)).toList();
        assertTrue(resultIds.contains("cs-2"), "Should contain cs-2");
        assertTrue(resultIds.contains("cs-4"), "Should contain cs-4");
      }
    }

    @Test
    @DisplayName("Should return empty results when ID pre-filter doesn't match")
    void shouldReturnEmptyWhenIdDoesNotMatch() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: CodeSystems with specific IDs
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-2");

        // WHEN: Filter by non-existing ID
        Query query = createFullFilterQuery("fhir", "CodeSystem", List.of(new IdType("cs-999")));
        List<Document> results = helper.search(query);

        // THEN: Should find no documents
        assertEquals(0, results.size(), "Should find no documents with ID cs-999");
      }
    }

    @Test
    @DisplayName("Should ignore empty ID list")
    void shouldIgnoreEmptyIdList() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Multiple CodeSystems
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-2");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-3");

        // WHEN: Filter with empty ID list (should be ignored)
        Query query = createFullFilterQuery("fhir", "CodeSystem", List.of());
        List<Document> results = helper.search(query);

        // THEN: Should find all CodeSystems (empty list ignored)
        assertEquals(3, results.size(), "Should find all 3 CodeSystems (empty ID list ignored)");
      }
    }
  }

  // ================================================================================
  // Combined Filtering Tests
  // ================================================================================

  @Nested
  @DisplayName("Combined Filtering Tests")
  class CombinedFilteringTests {

    @Test
    @DisplayName("Should apply all filters correctly (scope + type + IDs)")
    void shouldApplyAllFiltersCombined() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Complex mix of resources across scopes and types
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "CodeSystem", "cs-2");
        prepareDocument(helper, "fhir", "ValueSet", "vs-1");
        prepareDocument(helper, "otherscope", "CodeSystem", "cs-1"); // Same ID, different scope
        prepareDocument(helper, "otherscope", "CodeSystem", "cs-3");

        // WHEN: Filter by tenant1, CodeSystem, and ID cs-1
        Query query = createFullFilterQuery("fhir", "CodeSystem", List.of(new IdType("cs-1")));
        List<Document> results = helper.search(query);

        // THEN: Should find only cs-1 from tenant1 CodeSystem
        assertEquals(1, results.size(), "Should find exactly 1 document");
        assertEquals(
            "cs-1", results.getFirst().get(FhirSearchHelper.RESOURCE_ID_FIELD), "Should find cs-1");
        assertEquals("fhir", results.getFirst().get(NAME_SCOPE), "Should be from tenant1");
      }
    }

    @Test
    @DisplayName("Should respect all filter conditions independently")
    void shouldRespectAllFilterConditions() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Resources that match individual conditions but not all
        prepareDocument(helper, "fhir", "CodeSystem", "cs-1");
        prepareDocument(helper, "fhir", "ValueSet", "cs-2"); // Wrong type
        prepareDocument(helper, "otherscope", "CodeSystem", "cs-2"); // Wrong scope

        // WHEN: Filter by fhir, CodeSystem, and cs-2
        Query query = createFullFilterQuery("tenant1", "CodeSystem", List.of(new IdType("cs-2")));
        List<Document> results = helper.search(query);

        // THEN: Should find no documents (cs-2 is ValueSet in tenant1, or wrong scope)
        assertEquals(0, results.size(), "Should find no documents (no CodeSystem cs-2 in tenant1)");
      }
    }

    @Test
    @DisplayName("Should handle multiple IDs across multiple resource types")
    void shouldHandleMultipleIdsAcrossTypes() throws IOException, InvalidRequestException {
      try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {

        // GIVEN: Mixed resources with overlapping IDs
        prepareDocument(helper, "fhir", "CodeSystem", "res-1");
        prepareDocument(helper, "fhir", "CodeSystem", "res-2");
        prepareDocument(helper, "fhir", "ValueSet", "res-1"); // Same ID, different type
        prepareDocument(helper, "fhir", "ValueSet", "res-3");

        // WHEN: Filter CodeSystems with IDs res-1 and res-3
        Query query =
            createFullFilterQuery(
                "fhir", "CodeSystem", List.of(new IdType("res-1"), new IdType("res-3")));
        List<Document> results = helper.search(query);

        // THEN: Should find only CodeSystem res-1 (res-3 is ValueSet)
        assertEquals(1, results.size(), "Should find 1 CodeSystem");
        assertEquals(
            "res-1",
            results.getFirst().get(FhirSearchHelper.RESOURCE_ID_FIELD),
            "Should find res-1");
      }
    }
  }
}

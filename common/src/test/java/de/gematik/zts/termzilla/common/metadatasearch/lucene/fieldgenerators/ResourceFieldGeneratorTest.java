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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.NAME_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the abstract {@link ResourceFieldGenerator}.
 *
 * <p>This test class verifies the base functionality shared by ALL FHIR resources:
 *
 * <ul>
 *   <li><b>scope</b> (scope) - TERMzilla-specific extension for multi-tenancy
 *   <li><b>resourceType</b> (_type) - FHIR resource type
 *   <li><b>id</b> (_id) - Resource identifier
 *   <li><b>meta.lastUpdated</b> (_lastUpdated) - Last update timestamp
 *   <li><b>meta.source</b> (_source) - Source URI/provenance
 *   <li><b>meta.tag</b> (_tag) - Resource tags
 *   <li><b>meta.profile</b> (_profile) - Profile declarations
 *   <li><b>meta.security</b> (_security) - Security labels
 * </ul>
 *
 * <p><b>Why separate tests for the abstract class?</b>
 *
 * <p>All concrete field generators (CodeSystemFieldGenerator, ValueSetFieldGenerator,
 * ConceptMapFieldGenerator) inherit this functionality. By testing it centrally here, we avoid test
 * duplication in the concrete implementations.
 *
 * <p><b>Test Strategy:</b> We use a minimal test implementation of the abstract class to verify
 * that all inherited field generation methods work correctly across different resource types.
 *
 * @see ResourceFieldGenerator
 */
@DisplayName("ResourceFieldGenerator Tests")
class ResourceFieldGeneratorTest {

  /**
   * Concrete test implementation of the abstract {@link ResourceFieldGenerator}.
   *
   * <p>We use CodeSystem as the test resource because it's one of the simplest FHIR resources. No
   * additional implementation is needed - we're only testing the parent class methods.
   */
  private static class TestResourceFieldGenerator extends ResourceFieldGenerator<CodeSystem> {
    // No additional implementation needed - testing parent methods only
  }

  private TestResourceFieldGenerator fieldGenerator;

  @BeforeEach
  void setUp() {
    fieldGenerator = new TestResourceFieldGenerator();
  }

  // ========================================================================
  // Helper Methods
  // ========================================================================

  private static final String RESOURCE_ID = "test-resource";
  private static final String RESOURCE_SCOPE_1 = "fhir";
  private static final String RESOURCE_SCOPE_2 = "org.terminologies.test-1.0.0";

  /**
   * Creates a minimal FHIR CodeSystem resource for testing.
   *
   * <p>The resource includes:
   *
   * <ul>
   *   <li>ID: {@value RESOURCE_ID}
   *   <li>Scopes: {@value RESOURCE_SCOPE_1} and {@value RESOURCE_SCOPE_2}
   * </ul>
   *
   * @return a CodeSystem resource with minimal required fields
   */
  private CodeSystem createMinimalResource() {
    CodeSystem cs = new CodeSystem();
    cs.setId(RESOURCE_ID);
    cs.setUserData(NAME_SCOPE, Set.of(RESOURCE_SCOPE_1, RESOURCE_SCOPE_2));
    return cs;
  }

  /**
   * Gets the first value of a field from a Lucene document.
   *
   * @param doc the Lucene document
   * @param fieldName the field name
   * @return the field value, or null if field doesn't exist
   */
  private String getFieldValue(Document doc, String fieldName) {
    return doc.get(fieldName);
  }

  /**
   * Checks if a field exists in a Lucene document.
   *
   * @param doc the Lucene document
   * @param fieldName the field name
   * @return true if the field exists, false otherwise
   */
  private boolean hasField(Document doc, String fieldName) {
    return doc.getField(fieldName) != null;
  }

  /**
   * Gets all values of a multi-valued field from a Lucene document.
   *
   * @param doc the Lucene document
   * @param fieldName the field name
   * @return list of all field values (empty list if field doesn't exist)
   */
  private List<String> getAllFieldValues(Document doc, String fieldName) {
    return doc.getFields().stream()
        .filter(f -> f.name().equals(fieldName))
        .map(f -> f.stringValue())
        .toList();
  }

  // ========================================================================
  // Basic Fields (scope, _type, _id)
  // ========================================================================

  @Nested
  @DisplayName("Basic Fields (scope, _type, _id)")
  class BasicFieldsTests {

    @Test
    @DisplayName("Should generate 'scope' field correctly")
    void shouldGenerateScopeField() {
      // GIVEN: A FHIR resource with multiple scopes
      CodeSystem resource = createMinimalResource();
      Document doc = new Document();

      // WHEN: Fields are generated
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: The scope field should contain all scopes from UserData
      assertTrue(hasField(doc, "scope"), "scope field is missing");
      assertTrue(
          getAllFieldValues(doc, "scope").contains(RESOURCE_SCOPE_1),
          "'scope' should contain scope '" + RESOURCE_SCOPE_1 + "' from resource UserData");
      assertTrue(
          getAllFieldValues(doc, "scope").contains(RESOURCE_SCOPE_2),
          "'scope' should contain scope '" + RESOURCE_SCOPE_2 + "' from resource UserData");
    }

    @Test
    @DisplayName("Should generate '_type' field correctly")
    void shouldGenerateTypeField() {
      // GIVEN: A FHIR resource
      CodeSystem resource = createMinimalResource();
      Document doc = new Document();

      // WHEN: Fields are generated
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: The _type field should contain the FHIR resource type
      assertTrue(hasField(doc, "_type_tokenexact"), "_type field is missing");
      assertEquals(
          "CodeSystem",
          getFieldValue(doc, "_type_tokenexact"),
          "'_type_tokenexact' should contain the FHIR resource type");
    }

    @Test
    @DisplayName("Should generate '_id' field correctly")
    void shouldGenerateIdField() {
      // GIVEN: A FHIR resource with an ID
      CodeSystem resource = createMinimalResource();
      Document doc = new Document();

      // WHEN: Fields are generated
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: The _id field should contain the resource ID
      assertTrue(hasField(doc, "_id_tokenexact"), "_id_tokenexact field is missing");
      assertEquals(
          RESOURCE_ID,
          getFieldValue(doc, "_id_tokenexact"),
          "_id_tokenexact must contain the resource ID");
    }
  }

  // ========================================================================
  // Meta Fields (lastUpdated)
  // ========================================================================

  @Nested
  @DisplayName("Meta Fields (lastUpdated)")
  class MetaFieldsTests {

    @Test
    @DisplayName("Should index '_lastUpdated_missing' field correctly when meta is missing")
    void shouldHandleMissingMetaFields() {
      // GIVEN: Resource WITHOUT Meta
      CodeSystem resource = createMinimalResource();
      Document doc = new Document();

      // WHEN: Fields are generated
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: There should be a :missing field for _lastUpdated with value 'true'
      assertTrue(hasField(doc, "_lastUpdated_missing"), "_lastUpdated_missing field is missing");
      assertEquals(
          true,
          Boolean.valueOf(getFieldValue(doc, "_lastUpdated_missing")),
          "_lastUpdated_missing must be 'true'");
    }

    @Test
    @DisplayName("Should index '_lastUpdated' correctly")
    void shouldIndexLastUpdated() {
      // GIVEN: Resource with Meta containing lastUpdated
      CodeSystem resource = createMinimalResource();
      Meta meta = new Meta();
      meta.setLastUpdated(new Date());
      resource.setMeta(meta);
      Document doc = new Document();

      // WHEN: Fields are generated
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: All lastUpdated fields should be present
      assertTrue(hasField(doc, "_lastUpdated_missing"), "'_lastUpdated_missing' field is missing");
      assertEquals(
          false,
          Boolean.valueOf(getFieldValue(doc, "_lastUpdated_missing")),
          "_lastUpdated_missing must be 'false'");

      assertTrue(
          hasField(doc, "_lastUpdated_date_instant"),
          "'_lastUpdated_date_instant' field is missing");
      assertTrue(
          hasField(doc, "_lastUpdated_date_period_start"),
          "'_lastUpdated_date_period_start' field is missing");
      assertTrue(
          hasField(doc, "_lastUpdated_date_period_end"),
          "'_lastUpdated_date_period_end' field is missing");
    }
  }

  // ========================================================================
  // Meta Fields (source)
  // ========================================================================

  @Nested
  @DisplayName("Meta Fields (source)")
  class MetaSourceTests {

    @Test
    @DisplayName("Should index '_source_missing' field correctly when meta is missing")
    void shouldHandleMissingMetaFields() {
      // GIVEN: Resource WITHOUT Meta
      CodeSystem resource = createMinimalResource();
      Document doc = new Document();

      // WHEN: Fields are generated
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: There should be a :missing field for _source with value 'true'
      assertTrue(hasField(doc, "_source_missing"), "_source_missing field is missing");
      assertEquals(
          true,
          Boolean.valueOf(getFieldValue(doc, "_source_missing")),
          "_source_missing must be 'true'");
    }

    @Test
    @DisplayName("Should index '_source' with all URI component fields correctly")
    void shouldIndexSource() {

      // GIVEN: Resource with Meta containing source URI
      CodeSystem resource = createMinimalResource();
      Meta meta = new Meta();
      meta.setSource(
          "https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/2022.0.0");
      resource.setMeta(meta);
      Document doc = new Document();

      // WHEN: Fields are generated
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: Missing marker should be false
      assertTrue(hasField(doc, "_source_missing"), "'_source_missing' field is missing");
      assertEquals(
          false,
          Boolean.valueOf(getFieldValue(doc, "_source_missing")),
          "_source_missing must be 'false'");

      // URI exact field
      assertTrue(hasField(doc, "_source_uriexact"), "'_source_uriexact' field is missing");
      assertEquals(
          meta.getSource(),
          getFieldValue(doc, "_source_uriexact"),
          "'_source_uriexact' has incorrect value");

      // URI scheme field
      assertTrue(
          hasField(doc, "_source_urischemelowercaseexact"),
          "'_source_urischemelowercaseexact' field is missing");
      assertEquals(
          "https",
          getFieldValue(doc, "_source_urischemelowercaseexact"),
          "'_source_urischemelowercaseexact' has incorrect value");

      // URL host field
      assertTrue(
          hasField(doc, "_source_urlhostlowercaseexact"),
          "'_source_urlhostlowercaseexact' field is missing");
      assertEquals(
          "terminologien.bfarm.de",
          getFieldValue(doc, "_source_urlhostlowercaseexact"),
          "'_source_urlhostlowercaseexact' has incorrect value");

      // URL path exact field
      assertTrue(hasField(doc, "_source_urlpathexact"), "'_source_urlpathexact' field is missing");
      assertEquals(
          "/packages/bfarm.terminologien.icd10gm/2022.0.0",
          getFieldValue(doc, "_source_urlpathexact"),
          "'_source_urlpathexact' has incorrect value");

      // URL path below field (hierarchical path components)
      assertTrue(
          hasField(doc, "_source_urlpathbelowexact"),
          "'_source_urlpathbelowexact' field is missing");
      List<String> pathBelow = getAllFieldValues(doc, "_source_urlpathbelowexact");
      assertTrue(
          pathBelow.contains(
              "https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/"),
          "'_source_urlpathbelowexact' missing path component");
      assertTrue(
          pathBelow.contains("https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm"),
          "'_source_urlpathbelowexact' missing path component");
      assertTrue(
          pathBelow.contains("https://terminologien.bfarm.de/packages/"),
          "'_source_urlpathbelowexact' missing path component");
      assertTrue(
          pathBelow.contains("https://terminologien.bfarm.de/packages"),
          "'_source_urlpathbelowexact' missing path component");
      assertTrue(
          pathBelow.contains("https://terminologien.bfarm.de/"),
          "'_source_urlpathbelowexact' missing path component");
      assertTrue(
          pathBelow.contains("https://terminologien.bfarm.de"),
          "'_source_urlpathbelowexact' missing path component");

      // Sort field
      assertTrue(hasField(doc, "_source_sort"), "'_source_sort' field is missing");
    }
  }

  // ========================================================================
  // PART 4: Meta Fields (tag)
  // ========================================================================

  /**
   * Tests for FHIR Meta.tag field indexing.
   *
   * <p>Tests verify that the {@code _tag} search parameter is correctly indexed. Tags are FHIR
   * Coding elements that provide additional categorization for resources. The field generator must
   * create:
   *
   * <ul>
   *   <li>{@code _tag_missing} - Boolean field indicating absence of tags
   *   <li>{@code _tag_lowercaseexact} - Lowercase token fields for exact tag matching (system|code
   *       format)
   * </ul>
   *
   * <p>Tag indexing follows FHIR token parameter conventions with system|code format and
   * case-insensitive matching.
   */
  @Nested
  @DisplayName("Meta Fields (tag)")
  class MetaTagTests {

    /**
     * Tests that the {@code _tag_missing} field is correctly set to {@code true} when a resource
     * has no Meta or no tags.
     *
     * <p>Expected behavior: Resources without tags should have {@code _tag_missing=true} to support
     * FHIR's :missing modifier in searches.
     */
    @Test
    @DisplayName("'_tag_missing' field is correctly indexed when meta information is absent")
    void shouldHandleMissingMetaFields() {
      // GIVEN: Resource WITHOUT Meta
      CodeSystem resource = createMinimalResource();
      Document doc = new Document();

      // WHEN
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: A :missing field for _tag should exist (with value 'true')
      assertTrue(hasField(doc, "_tag_missing"), "_tag_missing field is missing");
      assertEquals(
          true, Boolean.valueOf(getFieldValue(doc, "_tag_missing")), "_tag_missing must be 'true'");
    }

    /**
     * Tests that a single tag with system and code is correctly indexed.
     *
     * <p>Expected behavior: The tag should be indexed in lowercase format as:
     *
     * <ul>
     *   <li>Full format: {@code system|code}
     *   <li>System-only: {@code system|}
     *   <li>Code-only: {@code code}
     * </ul>
     *
     * <p>This supports FHIR's flexible token search patterns.
     */
    @Test
    @DisplayName("Single tag is correctly indexed")
    void shouldIndexSingleTagWithSystemAndCode() {
      // GIVEN: Resource with a single tag
      CodeSystem resource = createMinimalResource();
      Meta meta = new Meta();
      Coding tag = new Coding();
      tag.setSystem("http://example.org/CodeSystem/testTags");
      tag.setCode("TAG_1");
      tag.setDisplay("first test tag");
      meta.addTag(tag);
      resource.setMeta(meta);
      Document doc = new Document();

      // WHEN
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: The tag should be indexed in three formats (system|code, system|, code)
      assertTrue(hasField(doc, "_tag_missing"), "_tag_missing field is missing");
      assertEquals(
          false,
          Boolean.valueOf(getFieldValue(doc, "_tag_missing")),
          "'_tag_missing' must be 'false'");

      assertTrue(hasField(doc, "_tag_lowercaseexact"), "'_tag_lowercaseexact' field is missing");
      List<String> lowercaseexact = getAllFieldValues(doc, "_tag_lowercaseexact");
      assertTrue(
          lowercaseexact.contains("http://example.org/codesystem/testtags|tag_1"),
          "'_tag_lowercaseexact' does not contain expected value (system|code)");
      assertTrue(
          lowercaseexact.contains("http://example.org/codesystem/testtags|"),
          "'_tag_lowercaseexact' does not contain expected value (system|)");
      assertTrue(
          lowercaseexact.contains("tag_1"),
          "'_tag_lowercaseexact' does not contain expected value (code only)");
    }

    /**
     * Tests that multiple tags are all indexed.
     *
     * <p>Expected behavior: Each tag should be indexed in all three formats (system|code, system|,
     * code), resulting in 6 total field values for 2 tags.
     */
    @Test
    @DisplayName("Multiple tags are all indexed")
    void shouldIndexMultipleTags() {
      // GIVEN: Resource with two tags
      CodeSystem resource = createMinimalResource();
      Meta meta = new Meta();

      Coding tag1 = new Coding("http://system1.org", "code1", "Display 1");
      Coding tag2 = new Coding("http://system2.org", "code2", "Display 2");

      meta.addTag(tag1);
      meta.addTag(tag2);
      resource.setMeta(meta);
      Document doc = new Document();

      // WHEN
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: All tags should be indexed (3 formats × 2 tags = 6 field values)
      List<String> tagValues = getAllFieldValues(doc, "_tag_lowercaseexact");

      assertEquals(6, tagValues.size(), "Both tags should be indexed (6 total field values)");
    }
  }

  // ========================================================================
  // PART 5: Meta Fields (profile)
  // ========================================================================

  /**
   * Tests for FHIR Meta.profile field indexing.
   *
   * <p>Tests verify that the {@code _profile} search parameter is correctly indexed. Profiles are
   * canonical URIs that declare conformance to specific FHIR profiles. The field generator must
   * create:
   *
   * <ul>
   *   <li>{@code _profile_missing} - Boolean field indicating absence of profiles
   *   <li>{@code _profile_uri} - URI fields for profile matching
   * </ul>
   *
   * <p><b>Status:</b> Tests pending implementation.
   */
  @Nested
  @DisplayName("Meta Fields (profile)")
  class MetaProfileTests {
    // TODO: Add tests for Meta.profile field indexing
  }

  // ========================================================================
  // PART 6: Meta Fields (security)
  // ========================================================================

  /**
   * Tests for FHIR Meta.security field indexing.
   *
   * <p>Tests verify that the {@code _security} search parameter is correctly indexed. Security
   * labels are FHIR Coding elements that define security tags for access control and data
   * classification. The field generator must create:
   *
   * <ul>
   *   <li>{@code _security_missing} - Boolean field indicating absence of security labels
   *   <li>{@code _security_lowercaseexact} - Lowercase token fields for exact security label
   *       matching
   * </ul>
   *
   * <p>Security label indexing follows the same token parameter conventions as tags (system|code
   * format).
   *
   * <p><b>Status:</b> Tests pending implementation.
   */
  @Nested
  @DisplayName("Meta Fields (security)")
  class MetaSecurityTests {
    // TODO: Add tests for Meta.security field indexing
  }
}

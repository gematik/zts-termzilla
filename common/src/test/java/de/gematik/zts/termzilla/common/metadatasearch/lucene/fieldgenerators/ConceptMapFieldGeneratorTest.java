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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import java.util.stream.Stream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.hl7.fhir.r4.model.ConceptMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link ConceptMapFieldGenerator}. Tests focus on source/target URI, canonical, and
 * system indexing functionality.
 */
@DisplayName("ConceptMapFieldGenerator Tests")
class ConceptMapFieldGeneratorTest {

  private ConceptMapFieldGenerator fieldGenerator;
  private Document document;
  private static final Field.Store FIELD_STORE = Field.Store.YES;

  // Field name constants matching ResourceFieldGeneratorHelper suffixes
  private static final String SUFFIX_REFERENCE_EXACT = "_referenceexact";
  private static final String SUFFIX_MISSING = "_missing";

  @BeforeEach
  void setUp() {
    // GIVEN
    fieldGenerator = new ConceptMapFieldGenerator();
    document = new Document();
  }

  @Nested
  @DisplayName("Source URI Tests")
  class SourceUriTests {

    @Test
    @DisplayName("Should add missing field when source URI is absent")
    void shouldAddMissingFieldWhenSourceUriAbsent() {
      // GIVEN
      ConceptMap conceptMap = ConceptMapTestDataHelper.createMinimalConceptMap();

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_URI + SUFFIX_MISSING))
          .isNotEmpty()
          .allMatch(field -> "true".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index source URI when present")
    void shouldIndexSourceUriWhenPresent() {
      // GIVEN
      String sourceUri = "http://example.org/fhir/ValueSet/source";
      ConceptMap conceptMap = ConceptMapTestDataHelper.createConceptMapWithSourceUri(sourceUri);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_URI + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> sourceUri.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_URI + SUFFIX_MISSING))
          .allMatch(field -> "false".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index source URI as both URI and canonical parameter")
    void shouldIndexSourceUriAsBothParameters() {
      // GIVEN
      String sourceUri = "http://example.org/fhir/ValueSet/source";
      ConceptMap conceptMap = ConceptMapTestDataHelper.createConceptMapWithSourceUri(sourceUri);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_URI + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> sourceUri.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> sourceUri.equals(field.stringValue()));
    }
  }

  @Nested
  @DisplayName("Source Canonical Tests")
  class SourceCanonicalTests {

    @Test
    @DisplayName("Should add missing field when source canonical is absent")
    void shouldAddMissingFieldWhenSourceCanonicalAbsent() {
      // GIVEN
      ConceptMap conceptMap = ConceptMapTestDataHelper.createMinimalConceptMap();

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_MISSING))
          .isNotEmpty()
          .allMatch(field -> "true".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index source canonical without version")
    void shouldIndexSourceCanonicalWithoutVersion() {
      // GIVEN
      String sourceCanonical = "http://example.org/fhir/ValueSet/source";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithSourceCanonical(sourceCanonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> sourceCanonical.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_MISSING))
          .allMatch(field -> "false".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index source canonical with version and URL separately")
    void shouldIndexSourceCanonicalWithVersionAndUrlSeparately() {
      // GIVEN
      String sourceUrl = "http://example.org/fhir/ValueSet/source";
      String version = "1.0.0";
      String sourceCanonical = sourceUrl + "|" + version;
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithSourceCanonical(sourceCanonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      IndexableField[] fields =
          document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_REFERENCE_EXACT);
      assertThat(fields)
          .hasSizeGreaterThanOrEqualTo(2)
          .extracting(IndexableField::stringValue)
          .contains(sourceCanonical, sourceUrl);
    }

    @Test
    @DisplayName("Should index source canonical as both canonical and URI parameter")
    void shouldIndexSourceCanonicalAsBothParameters() {
      // GIVEN
      String sourceCanonical = "http://example.org/fhir/ValueSet/source";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithSourceCanonical(sourceCanonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> sourceCanonical.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_URI + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> sourceCanonical.equals(field.stringValue()));
    }

    @ParameterizedTest
    @MethodSource("provideSourceCanonicalWithVersions")
    @DisplayName("Should correctly handle source canonical with different version formats")
    void shouldHandleSourceCanonicalWithDifferentVersions(
        String canonical, String expectedUrl, String expectedVersion) {
      // GIVEN
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithSourceCanonical(canonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      IndexableField[] fields =
          document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_REFERENCE_EXACT);
      assertThat(fields).extracting(IndexableField::stringValue).contains(canonical);

      if (expectedVersion != null) {
        assertThat(fields).extracting(IndexableField::stringValue).contains(expectedUrl);
      }
    }

    static Stream<Arguments> provideSourceCanonicalWithVersions() {
      return Stream.of(
          Arguments.of("http://example.org/vs|1.0.0", "http://example.org/vs", "1.0.0"),
          Arguments.of("http://example.org/vs|2.5.3", "http://example.org/vs", "2.5.3"),
          Arguments.of("http://example.org/vs", "http://example.org/vs", null));
    }
  }

  @Nested
  @DisplayName("Source System Tests")
  class SourceSystemTests {

    @Test
    @DisplayName("Should add missing field when no source system exists")
    void shouldAddMissingFieldWhenNoSourceSystemExists() {
      // GIVEN
      ConceptMap conceptMap = ConceptMapTestDataHelper.createMinimalConceptMap();

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_SYSTEM + SUFFIX_MISSING))
          .isNotEmpty()
          .allMatch(field -> "true".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index source system from group without version")
    void shouldIndexSourceSystemFromGroupWithoutVersion() {
      // GIVEN
      String sourceSystem = "http://example.org/fhir/CodeSystem/source";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithGroupSource(sourceSystem, null);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_SYSTEM + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> sourceSystem.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_SYSTEM + SUFFIX_MISSING))
          .allMatch(field -> "false".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index source system from group with version")
    void shouldIndexSourceSystemFromGroupWithVersion() {
      // GIVEN
      String sourceSystem = "http://example.org/fhir/CodeSystem/source";
      String version = "2.0.0";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithGroupSource(sourceSystem, version);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      IndexableField[] fields =
          document.getFields(CmSearchParameter.SP_SOURCE_SYSTEM + SUFFIX_REFERENCE_EXACT);
      assertThat(fields)
          .hasSizeGreaterThanOrEqualTo(2)
          .extracting(IndexableField::stringValue)
          .contains(sourceSystem, sourceSystem + "|" + version);
    }

    @Test
    @DisplayName("Should index multiple source systems from different groups")
    void shouldIndexMultipleSourceSystemsFromDifferentGroups() {
      // GIVEN
      String sourceSystem1 = "http://example.org/cs1";
      String sourceSystem2 = "http://example.org/cs2";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithMultipleGroupSources(
              sourceSystem1, null, sourceSystem2, "1.0");

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      IndexableField[] fields =
          document.getFields(CmSearchParameter.SP_SOURCE_SYSTEM + SUFFIX_REFERENCE_EXACT);
      assertThat(fields)
          .hasSizeGreaterThanOrEqualTo(3)
          .extracting(IndexableField::stringValue)
          .contains(sourceSystem1, sourceSystem2, sourceSystem2 + "|1.0");
    }

    @Test
    @DisplayName("Should deduplicate identical source systems from multiple groups")
    void shouldDeduplicateIdenticalSourceSystems() {
      // GIVEN
      String sourceSystem = "http://example.org/cs";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithMultipleGroupSources(
              sourceSystem, null,
              sourceSystem, null);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      IndexableField[] fields =
          document.getFields(CmSearchParameter.SP_SOURCE_SYSTEM + SUFFIX_REFERENCE_EXACT);
      assertThat(fields)
          .extracting(IndexableField::stringValue)
          .filteredOn(sourceSystem::equals)
          .hasSize(1);
    }
  }

  @Nested
  @DisplayName("Target URI Tests")
  class TargetUriTests {

    @Test
    @DisplayName("Should add missing field when target URI is absent")
    void shouldAddMissingFieldWhenTargetUriAbsent() {
      // GIVEN
      ConceptMap conceptMap = ConceptMapTestDataHelper.createMinimalConceptMap();

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_URI + SUFFIX_MISSING))
          .isNotEmpty()
          .allMatch(field -> "true".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index target URI when present")
    void shouldIndexTargetUriWhenPresent() {
      // GIVEN
      String targetUri = "http://example.org/fhir/ValueSet/target";
      ConceptMap conceptMap = ConceptMapTestDataHelper.createConceptMapWithTargetUri(targetUri);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_URI + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> targetUri.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_URI + SUFFIX_MISSING))
          .allMatch(field -> "false".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index target URI as both URI and canonical parameter")
    void shouldIndexTargetUriAsBothParameters() {
      // GIVEN
      String targetUri = "http://example.org/fhir/ValueSet/target";
      ConceptMap conceptMap = ConceptMapTestDataHelper.createConceptMapWithTargetUri(targetUri);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_URI + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> targetUri.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> targetUri.equals(field.stringValue()));
    }
  }

  @Nested
  @DisplayName("Target Canonical Tests")
  class TargetCanonicalTests {

    @Test
    @DisplayName("Should add missing field when target canonical is absent")
    void shouldAddMissingFieldWhenTargetCanonicalAbsent() {
      // GIVEN
      ConceptMap conceptMap = ConceptMapTestDataHelper.createMinimalConceptMap();

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_MISSING))
          .isNotEmpty()
          .allMatch(field -> "true".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index target canonical without version")
    void shouldIndexTargetCanonicalWithoutVersion() {
      // GIVEN
      String targetCanonical = "http://example.org/fhir/ValueSet/target";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithTargetCanonical(targetCanonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> targetCanonical.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_MISSING))
          .allMatch(field -> "false".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index target canonical with version and URL separately")
    void shouldIndexTargetCanonicalWithVersionAndUrlSeparately() {
      // GIVEN
      String targetUrl = "http://example.org/fhir/ValueSet/target";
      String version = "3.0.0";
      String targetCanonical = targetUrl + "|" + version;
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithTargetCanonical(targetCanonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      IndexableField[] fields =
          document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_REFERENCE_EXACT);
      assertThat(fields)
          .hasSizeGreaterThanOrEqualTo(2)
          .extracting(IndexableField::stringValue)
          .contains(targetCanonical, targetUrl);
    }

    @Test
    @DisplayName("Should index target canonical as both canonical and URI parameter")
    void shouldIndexTargetCanonicalAsBothParameters() {
      // GIVEN
      String targetCanonical = "http://example.org/fhir/ValueSet/target";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithTargetCanonical(targetCanonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> targetCanonical.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_URI + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> targetCanonical.equals(field.stringValue()));
    }
  }

  @Nested
  @DisplayName("Target System Tests")
  class TargetSystemTests {

    @Test
    @DisplayName("Should add missing field when no target system exists")
    void shouldAddMissingFieldWhenNoTargetSystemExists() {
      // GIVEN
      ConceptMap conceptMap = ConceptMapTestDataHelper.createMinimalConceptMap();

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_SYSTEM + SUFFIX_MISSING))
          .isNotEmpty()
          .allMatch(field -> "true".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index target system from group without version")
    void shouldIndexTargetSystemFromGroupWithoutVersion() {
      // GIVEN
      String targetSystem = "http://example.org/fhir/CodeSystem/target";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithGroupTarget(targetSystem, null);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_SYSTEM + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty()
          .anyMatch(field -> targetSystem.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_SYSTEM + SUFFIX_MISSING))
          .allMatch(field -> "false".equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should index target system from group with version")
    void shouldIndexTargetSystemFromGroupWithVersion() {
      // GIVEN
      String targetSystem = "http://example.org/fhir/CodeSystem/target";
      String version = "4.0.1";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithGroupTarget(targetSystem, version);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      IndexableField[] fields =
          document.getFields(CmSearchParameter.SP_TARGET_SYSTEM + SUFFIX_REFERENCE_EXACT);
      assertThat(fields)
          .hasSizeGreaterThanOrEqualTo(2)
          .extracting(IndexableField::stringValue)
          .contains(targetSystem, targetSystem + "|" + version);
    }

    @Test
    @DisplayName("Should index multiple target systems from different groups")
    void shouldIndexMultipleTargetSystemsFromDifferentGroups() {
      // GIVEN
      String targetSystem1 = "http://example.org/cs1";
      String targetSystem2 = "http://example.org/cs2";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithMultipleGroupTargets(
              targetSystem1, null, targetSystem2, "2.0");

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      IndexableField[] fields =
          document.getFields(CmSearchParameter.SP_TARGET_SYSTEM + SUFFIX_REFERENCE_EXACT);
      assertThat(fields)
          .hasSizeGreaterThanOrEqualTo(3)
          .extracting(IndexableField::stringValue)
          .contains(targetSystem1, targetSystem2, targetSystem2 + "|2.0");
    }
  }

  @Nested
  @DisplayName("Combined Source and Target Tests")
  class CombinedSourceAndTargetTests {

    @Test
    @DisplayName("Should handle ConceptMap with both source and target URI")
    void shouldHandleConceptMapWithBothSourceAndTargetUri() {
      // GIVEN
      String sourceUri = "http://example.org/source";
      String targetUri = "http://example.org/target";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithSourceAndTargetUri(sourceUri, targetUri);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_URI + SUFFIX_REFERENCE_EXACT))
          .anyMatch(field -> sourceUri.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_URI + SUFFIX_REFERENCE_EXACT))
          .anyMatch(field -> targetUri.equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should handle ConceptMap with both source and target canonical")
    void shouldHandleConceptMapWithBothSourceAndTargetCanonical() {
      // GIVEN
      String sourceCanonical = "http://example.org/source|1.0";
      String targetCanonical = "http://example.org/target|2.0";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithSourceAndTargetCanonical(
              sourceCanonical, targetCanonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .anyMatch(field -> sourceCanonical.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .anyMatch(field -> targetCanonical.equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should handle ConceptMap with source URI and target canonical")
    void shouldHandleConceptMapWithMixedSourceAndTarget() {
      // GIVEN
      String sourceUri = "http://example.org/source";
      String targetCanonical = "http://example.org/target|1.0";
      ConceptMap conceptMap =
          ConceptMapTestDataHelper.createConceptMapWithSourceUriAndTargetCanonical(
              sourceUri, targetCanonical);

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_URI + SUFFIX_REFERENCE_EXACT))
          .anyMatch(field -> sourceUri.equals(field.stringValue()));
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .anyMatch(field -> targetCanonical.equals(field.stringValue()));
    }

    @Test
    @DisplayName("Should handle ConceptMap with all source and target types")
    void shouldHandleConceptMapWithAllSourceAndTargetTypes() {
      // GIVEN
      ConceptMap conceptMap = ConceptMapTestDataHelper.createComplexConceptMap();

      // WHEN
      fieldGenerator.createFields(document, conceptMap, FIELD_STORE);

      // THEN
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_URI + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty();
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty();
      assertThat(document.getFields(CmSearchParameter.SP_SOURCE_SYSTEM + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty();
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_URI + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty();
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_CANONICAL + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty();
      assertThat(document.getFields(CmSearchParameter.SP_TARGET_SYSTEM + SUFFIX_REFERENCE_EXACT))
          .isNotEmpty();
    }
  }
}

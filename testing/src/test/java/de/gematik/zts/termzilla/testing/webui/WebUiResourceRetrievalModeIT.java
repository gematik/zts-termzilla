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
package de.gematik.zts.termzilla.testing.webui;

import static de.gematik.zts.termzilla.testing.data.ResourceRetrievalModeTestDataGenerator.TEST_CODESYSTEM_URL;
import static de.gematik.zts.termzilla.testing.data.ResourceRetrievalModeTestDataGenerator.TEST_CONCEPTMAP_URL;
import static de.gematik.zts.termzilla.testing.data.ResourceRetrievalModeTestDataGenerator.TEST_VALUESET_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloadException;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testing.data.ResourceRetrievalModeTestDataGenerator;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Integration tests for resource retrieval modes (FULL vs STRIPPED).
 *
 * <p>Validates that the server correctly returns either complete resources or stripped versions
 * based on the CORE_RESOURCERETRIEVALMODE environment variable setting.
 */
@Slf4j
@DisplayName("Resource Retrieval Mode Integration Tests")
class WebUiResourceRetrievalModeIT {

  private static final String TEST_SCOPE = "fhir";

  @Nested
  @DisplayName("FULL Mode Tests")
  class FullModeTests extends BaseFhirIT {

    @Override
    protected void configureEnvironment(EnvironmentVariables env) {
      env.set("CORE_RESOURCERETRIEVALMODE", "FULL");
    }

    @Override
    protected void copyData(Path packageLoadTempDir) throws PackageDownloadException {
      try {
        ResourceRetrievalModeTestDataGenerator.generateTestPackage(packageLoadTempDir);
      } catch (PackageGenerationException e) {
        throw new PackageDownloadException("Failed to generate test package", e);
      }
    }

    @Test
    @DisplayName("Should return complete CodeSystem with concepts in FULL mode")
    void shouldReturnFullCodeSystem() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      CodeSystem codeSystem = (CodeSystem) searchResult.getEntryFirstRep().getResource();
      assertThat(codeSystem).isNotNull();
      assertThat(codeSystem.getUrl()).isEqualTo(TEST_CODESYSTEM_URL);
      assertThat(codeSystem.getConcept())
          .as("CodeSystem should contain concepts in FULL mode")
          .isNotNull()
          .isNotEmpty()
          .hasSizeGreaterThanOrEqualTo(2);
      assertThat(codeSystem.getContent())
          .as("Content mode should indicate complete content")
          .isEqualTo(CodeSystem.CodeSystemContentMode.COMPLETE);

      log.info("Retrieved CodeSystem with {} root concepts", codeSystem.getConcept().size());
    }

    @Test
    @DisplayName("Should return complete ValueSet with compose and expansion in FULL mode")
    void shouldReturnFullValueSet() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      ValueSet valueSet = (ValueSet) searchResult.getEntryFirstRep().getResource();
      assertThat(valueSet).isNotNull();
      assertThat(valueSet.getUrl()).isEqualTo(TEST_VALUESET_URL);
      assertThat(valueSet.hasCompose()).as("ValueSet should contain compose in FULL mode").isTrue();
      assertThat(valueSet.getCompose().getInclude())
          .as("Compose should have include elements")
          .isNotEmpty();

      if (valueSet.hasExpansion()) {
        assertThat(valueSet.getExpansion().getContains())
            .as("Expansion should have contains elements")
            .isNotEmpty();
        log.info(
            "Retrieved ValueSet with expansion containing {} concepts",
            valueSet.getExpansion().getContains().size());
      }

      log.info(
          "Retrieved ValueSet with compose containing {} include elements",
          valueSet.getCompose().getInclude().size());
    }

    @Test
    @DisplayName("Should return complete ConceptMap with groups in FULL mode")
    void shouldReturnFullConceptMap() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ConceptMap.class)
              .where(ConceptMap.URL.matches().value(TEST_CONCEPTMAP_URL))
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      ConceptMap conceptMap = (ConceptMap) searchResult.getEntryFirstRep().getResource();
      assertThat(conceptMap).isNotNull();
      assertThat(conceptMap.getUrl()).isEqualTo(TEST_CONCEPTMAP_URL);
      assertThat(conceptMap.getGroup())
          .as("ConceptMap should contain groups in FULL mode")
          .isNotNull()
          .isNotEmpty()
          .hasSizeGreaterThanOrEqualTo(2);

      log.info("Retrieved ConceptMap with {} groups", conceptMap.getGroup().size());
    }

    // Add this to the FullModeTests class

    @Test
    @DisplayName("Should allow all elements including restricted ones in FULL mode")
    void shouldAllowRestrictedElementsInFullMode() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN - CodeSystem with 'concept'
      Bundle codeSystemResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "version", "concept")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(codeSystemResult.getEntry()).as("Search should return results").isNotEmpty();
      CodeSystem codeSystem = (CodeSystem) codeSystemResult.getEntryFirstRep().getResource();
      assertThat(codeSystem.getConcept())
          .as("Concepts should be included when requested in FULL mode")
          .isNotNull()
          .isNotEmpty();

      // WHEN - ValueSet with 'compose' and 'expansion'
      Bundle valueSetResult =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .elementsSubset("url", "version", "compose", "expansion")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(valueSetResult.getEntry()).as("Search should return results").isNotEmpty();
      ValueSet valueSet = (ValueSet) valueSetResult.getEntryFirstRep().getResource();
      assertThat(valueSet.hasCompose())
          .as("Compose should be included when requested in FULL mode")
          .isTrue();

      // WHEN - ConceptMap with 'group'
      Bundle conceptMapResult =
          client
              .search()
              .forResource(ConceptMap.class)
              .where(ConceptMap.URL.matches().value(TEST_CONCEPTMAP_URL))
              .elementsSubset("url", "version", "group")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(conceptMapResult.getEntry()).as("Search should return results").isNotEmpty();
      ConceptMap conceptMap = (ConceptMap) conceptMapResult.getEntryFirstRep().getResource();
      assertThat(conceptMap.getGroup())
          .as("Groups should be included when requested in FULL mode")
          .isNotNull()
          .isNotEmpty();

      log.info("All restricted elements are allowed in FULL mode");
    }
  }

  @Nested
  @DisplayName("STRIPPED Mode Tests")
  class StrippedModeTests extends BaseFhirIT {

    @Override
    protected void configureEnvironment(EnvironmentVariables env) {
      env.set("CORE_RESOURCERETRIEVALMODE", "STRIPPED");
    }

    @Override
    protected void copyData(Path packageLoadTempDir) throws PackageDownloadException {
      try {
        ResourceRetrievalModeTestDataGenerator.generateTestPackage(packageLoadTempDir);
      } catch (PackageGenerationException e) {
        throw new PackageDownloadException("Failed to generate test package", e);
      }
    }

    @Test
    @DisplayName(
        "Should return CodeSystem without concepts in STRIPPED mode with _summary parameter")
    void shouldReturnStrippedCodeSystem() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      CodeSystem codeSystem = (CodeSystem) searchResult.getEntryFirstRep().getResource();
      assertThat(codeSystem).isNotNull();
      assertThat(codeSystem.getUrl()).isEqualTo(TEST_CODESYSTEM_URL);
      assertThat(codeSystem.getConcept())
          .as("CodeSystem should not contain concepts in STRIPPED mode")
          .isNullOrEmpty();

      // Metadata should still be present
      assertThat(codeSystem.getName()).isEqualTo("RetrievalTestCodeSystem");
      assertThat(codeSystem.getStatus())
          .isEqualTo(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
      assertThat(codeSystem.getContent()).isNotNull();

      log.info("Retrieved stripped CodeSystem without concepts");
    }

    @Test
    @DisplayName(
        "Should return ValueSet without compose and expansion in STRIPPED mode with _summary parameter")
    void shouldReturnStrippedValueSet() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      ValueSet valueSet = (ValueSet) searchResult.getEntryFirstRep().getResource();
      assertThat(valueSet).isNotNull();
      assertThat(valueSet.getUrl()).isEqualTo(TEST_VALUESET_URL);
      assertThat(valueSet.hasCompose())
          .as("ValueSet should not contain compose in STRIPPED mode")
          .isFalse();
      assertThat(valueSet.hasExpansion())
          .as("ValueSet should not contain expansion in STRIPPED mode")
          .isFalse();

      // Metadata should still be present
      assertThat(valueSet.getName()).isEqualTo("RetrievalTestValueSet");
      assertThat(valueSet.getStatus())
          .isEqualTo(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);

      log.info("Retrieved stripped ValueSet without compose and expansion");
    }

    @Test
    @DisplayName("Should return ConceptMap without groups in STRIPPED mode with _summary parameter")
    void shouldReturnStrippedConceptMap() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ConceptMap.class)
              .where(ConceptMap.URL.matches().value(TEST_CONCEPTMAP_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      ConceptMap conceptMap = (ConceptMap) searchResult.getEntryFirstRep().getResource();
      assertThat(conceptMap).isNotNull();
      assertThat(conceptMap.getUrl()).isEqualTo(TEST_CONCEPTMAP_URL);
      assertThat(conceptMap.getGroup())
          .as("ConceptMap should not contain groups in STRIPPED mode")
          .isNullOrEmpty();

      // Metadata should still be present
      assertThat(conceptMap.getName()).isEqualTo("RetrievalTestConceptMap");
      assertThat(conceptMap.getStatus())
          .isEqualTo(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
      assertThat(conceptMap.hasSourceUriType()).isTrue();
      assertThat(conceptMap.hasTargetUriType()).isTrue();

      log.info("Retrieved stripped ConceptMap without groups");
    }

    @Test
    @DisplayName("Should reject search without _summary or _elements in STRIPPED mode")
    void shouldRejectSearchWithoutSummaryOrElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Search request without _elements or _summary parameter")
          .hasMessageContaining("resource retrieval mode is STRIPPED");

      log.info("Search correctly rejected without _summary or _elements parameter");
    }

    @Test
    @DisplayName(
        "Should return complete CodeSystem with concepts when using read operation in STRIPPED mode")
    void shouldReturnCompleteCodeSystemOnRead() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // First, get the ID via search
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      String resourceId = searchResult.getEntryFirstRep().getResource().getIdElement().getIdPart();
      assertThat(resourceId).as("Resource ID should be present").isNotBlank();

      // WHEN - Read the complete resource using the ID
      CodeSystem codeSystem = client.read().resource(CodeSystem.class).withId(resourceId).execute();

      // THEN - The read operation should return the complete resource with concepts
      assertThat(codeSystem).isNotNull();
      assertThat(codeSystem.getUrl()).isEqualTo(TEST_CODESYSTEM_URL);
      assertThat(codeSystem.getConcept())
          .as("CodeSystem read operation should return concepts even in STRIPPED mode")
          .isNotNull()
          .isNotEmpty()
          .hasSizeGreaterThanOrEqualTo(2);
      assertThat(codeSystem.getContent())
          .as("Content mode should indicate complete content")
          .isEqualTo(CodeSystem.CodeSystemContentMode.COMPLETE);

      log.info(
          "Read operation returned complete CodeSystem with {} root concepts in STRIPPED mode",
          codeSystem.getConcept().size());
    }

    @Test
    @DisplayName(
        "Should return complete ValueSet with compose when using read operation in STRIPPED mode")
    void shouldReturnCompleteValueSetOnRead() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // First, get the ID via search
      Bundle searchResult =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      String resourceId = searchResult.getEntryFirstRep().getResource().getIdElement().getIdPart();
      assertThat(resourceId).as("Resource ID should be present").isNotBlank();

      // WHEN - Read the complete resource using the ID
      ValueSet valueSet = client.read().resource(ValueSet.class).withId(resourceId).execute();

      // THEN - The read operation should return the complete resource with compose
      assertThat(valueSet).isNotNull();
      assertThat(valueSet.getUrl()).isEqualTo(TEST_VALUESET_URL);
      assertThat(valueSet.hasCompose())
          .as("ValueSet read operation should return compose even in STRIPPED mode")
          .isTrue();
      assertThat(valueSet.getCompose().getInclude())
          .as("Compose should have include elements")
          .isNotEmpty();

      log.info(
          "Read operation returned complete ValueSet with compose containing {} include elements in STRIPPED mode",
          valueSet.getCompose().getInclude().size());
    }

    @Test
    @DisplayName(
        "Should return complete ConceptMap with groups when using read operation in STRIPPED mode")
    void shouldReturnCompleteConceptMapOnRead() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // First, get the ID via search
      Bundle searchResult =
          client
              .search()
              .forResource(ConceptMap.class)
              .where(ConceptMap.URL.matches().value(TEST_CONCEPTMAP_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      String resourceId = searchResult.getEntryFirstRep().getResource().getIdElement().getIdPart();
      assertThat(resourceId).as("Resource ID should be present").isNotBlank();

      // WHEN - Read the complete resource using the ID
      ConceptMap conceptMap = client.read().resource(ConceptMap.class).withId(resourceId).execute();

      // THEN - The read operation should return the complete resource with groups
      assertThat(conceptMap).isNotNull();
      assertThat(conceptMap.getUrl()).isEqualTo(TEST_CONCEPTMAP_URL);
      assertThat(conceptMap.getGroup())
          .as("ConceptMap read operation should return groups even in STRIPPED mode")
          .isNotNull()
          .isNotEmpty()
          .hasSizeGreaterThanOrEqualTo(2);

      log.info(
          "Read operation returned complete ConceptMap with {} groups in STRIPPED mode",
          conceptMap.getGroup().size());
    }

    @Test
    @DisplayName("Should demonstrate search-then-read workflow in STRIPPED mode")
    void shouldDemonstrateSearchThenReadWorkflow() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN - Step 1: Search with _summary to get stripped results
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      CodeSystem strippedCodeSystem = (CodeSystem) searchResult.getEntryFirstRep().getResource();
      String resourceId = strippedCodeSystem.getIdElement().getIdPart();

      // THEN - Step 1 verification: Stripped version has no concepts
      assertThat(strippedCodeSystem.getConcept())
          .as("Search result should be stripped")
          .isNullOrEmpty();

      // WHEN - Step 2: Read the full resource using the ID from search
      CodeSystem fullCodeSystem =
          client.read().resource(CodeSystem.class).withId(resourceId).execute();

      // THEN - Step 2 verification: Full version has concepts
      assertThat(fullCodeSystem.getConcept())
          .as("Read result should be complete")
          .isNotNull()
          .isNotEmpty()
          .hasSizeGreaterThanOrEqualTo(2);

      log.info(
          "Workflow verified: Search returned stripped version, Read returned complete version with {} concepts",
          fullCodeSystem.getConcept().size());
    }
  }

  @Nested
  @DisplayName("Metadata Preservation Tests")
  class MetadataPreservationTests extends BaseFhirIT {

    @Override
    protected void configureEnvironment(EnvironmentVariables env) {
      env.set("CORE_RESOURCERETRIEVALMODE", "STRIPPED");
    }

    @Override
    protected void copyData(Path packageLoadTempDir) throws PackageDownloadException {
      try {
        ResourceRetrievalModeTestDataGenerator.generateTestPackage(packageLoadTempDir);
      } catch (PackageGenerationException e) {
        throw new PackageDownloadException("Failed to generate test package", e);
      }
    }

    @Test
    @DisplayName("Should preserve all metadata fields in stripped CodeSystem")
    void shouldPreserveCodeSystemMetadata() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      CodeSystem codeSystem = (CodeSystem) searchResult.getEntryFirstRep().getResource();

      assertThat(codeSystem.getUrl()).isEqualTo(TEST_CODESYSTEM_URL);
      assertThat(codeSystem.getVersion()).isEqualTo("1.0.0");
      assertThat(codeSystem.getName()).isEqualTo("RetrievalTestCodeSystem");
      assertThat(codeSystem.getTitle()).isEqualTo("Resource Retrieval Test CodeSystem");
      assertThat(codeSystem.getStatus())
          .isEqualTo(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
      assertThat(codeSystem.getContent()).isNotNull();
      assertThat(codeSystem.hasProperty()).as("Properties should be preserved").isTrue();
    }

    @Test
    @DisplayName("Should preserve all metadata fields in stripped ValueSet")
    void shouldPreserveValueSetMetadata() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      ValueSet valueSet = (ValueSet) searchResult.getEntryFirstRep().getResource();

      assertThat(valueSet.getUrl()).isEqualTo(TEST_VALUESET_URL);
      assertThat(valueSet.getVersion()).isEqualTo("1.0.0");
      assertThat(valueSet.getName()).isEqualTo("RetrievalTestValueSet");
      assertThat(valueSet.getTitle()).isEqualTo("Resource Retrieval Test ValueSet");
      assertThat(valueSet.getStatus())
          .isEqualTo(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should preserve all metadata fields in stripped ConceptMap")
    void shouldPreserveConceptMapMetadata() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ConceptMap.class)
              .where(ConceptMap.URL.matches().value(TEST_CONCEPTMAP_URL))
              .summaryMode(ca.uhn.fhir.rest.api.SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      ConceptMap conceptMap = (ConceptMap) searchResult.getEntryFirstRep().getResource();

      assertThat(conceptMap.getUrl()).isEqualTo(TEST_CONCEPTMAP_URL);
      assertThat(conceptMap.getVersion()).isEqualTo("1.0.0");
      assertThat(conceptMap.getName()).isEqualTo("RetrievalTestConceptMap");
      assertThat(conceptMap.getTitle()).isEqualTo("Resource Retrieval Test ConceptMap");
      assertThat(conceptMap.getStatus())
          .isEqualTo(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
      assertThat(conceptMap.getSourceUriType()).isNotNull();
      assertThat(conceptMap.getTargetUriType()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Elements Parameter Tests")
  class ElementsParameterTests extends BaseFhirIT {

    @Override
    protected void configureEnvironment(EnvironmentVariables env) {
      env.set("CORE_RESOURCERETRIEVALMODE", "STRIPPED");
    }

    @Override
    protected void copyData(Path packageLoadTempDir) throws PackageDownloadException {
      try {
        ResourceRetrievalModeTestDataGenerator.generateTestPackage(packageLoadTempDir);
      } catch (PackageGenerationException e) {
        throw new PackageDownloadException("Failed to generate test package", e);
      }
    }

    @Test
    @DisplayName("Should return CodeSystem with specific elements using _elements parameter")
    void shouldReturnCodeSystemWithSpecificElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "version", "name", "status")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      CodeSystem codeSystem = (CodeSystem) searchResult.getEntryFirstRep().getResource();
      assertThat(codeSystem).isNotNull();
      assertThat(codeSystem.getUrl()).isEqualTo(TEST_CODESYSTEM_URL);
      assertThat(codeSystem.getVersion()).isEqualTo("1.0.0");
      assertThat(codeSystem.getName()).isEqualTo("RetrievalTestCodeSystem");
      assertThat(codeSystem.getStatus()).isNotNull();

      log.info("Retrieved CodeSystem with specific elements using _elements parameter");
    }

    @Test
    @DisplayName("Should return ValueSet with specific elements using _elements parameter")
    void shouldReturnValueSetWithSpecificElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .elementsSubset("url", "version", "name", "status")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      ValueSet valueSet = (ValueSet) searchResult.getEntryFirstRep().getResource();
      assertThat(valueSet).isNotNull();
      assertThat(valueSet.getUrl()).isEqualTo(TEST_VALUESET_URL);
      assertThat(valueSet.getVersion()).isEqualTo("1.0.0");
      assertThat(valueSet.getName()).isEqualTo("RetrievalTestValueSet");
      assertThat(valueSet.getStatus()).isNotNull();

      log.info("Retrieved ValueSet with specific elements using _elements parameter");
    }

    @Test
    @DisplayName("Should return ConceptMap with specific elements using _elements parameter")
    void shouldReturnConceptMapWithSpecificElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ConceptMap.class)
              .where(ConceptMap.URL.matches().value(TEST_CONCEPTMAP_URL))
              .elementsSubset("url", "version", "name", "status")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      ConceptMap conceptMap = (ConceptMap) searchResult.getEntryFirstRep().getResource();
      assertThat(conceptMap).isNotNull();
      assertThat(conceptMap.getUrl()).isEqualTo(TEST_CONCEPTMAP_URL);
      assertThat(conceptMap.getVersion()).isEqualTo("1.0.0");
      assertThat(conceptMap.getName()).isEqualTo("RetrievalTestConceptMap");
      assertThat(conceptMap.getStatus()).isNotNull();

      log.info("Retrieved ConceptMap with specific elements using _elements parameter");
    }
  }

  @Nested
  @DisplayName("Elements Parameter Restriction Tests")
  class ElementsParameterRestrictionTests extends BaseFhirIT {

    @Override
    protected void configureEnvironment(EnvironmentVariables env) {
      env.set("CORE_RESOURCERETRIEVALMODE", "STRIPPED");
    }

    @Override
    protected void copyData(Path packageLoadTempDir) throws PackageDownloadException {
      try {
        ResourceRetrievalModeTestDataGenerator.generateTestPackage(packageLoadTempDir);
      } catch (PackageGenerationException e) {
        throw new PackageDownloadException("Failed to generate test package", e);
      }
    }

    @Test
    @DisplayName("Should reject CodeSystem search with 'concept' in _elements parameter")
    void shouldRejectCodeSystemSearchWithConceptElement() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "version", "concept")
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Element 'concept' is not allowed in _elements parameter")
          .hasMessageContaining("CodeSystem");

      log.info("Search correctly rejected with restricted element 'concept' for CodeSystem");
    }

    @Test
    @DisplayName("Should reject CodeSystem search with only 'concept' in _elements parameter")
    void shouldRejectCodeSystemSearchWithOnlyConceptElement() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("concept")
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Element 'concept' is not allowed in _elements parameter")
          .hasMessageContaining("CodeSystem");

      log.info("Search correctly rejected with only restricted element 'concept' for CodeSystem");
    }

    @Test
    @DisplayName("Should reject ValueSet search with 'compose' in _elements parameter")
    void shouldRejectValueSetSearchWithComposeElement() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .elementsSubset("url", "version", "compose")
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Element 'compose' is not allowed in _elements parameter")
          .hasMessageContaining("ValueSet");

      log.info("Search correctly rejected with restricted element 'compose' for ValueSet");
    }

    @Test
    @DisplayName("Should reject ValueSet search with 'expansion' in _elements parameter")
    void shouldRejectValueSetSearchWithExpansionElement() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .elementsSubset("url", "version", "expansion")
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Element 'expansion' is not allowed in _elements parameter")
          .hasMessageContaining("ValueSet");

      log.info("Search correctly rejected with restricted element 'expansion' for ValueSet");
    }

    @Test
    @DisplayName(
        "Should reject ValueSet search with both 'compose' and 'expansion' in _elements parameter")
    void shouldRejectValueSetSearchWithComposeAndExpansionElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .elementsSubset("url", "compose", "expansion")
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("is not allowed in _elements parameter")
          .hasMessageContaining("ValueSet");

      log.info(
          "Search correctly rejected with both restricted elements 'compose' and 'expansion' for ValueSet");
    }

    @Test
    @DisplayName("Should reject ConceptMap search with 'group' in _elements parameter")
    void shouldRejectConceptMapSearchWithGroupElement() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(ConceptMap.class)
              .where(ConceptMap.URL.matches().value(TEST_CONCEPTMAP_URL))
              .elementsSubset("url", "version", "group")
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Element 'group' is not allowed in _elements parameter")
          .hasMessageContaining("ConceptMap");

      log.info("Search correctly rejected with restricted element 'group' for ConceptMap");
    }

    @Test
    @DisplayName("Should allow CodeSystem search with allowed elements in _elements parameter")
    void shouldAllowCodeSystemSearchWithAllowedElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "version", "name", "status", "content")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      CodeSystem codeSystem = (CodeSystem) searchResult.getEntryFirstRep().getResource();
      assertThat(codeSystem).isNotNull();
      assertThat(codeSystem.getUrl()).isEqualTo(TEST_CODESYSTEM_URL);
      assertThat(codeSystem.getConcept())
          .as("Concepts should not be included when not requested")
          .isNullOrEmpty();

      log.info("Search succeeded with allowed elements for CodeSystem");
    }

    @Test
    @DisplayName("Should allow ValueSet search with allowed elements in _elements parameter")
    void shouldAllowValueSetSearchWithAllowedElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .elementsSubset("url", "version", "name", "status")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      ValueSet valueSet = (ValueSet) searchResult.getEntryFirstRep().getResource();
      assertThat(valueSet).isNotNull();
      assertThat(valueSet.getUrl()).isEqualTo(TEST_VALUESET_URL);
      assertThat(valueSet.hasCompose())
          .as("Compose should not be included when not requested")
          .isFalse();
      assertThat(valueSet.hasExpansion())
          .as("Expansion should not be included when not requested")
          .isFalse();

      log.info("Search succeeded with allowed elements for ValueSet");
    }

    @Test
    @DisplayName("Should allow ConceptMap search with allowed elements in _elements parameter")
    void shouldAllowConceptMapSearchWithAllowedElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(ConceptMap.class)
              .where(ConceptMap.URL.matches().value(TEST_CONCEPTMAP_URL))
              .elementsSubset("url", "version", "name", "status")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      ConceptMap conceptMap = (ConceptMap) searchResult.getEntryFirstRep().getResource();
      assertThat(conceptMap).isNotNull();
      assertThat(conceptMap.getUrl()).isEqualTo(TEST_CONCEPTMAP_URL);
      assertThat(conceptMap.getGroup())
          .as("Groups should not be included when not requested")
          .isNullOrEmpty();

      log.info("Search succeeded with allowed elements for ConceptMap");
    }

    @Test
    @DisplayName("Should validate element names are case-sensitive")
    void shouldValidateElementNamesAreCaseSensitive() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN - Search with lowercase 'concept' should be rejected
      var searchRequestLowercase =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "concept")
              .returnBundle(Bundle.class);

      // THEN
      assertThatThrownBy(searchRequestLowercase::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Element 'concept' is not allowed in _elements parameter");

      // WHEN - Search with uppercase 'Concept' should succeed (different element name)
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "Concept")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      log.info("Element name validation is case-sensitive - 'concept' rejected, 'Concept' allowed");
    }

    @Test
    @DisplayName("Should reject CodeSystem search with 'concept' mixed with other valid elements")
    void shouldRejectCodeSystemSearchWithConceptMixedWithValidElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "version", "name", "status", "concept", "content")
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Element 'concept' is not allowed in _elements parameter")
          .hasMessageContaining("CodeSystem");

      log.info("Search correctly rejected with 'concept' mixed among valid elements");
    }

    @Test
    @DisplayName("Should reject search with empty _elements parameter in STRIPPED mode")
    void shouldRejectSearchWithEmptyElementsParameter() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset()
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Search request without _elements or _summary parameter")
          .hasMessageContaining("resource retrieval mode is STRIPPED");

      log.info("Search correctly rejected with empty _elements parameter in STRIPPED mode");
    }

    @Test
    @DisplayName("Should handle whitespace-only element names gracefully")
    void shouldHandleWhitespaceOnlyElementNames() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "version", "   ", "name")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      CodeSystem codeSystem = (CodeSystem) searchResult.getEntryFirstRep().getResource();
      assertThat(codeSystem).isNotNull();
      assertThat(codeSystem.getUrl()).isEqualTo(TEST_CODESYSTEM_URL);

      log.info("Search succeeded - whitespace-only element names are filtered out");
    }

    @Test
    @DisplayName("Should allow search with single valid element in _elements parameter")
    void shouldAllowSearchWithSingleValidElement() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN
      Bundle searchResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url")
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      assertThat(searchResult.getEntry()).as("Search should return results").isNotEmpty();

      CodeSystem codeSystem = (CodeSystem) searchResult.getEntryFirstRep().getResource();
      assertThat(codeSystem).isNotNull();
      assertThat(codeSystem.getUrl()).isEqualTo(TEST_CODESYSTEM_URL);

      log.info("Search succeeded with single valid element in _elements parameter");
    }

    @Test
    @DisplayName(
        "Should reject CodeSystem search with concept when other elements are empty strings")
    void shouldRejectCodeSystemSearchWithConceptAndEmptyElements() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      var searchRequest =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("", "concept", "")
              .returnBundle(Bundle.class);

      // WHEN & THEN
      assertThatThrownBy(searchRequest::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Element 'concept' is not allowed in _elements parameter")
          .hasMessageContaining("CodeSystem");

      log.info("Search correctly rejected with 'concept' even when mixed with empty strings");
    }

    @Test
    @DisplayName("Should validate restrictions work independently for different resource types")
    void shouldValidateRestrictionsWorkIndependently() {
      // GIVEN
      IGenericClient client = getFhirClient(TEST_SCOPE);

      // WHEN & THEN - 'group' should be allowed for CodeSystem
      Bundle codeSystemResult =
          client
              .search()
              .forResource(CodeSystem.class)
              .where(CodeSystem.URL.matches().value(TEST_CODESYSTEM_URL))
              .elementsSubset("url", "group")
              .returnBundle(Bundle.class)
              .execute();

      assertThat(codeSystemResult.getEntry())
          .as("Search should return CodeSystem results")
          .isNotEmpty();

      // WHEN & THEN - 'concept' should be allowed for ValueSet
      Bundle valueSetResult =
          client
              .search()
              .forResource(ValueSet.class)
              .where(ValueSet.URL.matches().value(TEST_VALUESET_URL))
              .elementsSubset("url", "concept")
              .returnBundle(Bundle.class)
              .execute();

      assertThat(valueSetResult.getEntry())
          .as("Search should return ValueSet results")
          .isNotEmpty();

      log.info("Restrictions are correctly applied per resource type");
    }
  }
}

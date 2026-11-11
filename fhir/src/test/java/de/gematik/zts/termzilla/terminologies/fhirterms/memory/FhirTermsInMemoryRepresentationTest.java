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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.termzilla.testutils.packages.TestPackageHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link FhirTermsInMemoryRepresentation} retrieval behavior. */
class FhirTermsInMemoryRepresentationTest {

  private Path loadPath;
  private Path indexPath;
  private Path unzipPath;
  private FhirTermsInMemoryRepresentation representation;

  @BeforeEach
  void setUp() throws IOException, InvalidConfigurationException {
    loadPath = Files.createTempDirectory("fhir-terms");
    indexPath = Files.createTempDirectory("lucene");
    unzipPath = Files.createTempDirectory("unzip");

    TestPackageHelper.copyTestPackage(
        FhirTermsInMemoryRepresentationTestDataHelper.TEST_PACKAGE_FILE, loadPath);

    FhirConfigProperties properties =
        FhirTermsInMemoryRepresentationTestDataHelper.createProperties(
            loadPath, indexPath, unzipPath);

    representation =
        new FhirTermsInMemoryRepresentation(
            properties, FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE);
  }

  @AfterEach
  void tearDown() throws IOException {
    TestPackageHelper.deletePackageDirectory(unzipPath);
    TestPackageHelper.deletePackageDirectory(indexPath);
    TestPackageHelper.deletePackageDirectory(loadPath);
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should initialize model and expose loaded scopes")
    void shouldInitializeModelAndExposeLoadedScopes() {
      // GIVEN
      String defaultScope = FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE;
      String packageScope = FhirTermsInMemoryRepresentationTestDataHelper.PACKAGE_SCOPE;

      // WHEN
      boolean containsDefaultScope = representation.getScopeMap().containsKey(defaultScope);
      boolean containsPackageScope = representation.getScopeMap().containsKey(packageScope);

      // THEN
      assertThat(containsDefaultScope).isTrue();
      assertThat(containsPackageScope).isTrue();
      assertThat(representation.getReleaseDataHash()).isNotBlank();
    }
  }

  @Nested
  @DisplayName("getResourceListByScope")
  class GetResourceListByScopeTests {

    @Test
    @DisplayName("should return resources for a known scope")
    void shouldReturnResourcesForKnownScope() throws UnknownScopeException {
      // GIVEN
      String scope = FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE;

      // WHEN
      List<CodeSystem> resources =
          representation.getResourceListByScope(
              CodeSystem.class, scope, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(resources).isNotEmpty();
      assertThat(resources)
          .extracting(MetadataResource::getUrl)
          .contains("https://terminologien.bfarm.de/fhir/CodeSystem/test");
    }

    @Test
    @DisplayName("should return stripped resources when retrieval mode is stripped")
    void shouldReturnStrippedResourcesWhenModeIsStripped() throws UnknownScopeException {
      // GIVEN
      String scope = FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE;
      List<CodeSystem> fullResources =
          representation.getResourceListByScope(
              CodeSystem.class, scope, ResourceRetrievalMode.FULL);
      CodeSystem fullResource = fullResources.getFirst();

      // WHEN
      List<CodeSystem> strippedResources =
          representation.getResourceListByScope(
              CodeSystem.class, scope, ResourceRetrievalMode.STRIPPED);
      CodeSystem strippedResource = strippedResources.getFirst();

      // THEN
      assertThat(strippedResources).hasSameSizeAs(fullResources);
      assertThat(strippedResource)
          .isNotSameAs(fullResource)
          .extracting(MetadataResource::getIdPart)
          .isEqualTo(fullResource.getIdPart());
    }

    @Test
    @DisplayName("should throw for unknown scope")
    void shouldThrowForUnknownScope() {
      // GIVEN
      String unknownScope = "unknown-scope";

      // WHEN THEN
      assertThatThrownBy(
              () ->
                  representation.getResourceListByScope(
                      CodeSystem.class, unknownScope, ResourceRetrievalMode.FULL))
          .isInstanceOf(UnknownScopeException.class)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("should reject null parameters")
    void shouldRejectNullParameters() {
      // GIVEN
      String scope = FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE;

      // WHEN THEN
      assertThatThrownBy(
              () -> representation.getResourceListByScope(null, scope, ResourceRetrievalMode.FULL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Resource class must not be null");

      assertThatThrownBy(
              () ->
                  representation.getResourceListByScope(
                      CodeSystem.class, null, ResourceRetrievalMode.FULL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Scope must not be null");

      assertThatThrownBy(() -> representation.getResourceListByScope(CodeSystem.class, scope, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Resource retrieval mode must not be null");
    }
  }

  @Nested
  @DisplayName("getResourceById")
  class GetResourceByIdTests {

    @Test
    @DisplayName("should return null for unknown id")
    void shouldReturnNullForUnknownId() throws UnsupportedTerminologyException {
      // GIVEN
      String unknownId = "missing-id";

      // WHEN
      CodeSystem resource =
          representation.getResourceById(CodeSystem.class, unknownId, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(resource).isNull();
    }

    @Test
    @DisplayName("should return resource by id for matching type")
    void shouldReturnResourceByIdForMatchingType() throws UnsupportedTerminologyException {
      // GIVEN
      List<CodeSystem> knownResources =
          representation.getResourceList(CodeSystem.class, ResourceRetrievalMode.FULL);
      CodeSystem expected = knownResources.getFirst();
      String id = expected.getIdPart();

      // WHEN
      CodeSystem actual =
          representation.getResourceById(CodeSystem.class, id, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(actual).isNotNull().isSameAs(expected);
    }

    @Test
    @DisplayName("should throw when id belongs to a different type")
    void shouldThrowWhenIdBelongsToDifferentType() {
      // GIVEN
      List<CodeSystem> knownResources =
          representation.getResourceList(CodeSystem.class, ResourceRetrievalMode.FULL);
      String codeSystemId = knownResources.getFirst().getIdPart();

      // WHEN THEN
      assertThatThrownBy(
              () ->
                  representation.getResourceById(
                      ValueSet.class, codeSystemId, ResourceRetrievalMode.FULL))
          .isInstanceOf(UnsupportedTerminologyException.class);
    }

    @Test
    @DisplayName("should return stripped resource when retrieval mode is stripped")
    void shouldReturnStrippedResourceWhenModeIsStripped() throws UnsupportedTerminologyException {
      // GIVEN
      List<CodeSystem> knownResources =
          representation.getResourceList(CodeSystem.class, ResourceRetrievalMode.FULL);
      CodeSystem fullResource = knownResources.getFirst();

      // WHEN
      CodeSystem strippedResource =
          representation.getResourceById(
              CodeSystem.class, fullResource.getIdPart(), ResourceRetrievalMode.STRIPPED);

      // THEN
      assertThat(strippedResource)
          .isNotNull()
          .isNotSameAs(fullResource)
          .extracting(MetadataResource::getIdPart)
          .isEqualTo(fullResource.getIdPart());
    }

    @Test
    @DisplayName("should reject null parameters")
    void shouldRejectNullParameters() {
      // GIVEN
      List<CodeSystem> knownResources =
          representation.getResourceList(CodeSystem.class, ResourceRetrievalMode.FULL);
      String id = knownResources.getFirst().getIdPart();

      // WHEN THEN
      assertThatThrownBy(() -> representation.getResourceById(null, id, ResourceRetrievalMode.FULL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Resource class must not be null");

      assertThatThrownBy(
              () ->
                  representation.getResourceById(
                      CodeSystem.class, null, ResourceRetrievalMode.FULL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ID must not be null");

      assertThatThrownBy(() -> representation.getResourceById(CodeSystem.class, id, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Resource retrieval mode must not be null");
    }
  }

  @Nested
  @DisplayName("getResourceListByUrlAndScope")
  class GetResourceListByUrlAndScopeTests {

    @Test
    @DisplayName("should return matching resources for url within a known scope")
    void shouldReturnMatchingResourcesForUrlWithinKnownScope() throws UnknownScopeException {
      // GIVEN
      String scope = FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE;
      List<CodeSystem> knownResources =
          representation.getResourceListByScope(
              CodeSystem.class, scope, ResourceRetrievalMode.FULL);
      String url = knownResources.getFirst().getUrl();

      // WHEN
      List<CodeSystem> resources =
          representation.getResourceListByUrlAndScope(
              CodeSystem.class, url, scope, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(resources).isNotEmpty().allMatch(resource -> resource.getUrl().equals(url));
    }

    @Test
    @DisplayName("should return stripped resources when retrieval mode is stripped")
    void shouldReturnStrippedResourcesWhenModeIsStripped() throws UnknownScopeException {
      // GIVEN
      String scope = FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE;
      List<CodeSystem> fullResources =
          representation.getResourceListByScope(
              CodeSystem.class, scope, ResourceRetrievalMode.FULL);
      CodeSystem fullResource = fullResources.getFirst();

      // WHEN
      List<CodeSystem> strippedResources =
          representation.getResourceListByUrlAndScope(
              CodeSystem.class, fullResource.getUrl(), scope, ResourceRetrievalMode.STRIPPED);

      // THEN
      assertThat(strippedResources).isNotEmpty().first().isNotSameAs(fullResource);
    }

    @Test
    @DisplayName("should return empty list when no resource matches url")
    void shouldReturnEmptyListWhenNoResourceMatchesUrl() throws UnknownScopeException {
      // GIVEN
      String scope = FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE;
      String unknownUrl = "https://example.org/unknown";

      // WHEN
      List<CodeSystem> resources =
          representation.getResourceListByUrlAndScope(
              CodeSystem.class, unknownUrl, scope, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(resources).isEmpty();
    }

    @Test
    @DisplayName("should throw for unknown scope")
    void shouldThrowForUnknownScope() {
      // GIVEN
      String unknownScope = "unknown-scope";
      String url = "https://terminologien.bfarm.de/fhir/CodeSystem/test";

      // WHEN THEN
      assertThatThrownBy(
              () ->
                  representation.getResourceListByUrlAndScope(
                      CodeSystem.class, url, unknownScope, ResourceRetrievalMode.FULL))
          .isInstanceOf(UnknownScopeException.class)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("should reject null parameters")
    void shouldRejectNullParameters() {
      // GIVEN
      String scope = FhirTermsInMemoryRepresentationTestDataHelper.DEFAULT_SCOPE;
      String url = "https://terminologien.bfarm.de/fhir/CodeSystem/test";

      // WHEN THEN
      assertThatThrownBy(
              () ->
                  representation.getResourceListByUrlAndScope(
                      null, url, scope, ResourceRetrievalMode.FULL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Resource class must not be null");

      assertThatThrownBy(
              () ->
                  representation.getResourceListByUrlAndScope(
                      CodeSystem.class, null, scope, ResourceRetrievalMode.FULL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("URL must not be null");

      assertThatThrownBy(
              () ->
                  representation.getResourceListByUrlAndScope(
                      CodeSystem.class, url, null, ResourceRetrievalMode.FULL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Scope must not be null");

      assertThatThrownBy(
              () -> representation.getResourceListByUrlAndScope(CodeSystem.class, url, scope, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Resource retrieval mode must not be null");
    }
  }
}

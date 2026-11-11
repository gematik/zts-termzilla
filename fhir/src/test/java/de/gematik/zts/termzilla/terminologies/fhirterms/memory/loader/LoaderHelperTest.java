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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory.loader;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.model.packagemanifest.PackageManifest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LoaderHelper}.
 *
 * <p>Covers the UUID v5 generation, deterministic ID generation for FHIR resources, and the {@link
 * LoaderHelper#applyNewId(org.hl7.fhir.instance.model.api.IBaseResource, String, PackageManifest)}
 * orchestration method.
 */
@DisplayName("LoaderHelper Tests")
class LoaderHelperTest {

  private static final String TEST_URL = "http://example.com/fhir/ValueSet/test";
  private static final String TEST_VERSION = "1.0.0";
  private static final String TEST_PACKAGE_NAME = "example.fhir.test";
  private static final String TEST_PACKAGE_VERSION = "0.1.0";
  private static final String TEST_RESOURCE_HASH = "abc123def456";

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a {@link PackageManifest} with the given name and version. */
  private static PackageManifest createManifest(String name, String version) {
    PackageManifest manifest = new PackageManifest();
    manifest.setName(name);
    manifest.setVersion(version);
    return manifest;
  }

  /** Creates a {@link ValueSet} with the given URL and version. */
  private static ValueSet createValueSet(String url, String version) {
    ValueSet valueSet = new ValueSet();
    valueSet.setUrl(url);
    valueSet.setVersion(version);
    return valueSet;
  }

  // ==========================================================================
  // Tests
  // ==========================================================================

  @Nested
  @DisplayName("uuidV5() Tests")
  class UuidV5Tests {

    private static final UUID URL_NAMESPACE =
        UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");

    @Test
    @DisplayName("Should generate UUID with version 5 marker")
    void shouldGenerateUuidWithVersion5() {
      // GIVEN
      byte[] name = "test".getBytes(StandardCharsets.UTF_8);

      // WHEN
      UUID result = LoaderHelper.uuidV5(URL_NAMESPACE, name);

      // THEN
      assertThat(result.version()).as("UUID should have version 5").isEqualTo(5);
    }

    @Test
    @DisplayName("Should generate UUID with RFC 9562 variant")
    void shouldGenerateUuidWithCorrectVariant() {
      // GIVEN
      byte[] name = "test".getBytes(StandardCharsets.UTF_8);

      // WHEN
      UUID result = LoaderHelper.uuidV5(URL_NAMESPACE, name);

      // THEN
      assertThat(result.variant()).as("UUID should have variant 2 (RFC 9562)").isEqualTo(2);
    }

    @Test
    @DisplayName("Should produce deterministic results for same input")
    void shouldProduceDeterministicResults() {
      // GIVEN
      byte[] name = "deterministic-input".getBytes(StandardCharsets.UTF_8);

      // WHEN
      UUID first = LoaderHelper.uuidV5(URL_NAMESPACE, name);
      UUID second = LoaderHelper.uuidV5(URL_NAMESPACE, name);

      // THEN
      assertThat(first)
          .as("Same namespace and name should produce the same UUID")
          .isEqualTo(second);
    }

    @Test
    @DisplayName("Should produce different UUIDs for different names")
    void shouldProduceDifferentUuidsForDifferentNames() {
      // GIVEN
      byte[] name1 = "name-one".getBytes(StandardCharsets.UTF_8);
      byte[] name2 = "name-two".getBytes(StandardCharsets.UTF_8);

      // WHEN
      UUID result1 = LoaderHelper.uuidV5(URL_NAMESPACE, name1);
      UUID result2 = LoaderHelper.uuidV5(URL_NAMESPACE, name2);

      // THEN
      assertThat(result1)
          .as("Different names should produce different UUIDs")
          .isNotEqualTo(result2);
    }

    @Test
    @DisplayName("Should produce different UUIDs for different namespaces")
    void shouldProduceDifferentUuidsForDifferentNamespaces() {
      // GIVEN
      UUID namespace1 = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
      UUID namespace2 = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");
      byte[] name = "same-name".getBytes(StandardCharsets.UTF_8);

      // WHEN
      UUID result1 = LoaderHelper.uuidV5(namespace1, name);
      UUID result2 = LoaderHelper.uuidV5(namespace2, name);

      // THEN
      assertThat(result1)
          .as("Different namespaces should produce different UUIDs")
          .isNotEqualTo(result2);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
      // GIVEN
      byte[] emptyName = new byte[0];

      // WHEN
      UUID result = LoaderHelper.uuidV5(URL_NAMESPACE, emptyName);

      // THEN
      assertThat(result).as("Should produce a valid UUID even for empty name").isNotNull();
      assertThat(result.version()).as("UUID should still have version 5").isEqualTo(5);
    }
  }

  @Nested
  @DisplayName("applyNewId() Tests")
  class ApplyNewIdTests {

    @Test
    @DisplayName("Should set deterministic ID on MetadataResource with URL")
    void shouldSetDeterministicIdOnMetadataResource() {
      // GIVEN
      ValueSet valueSet = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet, TEST_RESOURCE_HASH, manifest);

      // THEN
      assertThat(valueSet.getId())
          .as("ID should be set on MetadataResource")
          .isNotNull()
          .isNotEmpty();

      // Verify it's a valid UUID
      assertThat(UUID.fromString(valueSet.getIdPart()))
          .as("Generated ID should be a valid UUID")
          .isNotNull();
    }

    @Test
    @DisplayName("Should produce same ID for same inputs")
    void shouldProduceSameIdForSameInputs() {
      // GIVEN
      ValueSet valueSet1 = createValueSet(TEST_URL, TEST_VERSION);
      ValueSet valueSet2 = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet1, TEST_RESOURCE_HASH, manifest);
      LoaderHelper.applyNewId(valueSet2, TEST_RESOURCE_HASH, manifest);

      // THEN
      assertThat(valueSet1.getIdPart())
          .as("Same inputs should produce the same ID")
          .isEqualTo(valueSet2.getIdPart());
    }

    @Test
    @DisplayName("Should produce different ID when resource hash differs")
    void shouldProduceDifferentIdWhenResourceHashDiffers() {
      // GIVEN
      ValueSet valueSet1 = createValueSet(TEST_URL, TEST_VERSION);
      ValueSet valueSet2 = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet1, "hash-aaa", manifest);
      LoaderHelper.applyNewId(valueSet2, "hash-bbb", manifest);

      // THEN
      assertThat(valueSet1.getIdPart())
          .as("Different resource hashes should produce different IDs")
          .isNotEqualTo(valueSet2.getIdPart());
    }

    @Test
    @DisplayName("Should produce different ID when version differs")
    void shouldProduceDifferentIdWhenVersionDiffers() {
      // GIVEN
      ValueSet valueSet1 = createValueSet(TEST_URL, "1.0.0");
      ValueSet valueSet2 = createValueSet(TEST_URL, "2.0.0");
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet1, TEST_RESOURCE_HASH, manifest);
      LoaderHelper.applyNewId(valueSet2, TEST_RESOURCE_HASH, manifest);

      // THEN
      assertThat(valueSet1.getIdPart())
          .as("Different resource versions should produce different IDs")
          .isNotEqualTo(valueSet2.getIdPart());
    }

    @Test
    @DisplayName("Should produce different ID when package name differs")
    void shouldProduceDifferentIdWhenPackageNameDiffers() {
      // GIVEN
      ValueSet valueSet1 = createValueSet(TEST_URL, TEST_VERSION);
      ValueSet valueSet2 = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest1 = createManifest("package.one", TEST_PACKAGE_VERSION);
      PackageManifest manifest2 = createManifest("package.two", TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet1, TEST_RESOURCE_HASH, manifest1);
      LoaderHelper.applyNewId(valueSet2, TEST_RESOURCE_HASH, manifest2);

      // THEN
      assertThat(valueSet1.getIdPart())
          .as("Different package names should produce different IDs")
          .isNotEqualTo(valueSet2.getIdPart());
    }

    @Test
    @DisplayName("Should handle null resource version")
    void shouldHandleNullResourceVersion() {
      // GIVEN
      ValueSet valueSet = createValueSet(TEST_URL, null);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet, TEST_RESOURCE_HASH, manifest);

      // THEN
      assertThat(valueSet.getIdPart())
          .as("Should generate a valid UUID even without resource version")
          .isNotNull();
      assertThat(UUID.fromString(valueSet.getIdPart())).isNotNull();
    }

    @Test
    @DisplayName("Should handle null resource hash")
    void shouldHandleNullResourceHash() {
      // GIVEN
      ValueSet valueSet = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet, null, manifest);

      // THEN
      assertThat(valueSet.getIdPart())
          .as("Should generate a valid UUID even without resource hash")
          .isNotNull();
      assertThat(UUID.fromString(valueSet.getIdPart())).isNotNull();
    }

    @Test
    @DisplayName("Should generate random UUID when resource URL is null")
    void shouldGenerateRandomUuidWhenUrlIsNull() {
      // GIVEN
      ValueSet valueSet1 = createValueSet(null, TEST_VERSION);
      ValueSet valueSet2 = createValueSet(null, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet1, TEST_RESOURCE_HASH, manifest);
      LoaderHelper.applyNewId(valueSet2, TEST_RESOURCE_HASH, manifest);

      // THEN
      assertThat(valueSet1.getIdPart()).as("Should generate a valid UUID for null URL").isNotNull();
      assertThat(valueSet1.getIdPart())
          .as("Random UUIDs should differ across invocations")
          .isNotEqualTo(valueSet2.getIdPart());
    }

    @Test
    @DisplayName("Should not modify non-MetadataResource instances")
    void shouldNotModifyNonMetadataResource() {
      // GIVEN
      Bundle bundle = new Bundle();
      String originalId = bundle.getId();
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(bundle, TEST_RESOURCE_HASH, manifest);

      // THEN
      assertThat(bundle.getId())
          .as("Non-MetadataResource should not have its ID modified")
          .isEqualTo(originalId);
    }
  }

  @Nested
  @DisplayName("applyMetaVersionId() Tests")
  class ApplyMetaVersionIdTests {

    @Test
    @DisplayName("Should set Meta.versionId on MetadataResource")
    void shouldSetMetaVersionId() {
      // GIVEN
      ValueSet valueSet = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyMetaVersionId(valueSet, TEST_RESOURCE_HASH, manifest, 1L);

      // THEN
      assertThat(valueSet.getMeta().getVersionId())
          .as("Meta.versionId should be set")
          .isNotNull()
          .isNotEmpty();
      assertThat(UUID.fromString(valueSet.getMeta().getVersionId()))
          .as("Meta.versionId should be a valid UUID")
          .isNotNull();
    }

    @Test
    @DisplayName("Should not modify the resource ID")
    void shouldNotModifyResourceId() {
      // GIVEN
      ValueSet valueSet = createValueSet(TEST_URL, TEST_VERSION);
      valueSet.setId("original-id");
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyMetaVersionId(valueSet, TEST_RESOURCE_HASH, manifest, 1L);

      // THEN
      assertThat(valueSet.getIdPart())
          .as("Resource ID should remain unchanged")
          .isEqualTo("original-id");
    }

    @Test
    @DisplayName("Should produce deterministic results for same inputs")
    void shouldProduceDeterministicResults() {
      // GIVEN
      ValueSet valueSet1 = createValueSet(TEST_URL, TEST_VERSION);
      ValueSet valueSet2 = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyMetaVersionId(valueSet1, TEST_RESOURCE_HASH, manifest, 42L);
      LoaderHelper.applyMetaVersionId(valueSet2, TEST_RESOURCE_HASH, manifest, 42L);

      // THEN
      assertThat(valueSet1.getMeta().getVersionId())
          .as("Same inputs should produce the same Meta.versionId")
          .isEqualTo(valueSet2.getMeta().getVersionId());
    }

    @Test
    @DisplayName("Should produce different Meta.versionId for different versionId values")
    void shouldProduceDifferentResultForDifferentVersionId() {
      // GIVEN
      ValueSet valueSet1 = createValueSet(TEST_URL, TEST_VERSION);
      ValueSet valueSet2 = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyMetaVersionId(valueSet1, TEST_RESOURCE_HASH, manifest, 1L);
      LoaderHelper.applyMetaVersionId(valueSet2, TEST_RESOURCE_HASH, manifest, 2L);

      // THEN
      assertThat(valueSet1.getMeta().getVersionId())
          .as("Different versionId values should produce different Meta.versionId")
          .isNotEqualTo(valueSet2.getMeta().getVersionId());
    }

    @Test
    @DisplayName("Should produce different Meta.versionId than applyNewId for same resource")
    void shouldDifferFromApplyNewId() {
      // GIVEN
      ValueSet valueSet1 = createValueSet(TEST_URL, TEST_VERSION);
      ValueSet valueSet2 = createValueSet(TEST_URL, TEST_VERSION);
      PackageManifest manifest = createManifest(TEST_PACKAGE_NAME, TEST_PACKAGE_VERSION);

      // WHEN
      LoaderHelper.applyNewId(valueSet1, TEST_RESOURCE_HASH, manifest);
      LoaderHelper.applyMetaVersionId(valueSet2, TEST_RESOURCE_HASH, manifest, 1L);

      // THEN
      assertThat(valueSet1.getIdPart())
          .as("applyNewId and applyMetaVersionId should produce different UUIDs")
          .isNotEqualTo(valueSet2.getMeta().getVersionId());
    }
  }
}

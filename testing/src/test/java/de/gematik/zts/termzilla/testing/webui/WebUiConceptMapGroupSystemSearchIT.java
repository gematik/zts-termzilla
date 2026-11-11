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

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloadException;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testing.data.ConceptMapSearchTestResourceGenerator;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGeneration;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.utilities.CanonicalPair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class WebUiConceptMapGroupSystemSearchIT extends BaseFhirIT {

  public static final String CONCEPTMAP_PACKAGE_NAME = "bfarm.terminologien.conceptmapgrouptests";
  public static final String CONCEPTMAP_PACKAGE_VERSION = "2026.0.0";

  @Override
  protected void copyData(Path packageLoadTempDir) throws PackageDownloadException {
    try {
      prepareTestDataPackage(packageLoadTempDir);
    } catch (PackageGenerationException e) {
      throw new RuntimeException(e);
    }
  }

  private void prepareTestDataPackage(Path packageLoadTempDir) throws PackageGenerationException {
    PackageGeneration packageGeneration =
        PackageGeneration.builder()
            .packageName(CONCEPTMAP_PACKAGE_NAME)
            .packageVersion(CONCEPTMAP_PACKAGE_VERSION)
            .build();

    List<MetadataResource> resources = new ArrayList<>();

    // Add all ConceptMaps with group configurations
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM15());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM16());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM17());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM18());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM19());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM20());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM21());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM22());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM23());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM24());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM25());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM26());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM27());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM28());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM29());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM30());

    packageGeneration.generatePackage(resources, packageLoadTempDir);
  }

  // ==================================================================================================================
  // Test Data Providers
  // ==================================================================================================================

  /**
   * Provides test data for source-system search parameter tests Format: scope, searchValue,
   * expectedResults
   */
  static Stream<Arguments> provideSourceSystemSearchData() {
    String cs01 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS01";
    String cs03 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS03";
    String cs03v2025 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS03|2025";
    String cs03v2026 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS03|2026";
    String cs06 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS06";

    return Stream.of(
        // Search for CS01 (without version) - matches any group.source=CS01
        // Matches: CM15, CM16, CM19, CM22 (2 groups), CM23 (1 group), CM25 (1 group), CM27 (1
        // group)
        Arguments.of(
            "fhir",
            cs01,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM15|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM16|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM19|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM22|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM23|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM27|2025"))),
        // Search for CS03 (without version) - matches groups with CS03 regardless of sourceVersion
        // Matches: CM18, CM20, CM21, CM24 (2 groups), CM25 (1 group)
        Arguments.of(
            "fhir",
            cs03,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM18|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM20|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM21|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"))),
        // Search for CS03|2025 (with version) - matches groups with CS03 AND sourceVersion=2025
        // Matches: CM18, CM20, CM24 (1 of 2 groups), CM25 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs03v2025,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM18|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM20|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"))),
        // Search for CS03|2026 (with version) - matches groups with CS03 AND sourceVersion=2026
        // Matches: CM21, CM24 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs03v2026,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM21|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"))),
        // Search for CS06 - matches CM23 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs06,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM23|2025"))));
  }

  /**
   * Provides test data for target-system search parameter tests Format: scope, searchValue,
   * expectedResults
   */
  static Stream<Arguments> provideTargetSystemSearchData() {
    String cs02 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS02";
    String cs04 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS04";
    String cs04v2025 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS04|2025";
    String cs04v2026 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS04|2026";
    String cs05 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS05";

    return Stream.of(
        // Search for CS02 (without version) - matches any group.target=CS02
        // Matches: CM15, CM17, CM22 (1 group), CM23 (2 groups), CM25 (1 group), CM27 (1 group)
        Arguments.of(
            "fhir",
            cs02,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM15|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM17|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM22|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM23|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM27|2025"))),
        // Search for CS04 (without version) - matches groups with CS04 regardless of targetVersion
        // Matches: CM18, CM19, CM20, CM21, CM24 (2 groups), CM25 (1 group)
        Arguments.of(
            "fhir",
            cs04,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM18|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM19|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM20|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM21|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"))),
        // Search for CS04|2025 (with version) - matches groups with CS04 AND targetVersion=2025
        // Matches: CM19, CM20, CM24 (1 of 2 groups), CM25 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs04v2025,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM19|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM20|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"))),
        // Search for CS04|2026 (with version) - matches groups with CS04 AND targetVersion=2026
        // Matches: CM21, CM24 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs04v2026,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM21|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"))),
        // Search for CS05 - matches CM22 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs05,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM22|2025"))));
  }

  /**
   * Provides test data for combined source-system + target-system search Format: scope,
   * sourceValue, targetValue, expectedResults
   */
  static Stream<Arguments> provideSourceAndTargetSystemSearchData() {
    String cs01 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS01";
    String cs02 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS02";
    String cs03 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS03";
    String cs04 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS04";
    String cs03v2025 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS03|2025";
    String cs04v2025 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS04|2025";
    String cs03v2026 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS03|2026";
    String cs04v2026 = ConceptMapSearchTestResourceGenerator.DEFAULT_CS_URL_PREFIX + "CS04|2026";

    return Stream.of(
        // Search for source=CS01 AND target=CS02 (both without version)
        // Matches: CM15, CM22 (1 of 2 groups), CM23 (1 of 2 groups), CM25 (1 of 2 groups), CM27 (1
        // of 2 groups)
        Arguments.of(
            "fhir",
            cs01,
            cs02,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM15|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM22|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM23|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM27|2025"))),
        // Search for source=CS03 AND target=CS04 (both without version)
        // Matches: CM18, CM20, CM21, CM24 (2 groups), CM25 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs03,
            cs04,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM18|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM20|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM21|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"))),
        // Search for source=CS03|2025 AND target=CS04|2025 (both with version)
        // Matches: CM20, CM24 (1 of 2 groups), CM25 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs03v2025,
            cs04v2025,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM20|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM25|2025"))),
        // Search for source=CS03|2026 AND target=CS04|2026 (both with version)
        // Matches: CM21, CM24 (1 of 2 groups)
        Arguments.of(
            "fhir",
            cs03v2026,
            cs04v2026,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM21|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM24|2025"))));
  }

  /**
   * Provides test data for searching ConceptMaps without source-system Format: scope,
   * expectedResults
   */
  static Stream<Arguments> provideNoSourceSystemSearchData() {
    return Stream.of(
        // ConceptMaps without any group.source
        // Matches: CM17, CM26, CM28, CM29, CM30
        Arguments.of(
            "fhir",
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM17|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM26|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM28|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM29|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM30|2025"))));
  }

  /**
   * Provides test data for searching ConceptMaps without target-system Format: scope,
   * expectedResults
   */
  static Stream<Arguments> provideNoTargetSystemSearchData() {
    return Stream.of(
        // ConceptMaps without any group.target
        // Matches: CM16, CM26, CM28, CM29, CM30
        Arguments.of(
            "fhir",
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM16|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM26|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM28|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM29|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM30|2025"))));
  }

  /**
   * Provides test data for searching ConceptMaps without source-system AND without target-system
   * Format: scope, expectedResults
   */
  static Stream<Arguments> provideNoSourceAndNoTargetSystemSearchData() {
    return Stream.of(
        // ConceptMaps without any group.source AND without any group.target
        // Matches: CM26, CM28, CM29, CM30
        Arguments.of(
            "fhir",
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM26|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM28|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM29|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM30|2025"))));
  }

  // ==================================================================================================================
  // Test Methods
  // ==================================================================================================================

  @DisplayName("Search ConceptMaps by source-system (matches group.source and group.sourceVersion)")
  @ParameterizedTest(
      name = "Search in scope ''{0}'' for source-system=''{1}'' expecting {2} results")
  @MethodSource("provideSourceSystemSearchData")
  void testSearchBySourceSystem(
      String scope, String sourceValue, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source-system=" + sourceValue)
            .elementsSubset("url", "version", "name", "title")
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    assertThat(searchBundle).isNotNull();

    List<String> actualCanonicals =
        searchBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof ConceptMap)
            .map(
                e -> {
                  ConceptMap cm = (ConceptMap) e.getResource();
                  return cm.getUrl() + "|" + cm.getVersion();
                })
            .toList();

    List<String> expectedCanonicals =
        expectedResults.stream().map(cp -> cp.getUrl() + "|" + cp.getVersion()).toList();

    assertThat(actualCanonicals)
        .as("ConceptMaps matching source-system = " + sourceValue)
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps by target-system (matches group.target and group.targetVersion)")
  @ParameterizedTest(
      name = "Search in scope ''{0}'' for target-system=''{1}'' expecting {2} results")
  @MethodSource("provideTargetSystemSearchData")
  void testSearchByTargetSystem(
      String scope, String targetValue, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?target-system=" + targetValue)
            .elementsSubset("url", "version", "name", "title")
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    assertThat(searchBundle).isNotNull();

    List<String> actualCanonicals =
        searchBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof ConceptMap)
            .map(
                e -> {
                  ConceptMap cm = (ConceptMap) e.getResource();
                  return cm.getUrl() + "|" + cm.getVersion();
                })
            .toList();

    List<String> expectedCanonicals =
        expectedResults.stream().map(cp -> cp.getUrl() + "|" + cp.getVersion()).toList();

    assertThat(actualCanonicals)
        .as("ConceptMaps matching target-system = " + targetValue)
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps by source-system AND target-system (combined search)")
  @ParameterizedTest(
      name =
          "Search in scope ''{0}'' for source-system=''{1}'' AND target-system=''{2}'' expecting {3} results")
  @MethodSource("provideSourceAndTargetSystemSearchData")
  void testSearchBySourceAndTargetSystem(
      String scope, String sourceValue, String targetValue, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source-system=" + sourceValue + "&target-system=" + targetValue)
            .elementsSubset("url", "version", "name", "title")
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    assertThat(searchBundle).isNotNull();

    List<String> actualCanonicals =
        searchBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof ConceptMap)
            .map(
                e -> {
                  ConceptMap cm = (ConceptMap) e.getResource();
                  return cm.getUrl() + "|" + cm.getVersion();
                })
            .toList();

    List<String> expectedCanonicals =
        expectedResults.stream().map(cp -> cp.getUrl() + "|" + cp.getVersion()).toList();

    assertThat(actualCanonicals)
        .as(
            "ConceptMaps matching source-system="
                + sourceValue
                + " AND target-system="
                + targetValue)
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps without source-system (source-system:missing=true)")
  @ParameterizedTest(
      name = "Search in scope ''{0}'' for ConceptMaps without source-system expecting {1} results")
  @MethodSource("provideNoSourceSystemSearchData")
  void testSearchByMissingSourceSystem(String scope, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source-system:missing=true")
            .elementsSubset("url", "version", "name", "title")
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    assertThat(searchBundle).isNotNull();

    List<String> actualCanonicals =
        searchBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof ConceptMap)
            .map(
                e -> {
                  ConceptMap cm = (ConceptMap) e.getResource();
                  return cm.getUrl() + "|" + cm.getVersion();
                })
            .toList();

    List<String> expectedCanonicals =
        expectedResults.stream().map(cp -> cp.getUrl() + "|" + cp.getVersion()).toList();

    assertThat(actualCanonicals)
        .as("ConceptMaps without source-system (source-system:missing=true)")
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps without target-system (target-system:missing=true)")
  @ParameterizedTest(
      name = "Search in scope ''{0}'' for ConceptMaps without target-system expecting {1} results")
  @MethodSource("provideNoTargetSystemSearchData")
  void testSearchByMissingTargetSystem(String scope, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?target-system:missing=true")
            .elementsSubset("url", "version", "name", "title")
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    assertThat(searchBundle).isNotNull();

    List<String> actualCanonicals =
        searchBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof ConceptMap)
            .map(
                e -> {
                  ConceptMap cm = (ConceptMap) e.getResource();
                  return cm.getUrl() + "|" + cm.getVersion();
                })
            .toList();

    List<String> expectedCanonicals =
        expectedResults.stream().map(cp -> cp.getUrl() + "|" + cp.getVersion()).toList();

    assertThat(actualCanonicals)
        .as("ConceptMaps without target-system (target-system:missing=true)")
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps without source-system AND without target-system")
  @ParameterizedTest(
      name =
          "Search in scope ''{0}'' for ConceptMaps without source-system and target-system expecting {1} results")
  @MethodSource("provideNoSourceAndNoTargetSystemSearchData")
  void testSearchByMissingSourceAndTargetSystem(String scope, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source-system:missing=true&target-system:missing=true")
            .elementsSubset("url", "version", "name", "title")
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    assertThat(searchBundle).isNotNull();

    List<String> actualCanonicals =
        searchBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof ConceptMap)
            .map(
                e -> {
                  ConceptMap cm = (ConceptMap) e.getResource();
                  return cm.getUrl() + "|" + cm.getVersion();
                })
            .toList();

    List<String> expectedCanonicals =
        expectedResults.stream().map(cp -> cp.getUrl() + "|" + cp.getVersion()).toList();

    assertThat(actualCanonicals)
        .as("ConceptMaps without source-system AND without target-system")
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }
}

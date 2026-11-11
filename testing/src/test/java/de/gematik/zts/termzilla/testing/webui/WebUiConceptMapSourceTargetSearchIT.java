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
public class WebUiConceptMapSourceTargetSearchIT extends BaseFhirIT {

  public static final String CONCEPTMAP_PACKAGE_NAME = "bfarm.terminologien.conceptmaptests";
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

    // Add all ConceptMaps from ConceptMapTestResourceGenerator
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM01());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM02());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM03());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM04());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM05());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM06());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM07());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM08());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM09());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM10());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM11());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM12());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM13());
    resources.add(ConceptMapSearchTestResourceGenerator.createConceptMap_CM14());

    packageGeneration.generatePackage(resources, packageLoadTempDir);
  }

  // ==================================================================================================================
  // Test Data Providers
  // ==================================================================================================================

  /**
   * Provides test data for source/source-uri search parameter tests Format: scope, searchValue,
   * expectedResults
   */
  static Stream<Arguments> provideSourceSearchData() {
    String vs01 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS01";
    String vs00 =
        ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS00"; // without version
    String vs00v2025 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS00|2025";
    String vs04v2025 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS04|2025";

    return Stream.of(
        // Search for VS01 (without version) - matches sourceUri=VS01 and sourceCanonical=VS01 (with
        // or without version)
        // Matches: CM01, CM02, CM04, CM05, CM09, CM10, CM11, CM12
        Arguments.of(
            "fhir",
            vs01,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM01|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM02|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM04|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM05|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM09|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM10|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM11|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM12|2025"))),
        // Search for VS00 (without version) - matches sourceCanonical=VS00|2025
        // Matches: CM07, CM08
        Arguments.of(
            "fhir",
            vs00,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM07|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM08|2025"))),
        // Search for VS00|2025 (with version) - should match only CM07, CM08
        Arguments.of(
            "fhir",
            vs00v2025,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM07|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM08|2025"))),
        // Search for VS04|2025 (with version) - should match only CM13
        Arguments.of(
            "fhir",
            vs04v2025,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM13|2025"))));
  }

  /**
   * Provides test data for target/target-uri search parameter tests Format: scope, searchValue,
   * expectedResults
   */
  static Stream<Arguments> provideTargetSearchData() {
    String vs02 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS02";
    String vs00 =
        ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS00"; // without version
    String vs00v2026 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS00|2026";
    String vs02v2026 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS02|2026";

    return Stream.of(
        // Search for VS02 (without version) - matches targetUri=VS02 and targetCanonical=VS02 (with
        // or without version)
        // Matches: CM01, CM03, CM04, CM06, CM10, CM11, CM12, CM13
        Arguments.of(
            "fhir",
            vs02,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM01|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM03|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM04|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM06|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM10|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM11|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM12|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM13|2025"))),
        // Search for VS00 (without version) - matches targetCanonical=VS00|2026
        // Matches: CM07, CM09
        Arguments.of(
            "fhir",
            vs00,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM07|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM09|2026"))),
        // Search for VS00|2026 (with version) - should match only CM07, CM09
        Arguments.of(
            "fhir",
            vs00v2026,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM07|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM09|2026"))),
        // Search for VS02|2026 (with version) - should match only CM11
        Arguments.of(
            "fhir",
            vs02v2026,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM11|2026"))));
  }

  /**
   * Provides test data for combined source + target search Format: scope, sourceValue, targetValue,
   * expectedResults
   */
  static Stream<Arguments> provideSourceAndTargetSearchData() {
    String vs01 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS01";
    String vs02 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS02";
    String vs00v2025 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS00|2025";
    String vs00v2026 = ConceptMapSearchTestResourceGenerator.DEFAULT_VS_URL_PREFIX + "VS00|2026";

    return Stream.of(
        // Search for source=VS01 AND target=VS02 (both without version)
        // source=VS01 matches: CM01, CM02, CM04, CM05, CM09, CM10, CM11, CM12
        // target=VS02 matches: CM01, CM03, CM04, CM06, CM10, CM11, CM12, CM13
        // Intersection: CM01, CM04, CM10, CM11, CM12
        Arguments.of(
            "fhir",
            vs01,
            vs02,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM01|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM04|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM10|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM11|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM12|2025"))),
        // Search for source=VS00|2025 AND target=VS00|2026 - should match only CM07
        Arguments.of(
            "fhir",
            vs00v2025,
            vs00v2026,
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM07|2025"))));
  }

  // ==================================================================================================================
  // Test Methods
  // ==================================================================================================================

  @DisplayName("Search ConceptMaps by source (matches both sourceUri and sourceCanonical)")
  @ParameterizedTest(name = "Search in scope ''{0}'' for source=''{1}'' expecting {2} results")
  @MethodSource("provideSourceSearchData")
  void testSearchBySource(String scope, String sourceValue, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source=" + sourceValue)
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
        .as("ConceptMaps matching source (either sourceUri or sourceCanonical) = " + sourceValue)
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps by source-uri (same as source parameter)")
  @ParameterizedTest(name = "Search in scope ''{0}'' for source-uri=''{1}'' expecting {2} results")
  @MethodSource("provideSourceSearchData")
  void testSearchBySourceUri(
      String scope, String sourceValue, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source-uri=" + sourceValue)
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
            "ConceptMaps matching source-uri (either sourceUri or sourceCanonical) = "
                + sourceValue)
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps by target (matches both targetUri and targetCanonical)")
  @ParameterizedTest(name = "Search in scope ''{0}'' for target=''{1}'' expecting {2} results")
  @MethodSource("provideTargetSearchData")
  void testSearchByTarget(String scope, String targetValue, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?target=" + targetValue)
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
        .as("ConceptMaps matching target (either targetUri or targetCanonical) = " + targetValue)
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps by target-uri (same as target parameter)")
  @ParameterizedTest(name = "Search in scope ''{0}'' for target-uri=''{1}'' expecting {2} results")
  @MethodSource("provideTargetSearchData")
  void testSearchByTargetUri(
      String scope, String targetValue, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?target-uri=" + targetValue)
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
            "ConceptMaps matching target-uri (either targetUri or targetCanonical) = "
                + targetValue)
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps by source AND target (combined search)")
  @ParameterizedTest(
      name = "Search in scope ''{0}'' for source=''{1}'' AND target=''{2}'' expecting {3} results")
  @MethodSource("provideSourceAndTargetSearchData")
  void testSearchBySourceAndTarget(
      String scope, String sourceValue, String targetValue, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source=" + sourceValue + "&target=" + targetValue)
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
        .as("ConceptMaps matching source=" + sourceValue + " AND target=" + targetValue)
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  // ==================================================================================================================
  // Test Data Providers (Missing Tests)
  // ==================================================================================================================

  /** Provides test data for searching ConceptMaps without source Format: scope, expectedResults */
  static Stream<Arguments> provideNoSourceSearchData() {
    return Stream.of(
        // ConceptMaps without source (neither sourceUri nor sourceCanonical)
        // Only CM03, CM06, CM14
        Arguments.of(
            "fhir",
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM03|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM06|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM14|2025"))));
  }

  /** Provides test data for searching ConceptMaps without target Format: scope, expectedResults */
  static Stream<Arguments> provideNoTargetSearchData() {
    return Stream.of(
        // ConceptMaps without target (neither targetUri nor targetCanonical)
        // Only CM02, CM05, CM14
        Arguments.of(
            "fhir",
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM02|2025"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM05|2026"),
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM14|2025"))));
  }

  /**
   * Provides test data for searching ConceptMaps without source AND without target Format: scope,
   * expectedResults
   */
  static Stream<Arguments> provideNoSourceAndNoTargetSearchData() {
    return Stream.of(
        // ConceptMaps without source AND without target
        // Only CM14
        Arguments.of(
            "fhir",
            List.of(
                new CanonicalPair(
                    ConceptMapSearchTestResourceGenerator.DEFAULT_CM_URL_PREFIX + "CM14|2025"))));
  }

  // ==================================================================================================================
  // Missing Test Methods
  // ==================================================================================================================

  @DisplayName("Search ConceptMaps without source (source:missing=true)")
  @ParameterizedTest(
      name = "Search in scope ''{0}'' for ConceptMaps without source expecting {1} results")
  @MethodSource("provideNoSourceSearchData")
  void testSearchByMissingSource(String scope, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source:missing=true")
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
        .as("ConceptMaps without source (source:missing=true)")
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps without target (target:missing=true)")
  @ParameterizedTest(
      name = "Search in scope ''{0}'' for ConceptMaps without target expecting {1} results")
  @MethodSource("provideNoTargetSearchData")
  void testSearchByMissingTarget(String scope, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?target:missing=true")
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
        .as("ConceptMaps without target (target:missing=true)")
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  @DisplayName("Search ConceptMaps without source AND without target")
  @ParameterizedTest(
      name =
          "Search in scope ''{0}'' for ConceptMaps without source and target expecting {1} results")
  @MethodSource("provideNoSourceAndNoTargetSearchData")
  void testSearchByMissingSourceAndTarget(String scope, List<CanonicalPair> expectedResults) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl("ConceptMap?source:missing=true&target:missing=true")
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
        .as("ConceptMaps without source AND without target")
        .containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }
}

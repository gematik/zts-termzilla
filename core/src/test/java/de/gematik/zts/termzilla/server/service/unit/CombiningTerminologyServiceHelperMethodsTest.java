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
package de.gematik.zts.termzilla.server.service.unit;

import static de.gematik.zts.termzilla.server.service.fixtures.MockManagerFactory.createMockManager;
import static de.gematik.zts.termzilla.server.service.fixtures.MockManagerFactory.createMultiScopeManager;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CombiningTerminologyService} helper methods.
 *
 * <p>Tests the helper methods: checkScope() and getResponsibleTerminologyManager().
 */
@Tag("unit")
@DisplayName("CombiningTerminologyService - Helper Methods")
class CombiningTerminologyServiceHelperMethodsTest {

  private static final String SNOMED_URL = "http://snomed.info/sct";
  private static final String LOINC_URL = "http://loinc.org";
  private static final String ICD10_URL = "http://hl7.org/fhir/sid/icd-10";
  private static final String UNKNOWN_URL = "http://unknown.org/CodeSystem/unknown";

  private ConfigProperties config;

  @BeforeEach
  void setUp() {
    config = new ConfigProperties();
    config.setBaseWorkingDir("/tmp/termzilla-test");
  }

  // ==================================================================================================
  // checkScope() Tests
  // ==================================================================================================

  @Nested
  @DisplayName("checkScope()")
  class CheckScope {

    @Test
    @DisplayName("Should not throw exception for supported scope")
    void shouldAcceptSupportedScope() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertDoesNotThrow(() -> service.checkScope("fhir"), "Should accept supported scope 'fhir'");
    }

    @Test
    @DisplayName("Should throw UnknownScopeException when no scopes are supported")
    void shouldRejectAnyScope_whenNoScopesConfigured() throws InvalidConfigurationException {
      // Given - service with no managers (no scopes)
      CombiningTerminologyService service = new CombiningTerminologyService(List.of(), config);

      // When/Then
      assertThrows(
          UnknownScopeException.class,
          () -> service.checkScope("fhir"),
          "Should throw UnknownScopeException when no scopes are supported");
    }

    @Test
    @DisplayName("Should accept all scopes from multiple managers")
    void shouldAcceptAllScopesFromMultipleManagers() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager1 =
          createMultiScopeManager("M1", Set.of("fhir", "snomed"), List.of(SNOMED_URL));

      TerminologyManager manager2 =
          createMultiScopeManager("M2", Set.of("custom", "loinc"), List.of(LOINC_URL));

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager1, manager2), config);

      // When/Then
      assertDoesNotThrow(() -> service.checkScope("fhir"), "Should accept 'fhir' scope");
      assertDoesNotThrow(() -> service.checkScope("snomed"), "Should accept 'snomed' scope");
      assertDoesNotThrow(() -> service.checkScope("custom"), "Should accept 'custom' scope");
      assertDoesNotThrow(() -> service.checkScope("loinc"), "Should accept 'loinc' scope");
    }

    @Test
    @DisplayName("Should reject scope not in merged scope set")
    void shouldRejectScopeNotInMergedSet() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager1 =
          createMultiScopeManager("M1", Set.of("fhir"), List.of(SNOMED_URL));

      TerminologyManager manager2 =
          createMultiScopeManager("M2", Set.of("custom"), List.of(LOINC_URL));

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager1, manager2), config);

      // When/Then
      assertThrows(
          UnknownScopeException.class,
          () -> service.checkScope("unknown"),
          "Should reject scope not in merged set");
    }

    @ParameterizedTest(name = "Should reject scope input: {0}")
    @NullAndEmptySource
    @ValueSource(strings = {"unknown"})
    @DisplayName("Should throw UnknownScopeException for invalid scopes")
    void shouldRejectInvalidScopeInputs(final String scope) throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          UnknownScopeException.class,
          () -> service.checkScope(scope),
          "Should throw UnknownScopeException for unsupported, null, or empty scope");
    }

    @Test
    @DisplayName("Should be case-sensitive for scope matching")
    void shouldBeCaseSensitiveForScopes() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertDoesNotThrow(() -> service.checkScope("fhir"), "Should accept 'fhir' (lowercase)");

      assertThrows(
          UnknownScopeException.class,
          () -> service.checkScope("FHIR"),
          "Should reject 'FHIR' (uppercase) - scopes are case-sensitive");
    }
  }

  // ==================================================================================================
  // getResponsibleTerminologyManager() Tests
  // ==================================================================================================

  @Nested
  @DisplayName("getResponsibleTerminologyManager()")
  class GetResponsibleTerminologyManager {

    @Test
    @DisplayName("Should return manager for registered CodeSystem URL")
    void shouldReturnManagerForRegisteredUrl() throws Exception {
      // Given
      TerminologyManager snomedManager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(snomedManager), config);

      // When
      TerminologyManager result = service.getResponsibleTerminologyManager(SNOMED_URL);

      // Then
      assertNotNull(result, "Should return a manager");
      assertEquals(
          snomedManager, result, "Should return the correct manager for SNOMED CodeSystem");
    }

    @Test
    @DisplayName("Should throw UnsupportedTerminologyException for unknown URL")
    void shouldThrowExceptionForUnknownUrl() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          UnsupportedTerminologyException.class,
          () -> service.getResponsibleTerminologyManager(UNKNOWN_URL),
          "Should throw UnsupportedTerminologyException for unknown URL");
    }

    @Test
    @DisplayName("Should return correct manager when multiple managers are registered")
    void shouldReturnCorrectManager_withMultipleManagers() throws Exception {
      // Given
      TerminologyManager snomedManager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      TerminologyManager loincManager =
          createMockManager("LOINC", Set.of("fhir"), List.of(LOINC_URL), List.of(), List.of());

      TerminologyManager icd10Manager =
          createMockManager("ICD10", Set.of("fhir"), List.of(ICD10_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(
              List.of(snomedManager, loincManager, icd10Manager), config);

      // When/Then
      assertEquals(
          snomedManager,
          service.getResponsibleTerminologyManager(SNOMED_URL),
          "Should route SNOMED URL to SNOMED manager");

      assertEquals(
          loincManager,
          service.getResponsibleTerminologyManager(LOINC_URL),
          "Should route LOINC URL to LOINC manager");

      assertEquals(
          icd10Manager,
          service.getResponsibleTerminologyManager(ICD10_URL),
          "Should route ICD-10 URL to ICD-10 manager");
    }

    @Test
    @DisplayName("Should throw exception when no managers are registered")
    void shouldThrowException_whenNoManagers() throws InvalidConfigurationException {
      // Given
      CombiningTerminologyService service = new CombiningTerminologyService(List.of(), config);

      // When/Then
      assertThrows(
          UnsupportedTerminologyException.class,
          () -> service.getResponsibleTerminologyManager(SNOMED_URL),
          "Should throw UnsupportedTerminologyException when no managers are registered");
    }

    @Test
    @DisplayName("Should handle manager with multiple CodeSystems")
    void shouldHandleManagerWithMultipleCodeSystems() throws Exception {
      // Given
      TerminologyManager multiManager =
          createMockManager(
              "Multi",
              Set.of("fhir"),
              List.of(SNOMED_URL, LOINC_URL, ICD10_URL),
              List.of(),
              List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(multiManager), config);

      // When/Then
      assertEquals(
          multiManager,
          service.getResponsibleTerminologyManager(SNOMED_URL),
          "Should return manager for SNOMED");

      assertEquals(
          multiManager,
          service.getResponsibleTerminologyManager(LOINC_URL),
          "Should return manager for LOINC");

      assertEquals(
          multiManager,
          service.getResponsibleTerminologyManager(ICD10_URL),
          "Should return manager for ICD-10");
    }

    @Test
    @DisplayName("Should return last registered manager for duplicate URLs")
    void shouldReturnLastManager_forDuplicateUrls() throws Exception {
      // Given
      TerminologyManager manager1 =
          createMockManager("M1", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      TerminologyManager manager2 =
          createMockManager("M2", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager1, manager2), config);

      // When
      TerminologyManager result = service.getResponsibleTerminologyManager(SNOMED_URL);

      // Then
      assertEquals(
          manager2,
          result,
          "Should return last registered manager when URL is registered multiple times");
    }

    @Test
    @DisplayName("Should handle null URL parameter")
    void shouldHandleNullUrl() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          UnsupportedTerminologyException.class,
          () -> service.getResponsibleTerminologyManager(null),
          "Should throw UnsupportedTerminologyException for null URL");
    }

    @Test
    @DisplayName("Should handle empty string URL")
    void shouldHandleEmptyUrl() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          UnsupportedTerminologyException.class,
          () -> service.getResponsibleTerminologyManager(""),
          "Should throw UnsupportedTerminologyException for empty URL");
    }

    @Test
    @DisplayName("Should be case-sensitive for URL matching")
    void shouldBeCaseSensitiveForUrls() throws Exception {
      // Given
      String mixedCaseUrl = "http://SNOMED.info/sct";
      TerminologyManager manager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertDoesNotThrow(
          () -> service.getResponsibleTerminologyManager(SNOMED_URL),
          "Should find manager for exact URL match");

      assertThrows(
          UnsupportedTerminologyException.class,
          () -> service.getResponsibleTerminologyManager(mixedCaseUrl),
          "Should not find manager for URL with different case");
    }

    @Test
    @DisplayName("Should only check CodeSystem map, not ValueSet or ConceptMap maps")
    void shouldOnlyCheckCodeSystemMap() throws Exception {
      // Given - manager with only ValueSet registered
      String vsUrl = "http://example.org/fhir/ValueSet/example";
      TerminologyManager manager =
          createMockManager("VS-Only", Set.of("fhir"), List.of(), List.of(vsUrl), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          UnsupportedTerminologyException.class,
          () -> service.getResponsibleTerminologyManager(vsUrl),
          "Should not find manager when URL is only in ValueSet map, not CodeSystem map");
    }
  }
}

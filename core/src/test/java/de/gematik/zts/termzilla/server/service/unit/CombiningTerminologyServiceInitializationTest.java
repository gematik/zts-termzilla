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
import static de.gematik.zts.termzilla.server.service.fixtures.MockManagerFactory.createUninitializedManager;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CombiningTerminologyService} initialization.
 *
 * <p>Tests the constructor logic, manager registration, and routing map setup.
 */
@Tag("unit")
@DisplayName("CombiningTerminologyService - Initialization")
class CombiningTerminologyServiceInitializationTest {

  private static final String SNOMED_URL = "http://snomed.info/sct";
  private static final String LOINC_URL = "http://loinc.org";

  private static final String CS1_EXAMPLE = "http://example.org/fhir/CodeSystem/example01";
  private static final String CS2_EXAMPLE = "http://example.org/fhir/CodeSystem/example02";
  private static final String CS3_EXAMPLE = "http://example.org/fhir/CodeSystem/example03";
  private static final String VS_EXAMPLE = "http://example.org/fhir/ValueSet/example";
  private static final String CM_EXAMPLE = "http://example.org/fhir/ConceptMap/example";

  /**
   * Creates a minimal ConfigProperties for testing.
   *
   * @return ConfigProperties with baseWorkingDir set
   */
  private ConfigProperties createTestConfig() {
    ConfigProperties config = new ConfigProperties();
    config.setBaseWorkingDir("/tmp/termzilla-test");
    return config;
  }

  // ==================================================================================================
  // Constructor with List<TerminologyManager> - Basic Scenarios
  // ==================================================================================================

  @Nested
  @DisplayName("Constructor with List<TerminologyManager>")
  class ConstructorWithManagerList {

    @Test
    @DisplayName("Should initialize with empty manager list")
    void shouldInitializeWithEmptyList() throws InvalidConfigurationException {
      // Given
      List<TerminologyManager> emptyList = List.of();
      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service = new CombiningTerminologyService(emptyList, config);

      // Then
      assertNotNull(service, "Service should be created");
      assertTrue(service.getTerminologyManagerList().isEmpty(), "Manager list should be empty");
      assertTrue(service.getSupportedScopes().isEmpty(), "No scopes should be supported");
      assertTrue(
          service.getTerminologyManagerCodeSystemMap().isEmpty(), "CodeSystem map should be empty");
      assertTrue(
          service.getTerminologyManagerValueSetMap().isEmpty(), "ValueSet map should be empty");
      assertTrue(
          service.getTerminologyManagerConceptMapMap().isEmpty(), "ConceptMap map should be empty");
    }

    @Test
    @DisplayName("Should register single initialized manager")
    void shouldRegisterSingleManager() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager(
              "TestManager",
              Set.of("fhir"),
              List.of(CS1_EXAMPLE),
              List.of(VS_EXAMPLE),
              List.of(CM_EXAMPLE));

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // Then
      assertEquals(
          1, service.getTerminologyManagerList().size(), "One manager should be registered");
      assertTrue(service.getSupportedScopes().contains("fhir"), "FHIR scope should be supported");

      assertEquals(
          manager,
          service.getTerminologyManagerCodeSystemMap().get(CS1_EXAMPLE),
          "SNOMED should be mapped to manager");
      assertEquals(
          manager,
          service.getTerminologyManagerValueSetMap().get(VS_EXAMPLE),
          "ValueSet should be mapped to manager");
      assertEquals(
          manager,
          service.getTerminologyManagerConceptMapMap().get(CM_EXAMPLE),
          "ConceptMap should be mapped to manager");
    }

    @Test
    @DisplayName("Should register multiple managers")
    void shouldRegisterMultipleManagers() throws InvalidConfigurationException {
      // Given
      TerminologyManager snomedManager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      TerminologyManager loincManager =
          createMockManager("LOINC", Set.of("fhir"), List.of(LOINC_URL), List.of(), List.of());

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(snomedManager, loincManager), config);

      // Then
      assertEquals(
          2, service.getTerminologyManagerList().size(), "Two managers should be registered");
      assertTrue(
          service.getTerminologyManagerList().contains(snomedManager),
          "SNOMED manager should be registered");
      assertTrue(
          service.getTerminologyManagerList().contains(loincManager),
          "LOINC manager should be registered");

      assertEquals(
          snomedManager,
          service.getTerminologyManagerCodeSystemMap().get(SNOMED_URL),
          "SNOMED URL should route to SNOMED manager");
      assertEquals(
          loincManager,
          service.getTerminologyManagerCodeSystemMap().get(LOINC_URL),
          "LOINC URL should route to LOINC manager");
    }

    @Test
    @DisplayName("Should skip uninitialized managers")
    void shouldSkipUninitializedManagers() throws InvalidConfigurationException {
      // Given
      TerminologyManager initializedManager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      TerminologyManager uninitializedManager = createUninitializedManager("LOINC");

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(
              List.of(initializedManager, uninitializedManager), config);

      // Then
      assertEquals(
          1,
          service.getTerminologyManagerList().size(),
          "Only initialized manager should be registered");
      assertTrue(
          service.getTerminologyManagerList().contains(initializedManager),
          "Initialized manager should be registered");
      assertFalse(
          service.getTerminologyManagerList().contains(uninitializedManager),
          "Uninitialized manager should not be registered");
    }

    @Test
    @DisplayName("Should merge scopes from multiple managers")
    void shouldMergeScopesFromMultipleManagers() throws InvalidConfigurationException {
      // Given
      TerminologyManager fhirManager =
          createMultiScopeManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL));

      TerminologyManager customManager =
          createMultiScopeManager("FHIR", Set.of("custom", "fhir"), List.of(CS1_EXAMPLE));

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(fhirManager, customManager), config);

      // Then
      assertEquals(2, service.getSupportedScopes().size(), "Two scopes should be supported");
      assertTrue(service.getSupportedScopes().contains("fhir"), "FHIR scope should be supported");
      assertTrue(
          service.getSupportedScopes().contains("custom"), "Custom scope should be supported");
    }

    @Test
    @DisplayName("Should handle manager with no resources")
    void shouldHandleManagerWithNoResources() throws InvalidConfigurationException {
      // Given
      TerminologyManager emptyManager =
          createMockManager("Empty", Set.of("fhir"), List.of(), List.of(), List.of());

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(emptyManager), config);

      // Then
      assertEquals(
          1,
          service.getTerminologyManagerList().size(),
          "Manager should be registered even with no resources");
      assertTrue(
          service.getTerminologyManagerCodeSystemMap().isEmpty(), "CodeSystem map should be empty");
      assertTrue(
          service.getTerminologyManagerValueSetMap().isEmpty(), "ValueSet map should be empty");
      assertTrue(
          service.getTerminologyManagerConceptMapMap().isEmpty(), "ConceptMap map should be empty");
    }
  }

  // ==================================================================================================
  // Routing Map Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Routing Maps")
  class RoutingMaps {

    @Test
    @DisplayName("Should map multiple CodeSystems to same manager")
    void shouldMapMultipleCodeSystemsToSameManager() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager(
              "Multi-CS",
              Set.of("fhir"),
              List.of(CS1_EXAMPLE, CS2_EXAMPLE, CS3_EXAMPLE),
              List.of(),
              List.of());

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // Then
      assertEquals(
          3,
          service.getTerminologyManagerCodeSystemMap().size(),
          "Three CodeSystems should be mapped");
      assertEquals(
          manager,
          service.getTerminologyManagerCodeSystemMap().get(CS1_EXAMPLE),
          "CS1 should map to manager");
      assertEquals(
          manager,
          service.getTerminologyManagerCodeSystemMap().get(CS2_EXAMPLE),
          "CS2 should map to manager");
      assertEquals(
          manager,
          service.getTerminologyManagerCodeSystemMap().get(CS3_EXAMPLE),
          "CS3 should map to manager");
    }

    @Test
    @DisplayName("Should handle last-wins when multiple managers support same CodeSystem")
    void shouldHandleLastWinsForDuplicateCodeSystems() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager1 =
          createMockManager("Manager1", Set.of("fhir"), List.of(CS1_EXAMPLE), List.of(), List.of());

      TerminologyManager manager2 =
          createMockManager("Manager2", Set.of("fhir"), List.of(CS1_EXAMPLE), List.of(), List.of());

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager1, manager2), config);

      // Then
      assertEquals(
          manager2,
          service.getTerminologyManagerCodeSystemMap().get(CS1_EXAMPLE),
          "Last registered manager should win for duplicate CodeSystem");
    }

    @Test
    @DisplayName("Should maintain separate routing for CodeSystems, ValueSets, and ConceptMaps")
    void shouldMaintainSeparateRoutingMaps() throws InvalidConfigurationException {
      // Given
      String sameUrl = "http://example.org/fhir/same-url";

      TerminologyManager csManager =
          createMockManager("CS-Manager", Set.of("fhir"), List.of(sameUrl), List.of(), List.of());

      TerminologyManager vsManager =
          createMockManager("VS-Manager", Set.of("fhir"), List.of(), List.of(sameUrl), List.of());

      TerminologyManager cmManager =
          createMockManager("CM-Manager", Set.of("fhir"), List.of(), List.of(), List.of(sameUrl));

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(csManager, vsManager, cmManager), config);

      // Then
      assertEquals(
          csManager,
          service.getTerminologyManagerCodeSystemMap().get(sameUrl),
          "Same URL should route to different managers per resource type");
      assertEquals(
          vsManager,
          service.getTerminologyManagerValueSetMap().get(sameUrl),
          "Same URL should route to different managers per resource type");
      assertEquals(
          cmManager,
          service.getTerminologyManagerConceptMapMap().get(sameUrl),
          "Same URL should route to different managers per resource type");
    }
  }

  // ==================================================================================================
  // Edge Cases
  // ==================================================================================================

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("Should handle manager with empty scopes")
    void shouldHandleManagerWithEmptyScopes() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager("NoScope", Set.of(), List.of(SNOMED_URL), List.of(), List.of());

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // Then
      assertTrue(service.getSupportedScopes().isEmpty(), "No scopes should be supported");
      assertEquals(
          1, service.getTerminologyManagerList().size(), "Manager should still be registered");
    }

    @Test
    @DisplayName("Should handle mixed initialized and uninitialized managers")
    void shouldHandleMixedManagerStates() throws InvalidConfigurationException {
      // Given
      List<TerminologyManager> managers =
          Arrays.asList(
              createMockManager("M1", Set.of("fhir"), List.of(CS1_EXAMPLE), List.of(), List.of()),
              createUninitializedManager("M2"),
              createMockManager("M3", Set.of("fhir"), List.of(CS2_EXAMPLE), List.of(), List.of()),
              createUninitializedManager("M4"),
              createMockManager("M5", Set.of("fhir"), List.of(CS3_EXAMPLE), List.of(), List.of()));

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service = new CombiningTerminologyService(managers, config);

      // Then
      assertEquals(
          3,
          service.getTerminologyManagerList().size(),
          "Only initialized managers should be registered");
      assertEquals(
          3,
          service.getTerminologyManagerCodeSystemMap().size(),
          "Three CodeSystems should be mapped");
    }
  }

  // ==================================================================================================
  // CacheManager Initialization
  // ==================================================================================================

  @Nested
  @DisplayName("CacheManager Initialization")
  class CacheManagerInitialization {

    @Test
    @DisplayName("Should initialize CacheManager automatically")
    void shouldInitializeCacheManager() throws InvalidConfigurationException {
      // Given
      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service = new CombiningTerminologyService(List.of(), config);

      // Then
      assertNotNull(service.getCacheManager(), "CacheManager should be initialized");
    }

    @Test
    @DisplayName("CacheManager should be accessible after initialization")
    void cacheManagerShouldBeAccessible() throws InvalidConfigurationException {
      // Given
      TerminologyManager manager =
          createMockManager("SNOMED", Set.of("fhir"), List.of(SNOMED_URL), List.of(), List.of());

      ConfigProperties config = createTestConfig();

      // When
      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // Then
      assertNotNull(service.getCacheManager(), "CacheManager should not be null");
      assertDoesNotThrow(
          () -> service.getCacheManager().toString(),
          "CacheManager should be usable after initialization");
    }
  }
}

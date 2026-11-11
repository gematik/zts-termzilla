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

import static de.gematik.zts.termzilla.server.service.fixtures.AssertionHelpers.assertCodeSystemConceptCount;
import static de.gematik.zts.termzilla.server.service.fixtures.AssertionHelpers.assertCodeSystemUrl;
import static de.gematik.zts.termzilla.server.service.fixtures.MockManagerFactory.createMockManager;
import static de.gematik.zts.termzilla.server.service.fixtures.MockManagerFactory.createMultiScopeManager;
import static de.gematik.zts.termzilla.server.service.fixtures.TestDataBuilder.createCodeSystem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CombiningTerminologyService} CodeSystem retrieval methods.
 *
 * <p>Tests getDefaultCodeSystemByScopeAndUrl() and getCodeSystem() methods.
 */
@Tag("unit")
@DisplayName("CombiningTerminologyService - CodeSystem Retrieval")
class CombiningTerminologyServiceCodeSystemRetrievalTest {

  private static final String SNOMED_URL = "http://snomed.info/sct";
  private static final String LOINC_URL = "http://loinc.org";
  private static final String ICD10_URL = "http://hl7.org/fhir/sid/icd-10";
  private static final String UNKNOWN_URL = "http://unknown.org/CodeSystem/unknown";

  private static final String SCOPE_FHIR = "fhir";
  private static final String SCOPE_CUSTOM = "custom";

  private ConfigProperties config;

  @BeforeEach
  void setUp() {
    config = new ConfigProperties();
    config.setBaseWorkingDir("/tmp/termzilla-test");
  }

  // ==================================================================================================
  // getDefaultCodeSystemByScopeAndUrl() Tests
  // ==================================================================================================

  @Nested
  @DisplayName("getDefaultCodeSystemByScopeAndUrl()")
  class GetDefaultCodeSystemByScopeAndUrl {

    @Test
    @DisplayName("Should return CodeSystem when found by scope and URL")
    void shouldReturnCodeSystemWhenFound() throws Exception {
      // Given
      CodeSystem expectedCs = createCodeSystem(SNOMED_URL, List.of("123456789"));

      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(manager
              .cs()
              .getDefaultCodeSystemByUrlAndScope(
                  SCOPE_FHIR, SNOMED_URL, ResourceRetrievalMode.FULL))
          .thenReturn(expectedCs);

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When
      CodeSystem result =
          service.getDefaultCodeSystemByScopeAndUrl(
              SCOPE_FHIR, SNOMED_URL, ResourceRetrievalMode.FULL);

      // Then
      assertNotNull(result, "Should return a CodeSystem");
      assertCodeSystemUrl(result, SNOMED_URL);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when CodeSystem not found")
    void shouldThrowExceptionWhenNotFound() throws Exception {
      // Given
      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(manager.cs().getDefaultCodeSystemByUrlAndScope(SCOPE_FHIR, UNKNOWN_URL))
          .thenThrow(new ResourceNotFoundException("Not found"));

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      ResourceNotFoundException exception =
          assertThrows(
              ResourceNotFoundException.class,
              () -> service.getDefaultCodeSystemByScopeAndUrl(SCOPE_FHIR, UNKNOWN_URL),
              "Should throw ResourceNotFoundException when CodeSystem not found");

      assertTrue(
          exception.getMessage().contains(UNKNOWN_URL),
          "Exception message should contain the requested URL");
      assertTrue(
          exception.getMessage().contains(SCOPE_FHIR),
          "Exception message should contain the scope");
    }

    @Test
    @DisplayName("Should query multiple managers and return first match")
    void shouldQueryMultipleManagers() throws Exception {
      // Given
      CodeSystem expectedCs = createCodeSystem(LOINC_URL, List.of("1234-5"));

      TerminologyManager snomedManager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(snomedManager.cs().getDefaultCodeSystemByUrlAndScope(SCOPE_FHIR, LOINC_URL))
          .thenThrow(new ResourceNotFoundException("Not found"));

      TerminologyManager loincManager =
          createMockManager("LOINC", Set.of(SCOPE_FHIR), List.of(LOINC_URL), List.of(), List.of());
      when(loincManager
              .cs()
              .getDefaultCodeSystemByUrlAndScope(SCOPE_FHIR, LOINC_URL, ResourceRetrievalMode.FULL))
          .thenReturn(expectedCs);

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(snomedManager, loincManager), config);

      // When
      CodeSystem result =
          service.getDefaultCodeSystemByScopeAndUrl(
              SCOPE_FHIR, LOINC_URL, ResourceRetrievalMode.FULL);

      // Then
      assertNotNull(result, "Should return CodeSystem from second manager");
      assertCodeSystemUrl(result, LOINC_URL);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when no managers registered")
    void shouldThrowExceptionWhenNoManagers() throws Exception {
      // Given
      CombiningTerminologyService service = new CombiningTerminologyService(List.of(), config);

      // When/Then
      assertThrows(
          ResourceNotFoundException.class,
          () -> service.getDefaultCodeSystemByScopeAndUrl(SCOPE_FHIR, SNOMED_URL),
          "Should throw ResourceNotFoundException when no managers are registered");
    }

    @Test
    @DisplayName("Should handle exceptions from managers gracefully")
    void shouldHandleManagerExceptions() throws Exception {
      // Given
      CodeSystem expectedCs = createCodeSystem(SNOMED_URL, List.of("123456789"));

      TerminologyManager failingManager =
          createMockManager(
              "Failing", Set.of(SCOPE_FHIR), List.of(ICD10_URL), List.of(), List.of());
      when(failingManager.cs().getDefaultCodeSystemByUrlAndScope(any(), any()))
          .thenThrow(new RuntimeException("Manager error"));

      TerminologyManager workingManager =
          createMockManager(
              "Working", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(workingManager
              .cs()
              .getDefaultCodeSystemByUrlAndScope(
                  SCOPE_FHIR, SNOMED_URL, ResourceRetrievalMode.FULL))
          .thenReturn(expectedCs);

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(failingManager, workingManager), config);

      // When
      CodeSystem result = service.getDefaultCodeSystemByScopeAndUrl(SCOPE_FHIR, SNOMED_URL);

      // Then
      assertNotNull(result, "Should return result despite one manager failing");
      assertCodeSystemUrl(result, SNOMED_URL);
    }

    @Test
    @DisplayName("Should respect scope parameter")
    void shouldRespectScope() throws Exception {
      // Given
      CodeSystem fhirCs = createCodeSystem(SNOMED_URL, List.of("123456789"));
      CodeSystem customCs = createCodeSystem(SNOMED_URL, List.of("987654321"));

      TerminologyManager manager =
          createMultiScopeManager("Multi", Set.of(SCOPE_FHIR, SCOPE_CUSTOM), List.of(SNOMED_URL));
      when(manager
              .cs()
              .getDefaultCodeSystemByUrlAndScope(
                  SCOPE_FHIR, SNOMED_URL, ResourceRetrievalMode.FULL))
          .thenReturn(fhirCs);
      when(manager
              .cs()
              .getDefaultCodeSystemByUrlAndScope(
                  SCOPE_CUSTOM, SNOMED_URL, ResourceRetrievalMode.FULL))
          .thenReturn(customCs);

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When
      CodeSystem fhirResult = service.getDefaultCodeSystemByScopeAndUrl(SCOPE_FHIR, SNOMED_URL);
      CodeSystem customResult = service.getDefaultCodeSystemByScopeAndUrl(SCOPE_CUSTOM, SNOMED_URL);

      // Then
      assertNotNull(fhirResult, "Should return CodeSystem for fhir scope");
      assertNotNull(customResult, "Should return CodeSystem for custom scope");
      assertCodeSystemConceptCount(fhirResult, 1);
      assertCodeSystemConceptCount(customResult, 1);
    }

    @Test
    @DisplayName("Should handle null URL parameter")
    void shouldHandleNullUrl() throws Exception {
      // Given
      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          ResourceNotFoundException.class,
          () -> service.getDefaultCodeSystemByScopeAndUrl(SCOPE_FHIR, null),
          "Should throw exception for null URL");
    }

    @Test
    @DisplayName("Should handle null scope parameter")
    void shouldHandleNullScope() throws Exception {
      // Given
      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          ResourceNotFoundException.class,
          () -> service.getDefaultCodeSystemByScopeAndUrl(null, SNOMED_URL),
          "Should throw exception for null scope");
    }
  }

  // ==================================================================================================
  // getCodeSystem() Tests
  // ==================================================================================================

  @Nested
  @DisplayName("getCodeSystem()")
  class GetCodeSystem {

    @Test
    @DisplayName("Should return CodeSystem by IdType")
    void shouldReturnCodeSystemById() throws Exception {
      // Given
      IdType csId = new IdType("CodeSystem", "snomed-ct");
      CodeSystem expectedCs = createCodeSystem(SNOMED_URL, List.of("123456789"));
      expectedCs.setId(csId);

      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(manager.cs().getCodeSystem(SCOPE_FHIR, csId, ResourceRetrievalMode.FULL))
          .thenReturn(expectedCs);

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When
      CodeSystem result = service.getCodeSystem(SCOPE_FHIR, csId);

      // Then
      assertNotNull(result, "Should return a CodeSystem");
      assertEquals(csId.getIdPart(), result.getIdElement().getIdPart(), "IDs should match");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when CodeSystem not found by ID")
    void shouldThrowExceptionWhenNotFoundById() throws Exception {
      // Given
      IdType unknownId = new IdType("CodeSystem", "unknown");

      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(manager.cs().getCodeSystem(SCOPE_FHIR, unknownId))
          .thenThrow(new ResourceNotFoundException("Not found"));

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      ResourceNotFoundException exception =
          assertThrows(
              ResourceNotFoundException.class,
              () -> service.getCodeSystem(SCOPE_FHIR, unknownId),
              "Should throw ResourceNotFoundException when CodeSystem not found by ID");

      assertTrue(
          exception.getMessage().contains(unknownId.getIdPart()),
          "Exception message should contain the requested ID");
    }

    @Test
    @DisplayName("Should query multiple managers and return first match")
    void shouldQueryMultipleManagersById() throws Exception {
      // Given
      IdType csId = new IdType("CodeSystem", "loinc");
      CodeSystem expectedCs = createCodeSystem(LOINC_URL, List.of("1234-5"));
      expectedCs.setId(csId);

      TerminologyManager snomedManager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(snomedManager.cs().getCodeSystem(SCOPE_FHIR, csId, ResourceRetrievalMode.FULL))
          .thenThrow(new ResourceNotFoundException("Not found"));

      TerminologyManager loincManager =
          createMockManager("LOINC", Set.of(SCOPE_FHIR), List.of(LOINC_URL), List.of(), List.of());
      when(loincManager.cs().getCodeSystem(SCOPE_FHIR, csId, ResourceRetrievalMode.FULL))
          .thenReturn(expectedCs);

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(snomedManager, loincManager), config);

      // When
      CodeSystem result = service.getCodeSystem(SCOPE_FHIR, csId);

      // Then
      assertNotNull(result, "Should return CodeSystem from second manager");
      assertEquals(csId.getIdPart(), result.getIdElement().getIdPart(), "IDs should match");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when no managers registered")
    void shouldThrowExceptionWhenNoManagersById() throws Exception {
      // Given
      IdType csId = new IdType("CodeSystem", "snomed-ct");
      CombiningTerminologyService service = new CombiningTerminologyService(List.of(), config);

      // When/Then
      assertThrows(
          ResourceNotFoundException.class,
          () -> service.getCodeSystem(SCOPE_FHIR, csId),
          "Should throw ResourceNotFoundException when no managers are registered");
    }

    @Test
    @DisplayName("Should handle exceptions from managers gracefully")
    void shouldHandleManagerExceptionsById() throws Exception {
      // Given
      IdType csId = new IdType("CodeSystem", "snomed-ct");
      CodeSystem expectedCs = createCodeSystem(SNOMED_URL, List.of("123456789"));
      expectedCs.setId(csId);

      TerminologyManager failingManager =
          createMockManager(
              "Failing", Set.of(SCOPE_FHIR), List.of(ICD10_URL), List.of(), List.of());
      when(failingManager.cs().getCodeSystem(any(), any()))
          .thenThrow(new RuntimeException("Manager error"));

      TerminologyManager workingManager =
          createMockManager(
              "Working", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(workingManager.cs().getCodeSystem(SCOPE_FHIR, csId, ResourceRetrievalMode.FULL))
          .thenReturn(expectedCs);

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(failingManager, workingManager), config);

      // When
      CodeSystem result = service.getCodeSystem(SCOPE_FHIR, csId);

      // Then
      assertNotNull(result, "Should return result despite one manager failing");
      assertEquals(csId.getIdPart(), result.getIdElement().getIdPart(), "IDs should match");
    }

    @Test
    @DisplayName("Should respect scope parameter")
    void shouldRespectScopeById() throws Exception {
      // Given
      IdType csId = new IdType("CodeSystem", "snomed-ct");
      CodeSystem fhirCs = createCodeSystem(SNOMED_URL, List.of("123456789"));
      fhirCs.setId(csId);

      TerminologyManager manager =
          createMultiScopeManager("Multi", Set.of(SCOPE_FHIR, SCOPE_CUSTOM), List.of(SNOMED_URL));
      when(manager.cs().getCodeSystem(SCOPE_FHIR, csId, ResourceRetrievalMode.FULL))
          .thenReturn(fhirCs);
      when(manager.cs().getCodeSystem(SCOPE_CUSTOM, csId, ResourceRetrievalMode.FULL))
          .thenThrow(new ResourceNotFoundException("Not in custom scope"));

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When
      CodeSystem fhirResult = service.getCodeSystem(SCOPE_FHIR, csId);

      // Then
      assertNotNull(fhirResult, "Should return CodeSystem for fhir scope");

      // And when requesting from non-existent scope
      assertThrows(
          ResourceNotFoundException.class,
          () -> service.getCodeSystem(SCOPE_CUSTOM, csId),
          "Should throw exception when CodeSystem not in requested scope");
    }

    @Test
    @DisplayName("Should handle null IdType parameter")
    void shouldHandleNullId() throws Exception {
      // Given
      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          ResourceNotFoundException.class,
          () -> service.getCodeSystem(SCOPE_FHIR, null),
          "Should throw exception for null ID");
    }

    @Test
    @DisplayName("Should handle null scope parameter")
    void shouldHandleNullScopeById() throws Exception {
      // Given
      IdType csId = new IdType("CodeSystem", "snomed-ct");
      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          ResourceNotFoundException.class,
          () -> service.getCodeSystem(null, csId),
          "Should throw exception for null scope");
    }

    @Test
    @DisplayName("Should handle UnknownScopeException from managers")
    void shouldHandleUnknownScopeException() throws Exception {
      // Given
      IdType csId = new IdType("CodeSystem", "snomed-ct");

      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(manager.cs().getCodeSystem("unknown-scope", csId))
          .thenThrow(new UnknownScopeException("Unknown scope"));

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When/Then
      assertThrows(
          ResourceNotFoundException.class,
          () -> service.getCodeSystem("unknown-scope", csId),
          "Should throw ResourceNotFoundException when all managers throw UnknownScopeException");
    }

    @Test
    @DisplayName("Should handle ID with version")
    void shouldHandleIdWithVersion() throws Exception {
      // Given
      IdType csIdWithVersion = new IdType("CodeSystem", "snomed-ct", "2023-01");
      CodeSystem expectedCs = createCodeSystem(SNOMED_URL, List.of("123456789"));
      expectedCs.setId(csIdWithVersion);

      TerminologyManager manager =
          createMockManager(
              "SNOMED", Set.of(SCOPE_FHIR), List.of(SNOMED_URL), List.of(), List.of());
      when(manager.cs().getCodeSystem(SCOPE_FHIR, csIdWithVersion, ResourceRetrievalMode.FULL))
          .thenReturn(expectedCs);

      CombiningTerminologyService service =
          new CombiningTerminologyService(List.of(manager), config);

      // When
      CodeSystem result = service.getCodeSystem(SCOPE_FHIR, csIdWithVersion);

      // Then
      assertNotNull(result, "Should return CodeSystem with versioned ID");
      assertEquals(
          csIdWithVersion.getIdPart(), result.getIdElement().getIdPart(), "ID parts should match");
    }
  }
}

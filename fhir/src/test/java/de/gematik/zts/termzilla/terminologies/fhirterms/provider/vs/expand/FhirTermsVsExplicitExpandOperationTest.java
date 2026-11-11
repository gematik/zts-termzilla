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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.graph.FhirTermGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link FhirTermsVsExplicitExpandOperation}.
 *
 * <p>Tests verify the expansion logic for ValueSet include elements, including:
 *
 * <ul>
 *   <li>Version selection strategy determination
 *   <li>Parameter validation (force-system-version, check-system-version, system-version)
 *   <li>Delegation to extensional and intensional expansion methods
 * </ul>
 *
 * <p><b>Note:</b> To mock the methods {@code expandExtensionalInclude}, {@code
 * expandIntensionalInclude}, and {@code expandAllCodesFromCodeSystem}, their visibility has been
 * changed to package-protected.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FhirTermsVsExplicitExpandOperation Tests")
class FhirTermsVsExplicitExpandOperationTest {

  private static final String TEST_SYSTEM = "http://example.org/fhir/CodeSystem/test-cs";
  private static final String TEST_VERSION = "1.0.0";
  private static final String TEST_VERSION_2 = "2.0.0";
  private static final String TEST_SCOPE = "test-scope";

  @Mock private FhirTermsTerminologyManager fhirTermsTerminologyManager;
  @Mock private FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;
  @Mock private FhirTermGraphRepresentation fhirTermGraphRepresentation;

  private CodeSystem defaultCodeSystem;
  private CodeSystem alternativeCodeSystem;

  private FhirTermsVsExplicitExpandOperation expandOperation;

  @BeforeEach
  void setUp() {
    // GIVEN - Setup mock chain for constructor
    defaultCodeSystem = createTestCodeSystem(TEST_VERSION);
    alternativeCodeSystem = createTestCodeSystem(TEST_VERSION_2);

    when(fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation())
        .thenReturn(fhirTermsInMemoryRepresentation);

    expandOperation = spy(new FhirTermsVsExplicitExpandOperation(fhirTermsTerminologyManager));
  }

  @Nested
  @DisplayName("expandInclude Method Tests")
  class ExpandIncludeTests {

    @Nested
    @DisplayName("Version Selection Strategy Tests")
    class VersionSelectionStrategyTests {

      @Test
      @DisplayName(
          "Should use SYSTEM_DEFAULT strategy when no version parameters are provided and include has no version")
      void shouldUseSystemDefaultStrategyWhenNoVersionParametersProvided()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(defaultCodeSystem);

        // Mock the expansion methods to prevent actual execution
        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement,
                true, // includeInactiveConcepts
                null, // systemVersion
                null, // checkSystemVersion
                null, // forceSystemVersion
                null,
                TEST_SCOPE);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                TEST_SYSTEM + "|" + TEST_VERSION,
                VsExpandPartResult.VersionSelectionStrategy.SYSTEM_DEFAULT);
      }

      @Test
      @DisplayName("Should use DEFINED strategy when include element has version defined")
      void shouldUseDefinedStrategyWhenIncludeHasVersion()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class,
                TEST_SCOPE,
                TEST_SYSTEM,
                TEST_VERSION_2,
                ResourceRetrievalMode.FULL))
            .thenReturn(alternativeCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement,
                true,
                null, // systemVersion
                null, // checkSystemVersion
                null, // forceSystemVersion
                null,
                TEST_SCOPE);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                TEST_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.DEFINED);
      }

      @Test
      @DisplayName("Should use EXTERNALLY_SET strategy when system-version parameter is provided")
      void shouldUseExternallySetStrategyWhenSystemVersionProvided()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class,
                TEST_SCOPE,
                TEST_SYSTEM,
                TEST_VERSION_2,
                ResourceRetrievalMode.FULL))
            .thenReturn(alternativeCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement,
                true,
                TEST_VERSION_2, // systemVersion
                null, // checkSystemVersion
                null, // forceSystemVersion
                null,
                TEST_SCOPE);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                TEST_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_SET);
      }

      @Test
      @DisplayName(
          "Should use EXTERNALLY_CHECK strategy when check-system-version parameter is provided")
      void shouldUseExternallyCheckStrategyWhenCheckSystemVersionProvided()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class,
                TEST_SCOPE,
                TEST_SYSTEM,
                TEST_VERSION_2,
                ResourceRetrievalMode.FULL))
            .thenReturn(alternativeCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement,
                true,
                null, // systemVersion
                TEST_VERSION_2, // checkSystemVersion
                null, // forceSystemVersion
                null,
                TEST_SCOPE);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                TEST_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_CHECK);
      }

      @Test
      @DisplayName(
          "Should use EXTERNALLY_FORCE strategy when force-system-version parameter is provided")
      void shouldUseExternallyForceStrategyWhenForceSystemVersionProvided()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        assertForceStrategySelectionForDefaultInclude();
      }

      @Test
      @SuppressWarnings("java:S4144")
      @DisplayName(
          "Should prioritize EXTERNALLY_FORCE over DEFINED when both force-system-version and include version are provided")
      void shouldPrioritizeExternallyForceOverDefined()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);
        includeElement.addConcept().setCode("CODE-1");

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class,
                TEST_SCOPE,
                TEST_SYSTEM,
                TEST_VERSION,
                ResourceRetrievalMode.FULL))
            .thenReturn(defaultCodeSystem);

        doNothing()
            .when(expandOperation)
            .processExtensionalDefinition(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement, true, null, null, TEST_VERSION, "display", TEST_SCOPE);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                TEST_SYSTEM + "|" + TEST_VERSION,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_FORCE);

        verify(expandOperation)
            .processExtensionalDefinition(
                includeElement, "display", true, vsExpandIncludeResult, defaultCodeSystem);
      }
    }

    @Nested
    @DisplayName("Version Validation Tests")
    class VersionValidationTests {

      @Test
      @DisplayName(
          "Should throw InvalidRequestException when check-system-version does not match include version")
      void shouldThrowExceptionWhenCheckSystemVersionDoesNotMatchIncludeVersion() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);
        includeElement.setVersion(TEST_VERSION);

        // WHEN / THEN
        assertThatThrownBy(
                () ->
                    expandOperation.processIncludeOrExcludeElement(
                        includeElement,
                        true,
                        null,
                        TEST_VERSION_2, // checkSystemVersion differs from include version
                        null,
                        null,
                        TEST_SCOPE))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("check-system-version")
            .hasMessageContaining("does not match");
      }

      @Test
      @DisplayName("Should pass validation when check-system-version matches include version")
      void shouldPassValidationWhenCheckSystemVersionMatchesIncludeVersion()
          throws UnsupportedTerminologyException, UnknownScopeException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class,
                TEST_SCOPE,
                TEST_SYSTEM,
                TEST_VERSION_2,
                ResourceRetrievalMode.FULL))
            .thenReturn(alternativeCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN / THEN
        assertThatCode(
                () ->
                    expandOperation.processIncludeOrExcludeElement(
                        includeElement,
                        true,
                        null,
                        TEST_VERSION_2, // Matches include version
                        null,
                        null,
                        TEST_SCOPE))
            .doesNotThrowAnyException();
      }

      @Test
      @DisplayName(
          "Should throw InvalidRequestException when requested CodeSystem version is not found")
      void shouldThrowExceptionWhenCodeSystemNotFound()
          throws UnsupportedTerminologyException, UnknownScopeException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);
        includeElement.setVersion("99.99.99"); // Unsupported version

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, "99.99.99", ResourceRetrievalMode.FULL))
            .thenReturn(null);

        // WHEN / THEN
        assertThatThrownBy(
                () ->
                    expandOperation.processIncludeOrExcludeElement(
                        includeElement, true, null, null, null, null, TEST_SCOPE))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("not supported");
      }
    }

    @Nested
    @DisplayName("Supplement Validation Tests")
    class SupplementValidationTests {

      @Test
      @DisplayName(
          "Should throw InvalidRequestException when CodeSystem content mode is SUPPLEMENT")
      void shouldThrowExceptionWhenCodeSystemContentIsSupplement()
          throws UnsupportedTerminologyException, UnknownScopeException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        CodeSystem supplementCodeSystem = createTestCodeSystem(TEST_VERSION);
        supplementCodeSystem.setContent(CodeSystem.CodeSystemContentMode.SUPPLEMENT);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(supplementCodeSystem);

        // WHEN / THEN
        assertThatThrownBy(
                () ->
                    expandOperation.processIncludeOrExcludeElement(
                        includeElement, true, null, null, null, null, TEST_SCOPE))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("supplement")
            .hasMessageContaining("cannot be expanded")
            .hasMessageContaining(TEST_SYSTEM);
      }

      @Test
      @DisplayName("Should throw InvalidRequestException when CodeSystem has supplements defined")
      void shouldThrowExceptionWhenCodeSystemHasSupplements()
          throws UnsupportedTerminologyException, UnknownScopeException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        CodeSystem codeSystemWithSupplements = createTestCodeSystem(TEST_VERSION);
        codeSystemWithSupplements.setSupplements("http://example.org/fhir/CodeSystem/base-cs");

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(codeSystemWithSupplements);

        // WHEN / THEN
        assertThatThrownBy(
                () ->
                    expandOperation.processIncludeOrExcludeElement(
                        includeElement, true, null, null, null, null, TEST_SCOPE))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("supplement")
            .hasMessageContaining("cannot be expanded")
            .hasMessageContaining(TEST_SYSTEM);
      }

      @Test
      @DisplayName(
          "Should throw InvalidRequestException when CodeSystem has both SUPPLEMENT content and supplements reference")
      void shouldThrowExceptionWhenCodeSystemHasBothSupplementContentAndSupplementsReference()
          throws UnsupportedTerminologyException, UnknownScopeException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        CodeSystem supplementCodeSystem = createTestCodeSystem(TEST_VERSION);
        supplementCodeSystem.setContent(CodeSystem.CodeSystemContentMode.SUPPLEMENT);
        supplementCodeSystem.setSupplements("http://example.org/fhir/CodeSystem/base-cs");

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(supplementCodeSystem);

        // WHEN / THEN
        assertThatThrownBy(
                () ->
                    expandOperation.processIncludeOrExcludeElement(
                        includeElement, true, null, null, null, null, TEST_SCOPE))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("supplement")
            .hasMessageContaining("cannot be expanded");
      }

      @Test
      @DisplayName("Should not throw exception when CodeSystem has non-supplement content mode")
      void shouldNotThrowExceptionWhenCodeSystemHasNonSupplementContentMode()
          throws UnsupportedTerminologyException, UnknownScopeException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        CodeSystem completeCodeSystem = createTestCodeSystem(TEST_VERSION);
        completeCodeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(completeCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN / THEN
        assertThatCode(
                () ->
                    expandOperation.processIncludeOrExcludeElement(
                        includeElement, true, null, null, null, null, TEST_SCOPE))
            .doesNotThrowAnyException();
      }
    }

    @Nested
    @DisplayName("Expansion Delegation Tests")
    class ExpansionDelegationTests {

      @Test
      @DisplayName("Should delegate to expandExtensionalInclude when include has concepts")
      void shouldDelegateToExpandExtensionalIncludeWhenIncludeHasConcepts()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);
        includeElement.addConcept().setCode("CODE-1");

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(defaultCodeSystem);

        doNothing()
            .when(expandOperation)
            .processExtensionalDefinition(
                any(ValueSet.ConceptSetComponent.class),
                any(),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement, true, null, null, null, null, TEST_SCOPE);

        // THEN
        verify(expandOperation)
            .processExtensionalDefinition(
                includeElement, null, true, vsExpandIncludeResult, defaultCodeSystem);
      }

      @Test
      @DisplayName("Should delegate to expandIntensionalInclude when include has filters")
      void shouldDelegateToExpandIntensionalIncludeWhenIncludeHasFilters()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);
        includeElement
            .addFilter()
            .setProperty("status")
            .setOp(ValueSet.FilterOperator.EQUAL)
            .setValue("active");

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(defaultCodeSystem);

        doNothing()
            .when(expandOperation)
            .processIntensionalDefinition(
                any(ValueSet.ConceptSetComponent.class),
                any(),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class),
                anyString());

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement, true, null, null, null, null, TEST_SCOPE);

        // THEN
        verify(expandOperation)
            .processIntensionalDefinition(
                includeElement, null, true, vsExpandIncludeResult, defaultCodeSystem, TEST_SCOPE);
      }

      @Test
      @DisplayName(
          "Should delegate to expandAllCodesFromCodeSystem when include has no concepts and no filters")
      void shouldDelegateToExpandAllCodesWhenNoConceptsAndNoFilters()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(defaultCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement, true, null, null, null, null, TEST_SCOPE);

        // THEN
        verify(expandOperation)
            .processAllConcepts(
                includeElement, null, true, vsExpandIncludeResult, defaultCodeSystem);
      }

      @Test
      @DisplayName("Should pass correct version to expansion methods based on forceSystemVersion")
      void shouldPassCorrectVersionToExpansionMethodsBasedOnForceSystemVersion()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class,
                TEST_SCOPE,
                TEST_SYSTEM,
                TEST_VERSION_2,
                ResourceRetrievalMode.FULL))
            .thenReturn(alternativeCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.processIncludeOrExcludeElement(
                includeElement, false, null, null, TEST_VERSION_2, null, TEST_SCOPE);

        // THEN
        verify(expandOperation)
            .processAllConcepts(
                includeElement, null, false, vsExpandIncludeResult, alternativeCodeSystem);
      }
    }

    @Nested
    @DisplayName("CodeSystem Lookup Tests")
    class CodeSystemLookupTests {

      @Test
      @DisplayName(
          "Should call getCodeSystemByUrlAndVersion with null version when no version is specified")
      void shouldCallGetCodeSystemWithNullVersionWhenNoVersionSpecified()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL))
            .thenReturn(defaultCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        expandOperation.processIncludeOrExcludeElement(
            includeElement, true, null, null, null, null, TEST_SCOPE);

        // THEN
        verify(fhirTermsInMemoryRepresentation)
            .getResourceByUrlAndVersion(
                CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, null, ResourceRetrievalMode.FULL);
      }

      @Test
      @DisplayName(
          "Should call getCodeSystemByUrlAndVersion with specified version when version is defined")
      void shouldCallGetCodeSystemWithVersionWhenVersionSpecified()
          throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(TEST_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);

        when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
                CodeSystem.class,
                TEST_SCOPE,
                TEST_SYSTEM,
                TEST_VERSION_2,
                ResourceRetrievalMode.FULL))
            .thenReturn(alternativeCodeSystem);

        doNothing()
            .when(expandOperation)
            .processAllConcepts(
                any(ValueSet.ConceptSetComponent.class),
                nullable(String.class),
                anyBoolean(),
                any(VsExpandPartResult.class),
                any(CodeSystem.class));

        // WHEN
        expandOperation.processIncludeOrExcludeElement(
            includeElement, true, null, null, null, null, TEST_SCOPE);

        // THEN
        verify(fhirTermsInMemoryRepresentation)
            .getResourceByUrlAndVersion(
                CodeSystem.class,
                TEST_SCOPE,
                TEST_SYSTEM,
                TEST_VERSION_2,
                ResourceRetrievalMode.FULL);
      }
    }
  }

  @Nested
  @DisplayName("expandExtensionalInclude Method Tests")
  class ExpandExtensionalIncludeTests {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"   "})
    @DisplayName("Should include all explicit concepts when filter is null or blank")
    void shouldIncludeAllExplicitConceptsWhenFilterIsNullOrBlank(String filter)
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createExplicitInclude("CODE-1", "CODE-2");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processExtensionalDefinition(
          includeElement, filter, true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactlyInAnyOrder("CODE-1", "CODE-2");
    }

    @ParameterizedTest
    @CsvSource({
      "example.org/fhir/codesystem/test-cs, 2",
      "code-1, 1",
      "display match, 1",
      "definition match, 1",
      "designation match, 1"
    })
    @DisplayName("Should include concepts when filter is contained in supported fields")
    void shouldIncludeConceptsWhenFilterMatchesSupportedFields(String filter, int expectedMatches)
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createExplicitInclude("CODE-1", "CODE-2");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processExtensionalDefinition(
          includeElement, filter, true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos()).hasSize(expectedMatches);
    }

    @Test
    @DisplayName("Should exclude concepts when filter does not match any supported field")
    void shouldExcludeConceptsWhenFilterDoesNotMatchAnySupportedField()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createExplicitInclude("CODE-1", "CODE-2");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processExtensionalDefinition(
          includeElement, "not-present-anywhere", true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos()).isEmpty();
    }

    @Test
    @DisplayName("Should include concept when filter matches concept reference display")
    void shouldIncludeConceptWhenFilterMatchesConceptReferenceDisplay()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createReferenceOnlyMatchingCodeSystem();
      ValueSet.ConceptSetComponent includeElement =
          createExplicitIncludeWithDisplay("CODE-REF-1", "Reference Display Match");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processExtensionalDefinition(
          includeElement, "display match", true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-REF-1");
    }

    @Test
    @DisplayName("Should include concept when filter matches concept reference designation")
    void shouldIncludeConceptWhenFilterMatchesConceptReferenceDesignation()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createReferenceOnlyMatchingCodeSystem();
      ValueSet.ConceptSetComponent includeElement =
          createExplicitIncludeWithDesignation("CODE-REF-1", "Reference Designation Match");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processExtensionalDefinition(
          includeElement, "designation match", true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-REF-1");
    }

    @Test
    @DisplayName(
        "Should continue loop when neither concept reference nor concept definition matches filter")
    void shouldContinueLoopWhenNeitherReferenceNorDefinitionMatches()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createReferenceOnlyMatchingCodeSystem();
      ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
      includeElement.setSystem(TEST_SYSTEM);
      includeElement.addConcept().setCode("CODE-REF-1").setDisplay("Reference Display Match");
      includeElement.addConcept().setCode("CODE-REF-2").setDisplay("Other Reference Display");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processExtensionalDefinition(
          includeElement, "display match", true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-REF-1");
    }
  }

  @Nested
  @DisplayName("expandIntensionalInclude Method Tests")
  class ExpandIntensionalIncludeTests {

    @Test
    @DisplayName("Should include matching code for equals filter")
    void shouldIncludeMatchingCodeForEqualsFilter() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createEqualsCodeFilterInclude();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, null, true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-1");
    }

    @Test
    @DisplayName("Should apply resultFilter for equals filter")
    void shouldApplyResultFilterForEqualsFilter() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createEqualsCodeFilterInclude();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, "definition match", true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-1");
    }

    @Test
    @DisplayName("Should resolve exists child filter via graph and apply resultFilter")
    void shouldResolveExistsChildFilterViaGraphAndApplyResultFilter()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createExistsChildFilterInclude();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      when(fhirTermsInMemoryRepresentation.getGraphForCodeSystem(codeSystem))
          .thenReturn(fhirTermGraphRepresentation);
      when(fhirTermGraphRepresentation.getConceptsByRelationshipExistence(true, true))
          .thenReturn(new HashSet<>(Set.of("CODE-1")));

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, "display match", true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-1");
    }

    @Test
    @DisplayName(
        "Should return no results for exists child filter when resultFilter does not match")
    void shouldReturnNoResultsForExistsChildFilterWhenResultFilterDoesNotMatch()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createExistsChildFilterInclude();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      when(fhirTermsInMemoryRepresentation.getGraphForCodeSystem(codeSystem))
          .thenReturn(fhirTermGraphRepresentation);
      when(fhirTermGraphRepresentation.getConceptsByRelationshipExistence(true, true))
          .thenReturn(new HashSet<>(Set.of("CODE-1")));

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, "not-present-anywhere", true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos()).isEmpty();
    }

    @Test
    @DisplayName("Should resolve exists parent filter via graph and apply resultFilter")
    void shouldResolveExistsParentFilterViaGraphAndApplyResultFilter()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createExistsParentFilterInclude();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      when(fhirTermsInMemoryRepresentation.getGraphForCodeSystem(codeSystem))
          .thenReturn(fhirTermGraphRepresentation);
      when(fhirTermGraphRepresentation.getConceptsByRelationshipExistence(false, true))
          .thenReturn(new HashSet<>(Set.of("CODE-2")));

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, "other display", true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-2");
    }

    @Test
    @DisplayName(
        "Should return no results for exists parent filter when resultFilter does not match")
    void shouldReturnNoResultsForExistsParentFilterWhenResultFilterDoesNotMatch()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      ValueSet.ConceptSetComponent includeElement = createExistsParentFilterInclude();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      when(fhirTermsInMemoryRepresentation.getGraphForCodeSystem(codeSystem))
          .thenReturn(fhirTermGraphRepresentation);
      when(fhirTermGraphRepresentation.getConceptsByRelationshipExistence(false, true))
          .thenReturn(new HashSet<>(Set.of("CODE-2")));

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, "not-present-anywhere", true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos()).isEmpty();
    }

    @Test
    @DisplayName("Should return concepts with display for exists display=true filter")
    void shouldReturnConceptsWithDisplayForExistsDisplayTrueFilter()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createExistsPropertyTestCodeSystem();
      ValueSet.ConceptSetComponent includeElement =
          createExistsPropertyFilterInclude("display", "true");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, null, true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactlyInAnyOrder("CODE-1", "CODE-2");
    }

    @Test
    @DisplayName("Should return concepts without display for exists display=false filter")
    void shouldReturnConceptsWithoutDisplayForExistsDisplayFalseFilter()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createExistsPropertyTestCodeSystem();
      ValueSet.ConceptSetComponent includeElement =
          createExistsPropertyFilterInclude("display", "false");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, null, true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-3");
    }

    @Test
    @DisplayName("Should return concepts with explicit property for exists=true filter")
    void shouldReturnConceptsWithExplicitPropertyForExistsTrueFilter()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createExistsPropertyTestCodeSystem();
      ValueSet.ConceptSetComponent includeElement =
          createExistsPropertyFilterInclude("custom-prop", "true");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, null, true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-1");
    }

    @Test
    @DisplayName("Should return concepts without explicit property for exists=false filter")
    void shouldReturnConceptsWithoutExplicitPropertyForExistsFalseFilter()
        throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createExistsPropertyTestCodeSystem();
      ValueSet.ConceptSetComponent includeElement =
          createExistsPropertyFilterInclude("custom-prop", "false");
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processIntensionalDefinition(
          includeElement, null, true, result, codeSystem, TEST_SCOPE);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactlyInAnyOrder("CODE-2", "CODE-3");
    }
  }

  @Nested
  @DisplayName("expandAllCodesFromCodeSystem Method Tests")
  class ExpandAllCodesFromCodeSystemTests {

    @Test
    @DisplayName("Should include all concepts when resultFilter is null")
    void shouldIncludeAllConceptsWhenResultFilterIsNull() {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
      includeElement.setSystem(TEST_SYSTEM);
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processAllConcepts(includeElement, null, true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactlyInAnyOrder("CODE-1", "CODE-2");
    }

    @Test
    @DisplayName("Should filter all-codes expansion by resultFilter")
    void shouldFilterAllCodesExpansionByResultFilter() {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
      includeElement.setSystem(TEST_SYSTEM);
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processAllConcepts(includeElement, "display match", true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("CODE-1");
    }

    @Test
    @DisplayName("Should return empty result when resultFilter does not match")
    void shouldReturnEmptyResultWhenResultFilterDoesNotMatch() {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
      includeElement.setSystem(TEST_SYSTEM);
      CodeSystem codeSystem = createFilterAwareCodeSystem();
      VsExpandPartResult result = createVsProcessIncludeOrExcludeElementResult();

      // WHEN
      expandOperation.processAllConcepts(
          includeElement, "absent-fragment", true, result, codeSystem);

      // THEN
      assertThat(result.getConceptBaseInfos()).isEmpty();
    }
  }

  // ------------------------------------------------------------------------------------------------
  // Helper Methods
  // ------------------------------------------------------------------------------------------------

  /**
   * Creates a new {@link VsExpandPartResult} for testing.
   *
   * @return a new VsExpandPartResult for testing
   */
  private VsExpandPartResult createVsProcessIncludeOrExcludeElementResult() {
    return new VsExpandPartResult();
  }

  /**
   * Creates a test CodeSystem with the specified version.
   *
   * @param version the CodeSystem version
   * @return a configured CodeSystem for testing
   */
  private CodeSystem createTestCodeSystem(String version) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setUrl(TEST_SYSTEM);
    codeSystem.setVersion(version);
    codeSystem.setId("test-codesystem-" + version);

    // Add a property definition for filters to work
    codeSystem.addProperty().setCode("status").setType(CodeSystem.PropertyType.CODE);

    // Add some test concepts
    codeSystem.addConcept().setCode("CODE-1").setDisplay("Test Code 1");
    codeSystem.addConcept().setCode("CODE-2").setDisplay("Test Code 2");

    return codeSystem;
  }

  private ValueSet.ConceptSetComponent createExplicitInclude(String... codes) {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(TEST_SYSTEM);

    for (String code : codes) {
      includeElement.addConcept().setCode(code);
    }

    return includeElement;
  }

  private ValueSet.ConceptSetComponent createEqualsCodeFilterInclude() {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(TEST_SYSTEM);
    includeElement
        .addFilter()
        .setProperty("code")
        .setOp(ValueSet.FilterOperator.EQUAL)
        .setValue("CODE-1");
    return includeElement;
  }

  private ValueSet.ConceptSetComponent createExistsChildFilterInclude() {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(TEST_SYSTEM);
    includeElement
        .addFilter()
        .setProperty("child")
        .setOp(ValueSet.FilterOperator.EXISTS)
        .setValue("true");
    return includeElement;
  }

  private ValueSet.ConceptSetComponent createExistsParentFilterInclude() {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(TEST_SYSTEM);
    includeElement
        .addFilter()
        .setProperty("parent")
        .setOp(ValueSet.FilterOperator.EXISTS)
        .setValue("true");
    return includeElement;
  }

  private CodeSystem createFilterAwareCodeSystem() {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setUrl(TEST_SYSTEM);
    codeSystem.setVersion(TEST_VERSION);
    codeSystem.setId("filter-codesystem");
    codeSystem.addProperty().setCode("code").setType(CodeSystem.PropertyType.CODE);
    codeSystem.addProperty().setCode("child").setType(CodeSystem.PropertyType.CODE);
    codeSystem.addProperty().setCode("parent").setType(CodeSystem.PropertyType.CODE);

    CodeSystem.ConceptDefinitionComponent matchingConcept =
        codeSystem.addConcept().setCode("CODE-1").setDisplay("Display Match");
    matchingConcept.setDefinition("Definition Match");
    matchingConcept.addDesignation().setValue("Designation Match");

    codeSystem.addConcept().setCode("CODE-2").setDisplay("Other Display");

    return codeSystem;
  }

  private ValueSet.ConceptSetComponent createExplicitIncludeWithDisplay(
      String code, String display) {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(TEST_SYSTEM);
    includeElement.addConcept().setCode(code).setDisplay(display);
    return includeElement;
  }

  private ValueSet.ConceptSetComponent createExplicitIncludeWithDesignation(
      String code, String designationValue) {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(TEST_SYSTEM);
    includeElement.addConcept().setCode(code).addDesignation().setValue(designationValue);
    return includeElement;
  }

  private CodeSystem createReferenceOnlyMatchingCodeSystem() {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setUrl(TEST_SYSTEM);
    codeSystem.setVersion(TEST_VERSION);
    codeSystem.setId("reference-filter-codesystem");

    codeSystem.addConcept().setCode("CODE-REF-1").setDisplay("No Match").setDefinition("No Match");
    codeSystem.addConcept().setCode("CODE-REF-2").setDisplay("No Match").setDefinition("No Match");

    return codeSystem;
  }

  private ValueSet.ConceptSetComponent createExistsPropertyFilterInclude(
      String property, String value) {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(TEST_SYSTEM);
    includeElement
        .addFilter()
        .setProperty(property)
        .setOp(ValueSet.FilterOperator.EXISTS)
        .setValue(value);
    return includeElement;
  }

  private CodeSystem createExistsPropertyTestCodeSystem() {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setUrl(TEST_SYSTEM);
    codeSystem.setVersion(TEST_VERSION);
    codeSystem.setId("exists-property-codesystem");
    codeSystem.addProperty().setCode("display").setType(CodeSystem.PropertyType.STRING);
    codeSystem.addProperty().setCode("custom-prop").setType(CodeSystem.PropertyType.STRING);

    // CODE-1: hat Display und custom-prop
    CodeSystem.ConceptDefinitionComponent concept1 =
        codeSystem.addConcept().setCode("CODE-1").setDisplay("Display 1");
    concept1
        .addProperty()
        .setCode("custom-prop")
        .setValue(new org.hl7.fhir.r4.model.StringType("value-1"));

    // CODE-2: hat Display, aber kein custom-prop
    codeSystem.addConcept().setCode("CODE-2").setDisplay("Display 2");

    // CODE-3: kein Display, kein custom-prop
    codeSystem.addConcept().setCode("CODE-3");

    return codeSystem;
  }

  private void assertForceStrategySelectionForDefaultInclude()
      throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(TEST_SYSTEM);

    when(fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
            CodeSystem.class, TEST_SCOPE, TEST_SYSTEM, TEST_VERSION, ResourceRetrievalMode.FULL))
        .thenReturn(defaultCodeSystem);

    VsExpandPartResult vsExpandIncludeResult =
        expandOperation.processIncludeOrExcludeElement(
            includeElement, true, null, null, TEST_VERSION, null, TEST_SCOPE);

    assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
        .containsEntry(
            TEST_SYSTEM + "|" + TEST_VERSION,
            VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_FORCE);
  }
}

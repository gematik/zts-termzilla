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
package de.gematik.zts.termzilla.terminologies.snomed.provider.vs.expand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.parameters.ProcessingContext;
import de.gematik.zts.termzilla.api.parameters.VsExpandIncludeParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedValueSetService;
import de.gematik.zts.termzilla.terminologies.snomed.config.SnomedConfigProperties;
import de.gematik.zts.termzilla.terminologies.snomed.graph.SnomedGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.memory.SnomedInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.RefSet;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedLanguage;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.scg.DemisSCGProcessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.snomed.languages.scg.SCGExpressionParser;
import org.snomed.languages.scg.domain.model.Expression;

/**
 * Unit tests for {@link SnomedVsExplicitExpandOperation}.
 *
 * <p>Tests verify the expansion logic for ValueSet include elements, including:
 *
 * <ul>
 *   <li>Version selection strategy determination
 *   <li>Parameter validation (force-system-version, check-system-version, system-version)
 *   <li>Delegation to extensional and intensional expansion methods
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SnomedVsExplicitExpandOperation Tests")
class SnomedVsExplicitExpandOperationTest {

  private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
  private static final String TEST_MODULE_ID = "900000000000207008";
  private static final String TEST_DATE = "20240101";
  private static final String TEST_VERSION =
      "http://snomed.info/sct/" + TEST_MODULE_ID + "/version/" + TEST_DATE;
  private static final String TEST_DATE_2 = "20230701";
  private static final String TEST_VERSION_2 =
      "http://snomed.info/sct/" + TEST_MODULE_ID + "/version/" + TEST_DATE_2;

  @Mock private SnomedValueSetService snomedValueSetService;
  @Mock private SnomedTerminologyManager snomedTerminologyManager;
  @Mock private Snomed snomed;
  @Mock private SnomedCodeSystemService snomedCodeSystemService;
  @Mock private SCGExpressionParser expressionParser;
  @Mock private DemisSCGProcessor scgProcessor;
  @Mock private SnomedGraphRepresentation snomedGraphRepresentation;
  @Mock private SnomedInMemoryRepresentation snomedInMemoryRepresentation;
  @Mock private SnomedConfigProperties snomedConfigProperties;

  private Map<String, SnomedReleaseVersion> fhirVersionToSnomedReleaseVersionMap;
  private SnomedReleaseVersion defaultReleaseVersion;
  private SnomedReleaseVersion testReleaseVersion;

  private SnomedVsExplicitExpandOperation expandOperation;

  @BeforeEach
  void setUp() throws Exception {
    // GIVEN - Setup mock chain for constructor
    defaultReleaseVersion = new SnomedReleaseVersion(TEST_MODULE_ID, TEST_DATE, new HashMap<>());

    testReleaseVersion = new SnomedReleaseVersion(TEST_MODULE_ID, TEST_DATE_2, new HashMap<>());

    fhirVersionToSnomedReleaseVersionMap = new HashMap<>();
    fhirVersionToSnomedReleaseVersionMap.put(TEST_VERSION, defaultReleaseVersion);
    fhirVersionToSnomedReleaseVersionMap.put(TEST_VERSION_2, testReleaseVersion);

    when(snomedValueSetService.getSnomedTerminologyManager()).thenReturn(snomedTerminologyManager);
    when(snomedValueSetService.getSnomed()).thenReturn(snomed);
    when(snomedTerminologyManager.cs()).thenReturn(snomedCodeSystemService);
    lenient().when(snomedTerminologyManager.isExpressionProcessingEnabled()).thenReturn(true);
    lenient().when(snomedTerminologyManager.getExpressionParser()).thenReturn(expressionParser);
    lenient().when(snomedTerminologyManager.getScgProcessor()).thenReturn(scgProcessor);
    when(snomedCodeSystemService.getFhirVersionToSnomedReleaseVersionMap())
        .thenReturn(fhirVersionToSnomedReleaseVersionMap);
    lenient().when(snomed.getDefaultSnomedReleaseVersion()).thenReturn(defaultReleaseVersion);
    lenient().when(snomed.getSnomedGraph()).thenReturn(snomedGraphRepresentation);
    lenient()
        .when(snomed.getSnomedInMemoryRepresentation())
        .thenReturn(snomedInMemoryRepresentation);
    lenient().when(snomedTerminologyManager.getProperties()).thenReturn(snomedConfigProperties);
    lenient().when(snomedConfigProperties.isContextExtensionInDesignations()).thenReturn(false);

    expandOperation = spy(new SnomedVsExplicitExpandOperation(snomedValueSetService));
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
      void shouldUseSystemDefaultStrategyWhenNoVersionParametersProvided() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(
                includeElement,
                true, // includeInactiveConcepts
                null, // systemVersion
                null, // checkSystemVersion
                null); // forceSystemVersion

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                SNOMED_SYSTEM + "|" + TEST_VERSION,
                VsExpandPartResult.VersionSelectionStrategy.SYSTEM_DEFAULT);
      }

      @Test
      @DisplayName("Should use DEFINED strategy when include element has version defined")
      void shouldUseDefinedStrategyWhenIncludeHasVersion() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(
                includeElement,
                true,
                null, // systemVersion
                null, // checkSystemVersion
                null); // forceSystemVersion

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                SNOMED_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.DEFINED);
      }

      @Test
      @DisplayName("Should use EXTERNALLY_SET strategy when system-version parameter is provided")
      void shouldUseExternallySetStrategyWhenSystemVersionProvided() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(
                includeElement,
                true,
                TEST_VERSION_2, // systemVersion
                null, // checkSystemVersion
                null); // forceSystemVersion

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                SNOMED_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_SET);
      }

      @Test
      @DisplayName(
          "Should use EXTERNALLY_CHECK strategy when check-system-version parameter is provided")
      void shouldUseExternallyCheckStrategyWhenCheckSystemVersionProvided() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(
                includeElement,
                true,
                null, // systemVersion
                TEST_VERSION_2, // checkSystemVersion
                null); // forceSystemVersion

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                SNOMED_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_CHECK);
      }

      @Test
      @DisplayName(
          "Should use EXTERNALLY_FORCE strategy when force-system-version parameter is provided")
      void shouldUseExternallyForceStrategyWhenForceSystemVersionProvided() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(
                includeElement,
                true,
                null, // systemVersion
                null, // checkSystemVersion
                TEST_VERSION); // forceSystemVersion

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                SNOMED_SYSTEM + "|" + TEST_VERSION,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_FORCE);
      }

      @Test
      @DisplayName(
          "Should prioritize EXTERNALLY_FORCE over DEFINED when both force-system-version and include version are provided")
      void shouldPrioritizeExternallyForceOverDefined() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2); // Should be ignored due to force
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(
                includeElement,
                true,
                null,
                null,
                TEST_VERSION); // forceSystemVersion takes priority

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                SNOMED_SYSTEM + "|" + TEST_VERSION,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_FORCE);
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
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.setVersion(TEST_VERSION);
        includeElement.addConcept().setCode("12345678901");

        // WHEN / THEN
        assertThatThrownBy(
                () ->
                    expandProvidedInclude(
                        includeElement,
                        true,
                        null,
                        TEST_VERSION_2, // checkSystemVersion differs from include version
                        null))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("check-system-version")
            .hasMessageContaining("does not match");
      }

      @Test
      @DisplayName("Should pass validation when check-system-version matches include version")
      void shouldPassValidationWhenCheckSystemVersionMatchesIncludeVersion() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN / THEN
        assertThatCode(
                () ->
                    expandProvidedInclude(
                        includeElement,
                        true,
                        null,
                        TEST_VERSION_2, // Matches include version
                        null))
            .doesNotThrowAnyException();
      }

      @Test
      @DisplayName("Should throw InvalidRequestException when requested version is not supported")
      void shouldThrowExceptionWhenVersionNotSupported() {
        // GIVEN
        String unsupportedVersion = "http://snomed.info/sct/900000000000207008/version/99991231";
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.setVersion(unsupportedVersion);
        includeElement.addConcept().setCode("12345678901");

        // WHEN / THEN
        assertThatThrownBy(() -> expandProvidedInclude(includeElement, true, null, null, null))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("ValueSet Expansion does not support the requested version");
      }
    }

    @Nested
    @DisplayName("Expansion Delegation Tests")
    class ExpansionDelegationTests {

      @Test
      @DisplayName("Should delegate to expandExtensionalInclude when include has concepts")
      void shouldDelegateToExpandExtensionalIncludeWhenIncludeHasConcepts() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(includeElement, true, null, null, null);

        // THEN
        verify(expandOperation)
            .expandExtensionalInclude(
                eq(includeElement),
                eq(defaultReleaseVersion),
                nullable(String.class),
                eq(true),
                eq(vsExpandIncludeResult.getExplicitConceptReferences()),
                eq(vsExpandIncludeResult.getConceptBaseInfos()));
      }

      @Test
      @DisplayName(
          "Should delegate to expandIntensionalInclude when include has filter but no concepts")
      void shouldDelegateToExpandIntensionalIncludeWhenIncludeHasFilter() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement
            .addFilter()
            .setProperty("concept")
            .setOp(ValueSet.FilterOperator.ISA)
            .setValue("12345678901");

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(includeElement, true, null, null, null);

        // THEN
        verify(expandOperation)
            .expandIntensionalInclude(
                eq(includeElement),
                eq(defaultReleaseVersion),
                nullable(String.class),
                eq(true),
                eq(vsExpandIncludeResult.getConceptBaseInfos()));
      }

      @Test
      @DisplayName(
          "Should throw InvalidRequestException when include has neither concepts nor filter")
      void shouldThrowExceptionWhenIncludeHasNeitherConceptsNorFilter() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        // No concepts and no filter - this means all SNOMED concepts which is not supported

        // WHEN / THEN
        assertThatThrownBy(() -> expandProvidedInclude(includeElement, true, null, null, null))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining(
                "ValueSet Expansion currently requires either a concept or a filter in the compose-element");
      }

      @Test
      @DisplayName(
          "Should pass correct SnomedReleaseVersion to expansion methods based on forceSystemVersion")
      void shouldPassCorrectVersionToExpansionMethodsBasedOnForceSystemVersion() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandProvidedInclude(includeElement, false, null, null, TEST_VERSION_2);

        // THEN
        verify(expandOperation)
            .expandExtensionalInclude(
                eq(includeElement),
                eq(testReleaseVersion),
                nullable(String.class),
                eq(false),
                eq(vsExpandIncludeResult.getExplicitConceptReferences()),
                eq(vsExpandIncludeResult.getConceptBaseInfos()));
      }
    }

    @Nested
    @DisplayName("Default Version Lookup Tests")
    class DefaultVersionLookupTests {

      @Test
      @DisplayName("Should call getDefaultSnomedReleaseVersion when no version is specified")
      void shouldCallGetDefaultSnomedReleaseVersionWhenNoVersionSpecified() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        expandProvidedInclude(includeElement, true, null, null, null);

        // THEN
        verify(snomed).getDefaultSnomedReleaseVersion();
      }

      @Test
      @DisplayName("Should use fhirVersionToSnomedReleaseVersionMap when version is specified")
      void shouldUseFhirVersionMapWhenVersionSpecified() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(SNOMED_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);
        includeElement.addConcept().setCode("12345678901");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(SnomedReleaseVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        expandProvidedInclude(includeElement, true, null, null, null);

        // THEN - Verify the correct version was passed to the expansion method
        verify(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                eq(testReleaseVersion),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));
      }
    }
  }

  @Nested
  @DisplayName("expandExtensionalInclude Method Tests")
  class ExpandExtensionalIncludeTests {

    @Test
    @DisplayName("Should add active concept and explicit reference when code can be resolved")
    void shouldAddActiveConceptAndExplicitReferenceWhenCodeCanBeResolved()
        throws UnknownConceptException {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude("12345678901");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      SnomedConcept snomedConcept = createSnomedConcept("12345678901", true);

      when(snomed.getSnomedConcept("12345678901", defaultReleaseVersion, true))
          .thenReturn(snomedConcept);

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("12345678901");
      assertThat(vsExpandIncludeResult.getExplicitConceptReferences().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should skip concept when result filter does not match reference or concept")
    void shouldSkipConceptWhenResultFilterDoesNotMatchReferenceOrConcept()
        throws UnknownConceptException {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude("12345678901");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      SnomedConcept snomedConcept = createSnomedConcept("12345678901", true);

      when(snomed.getSnomedConcept("12345678901", defaultReleaseVersion, true))
          .thenReturn(snomedConcept);

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultReleaseVersion,
          "not-present-anywhere",
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos()).isEmpty();
      assertThat(vsExpandIncludeResult.getExplicitConceptReferences().size()).isZero();
    }

    @Test
    @DisplayName("Should not add inactive concept when includeInactiveConcepts is false")
    void shouldNotAddInactiveConceptWhenIncludeInactiveConceptsIsFalse()
        throws UnknownConceptException {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude("12345678901");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      SnomedConcept snomedConcept = createSnomedConcept("12345678901", false);

      when(snomed.getSnomedConcept("12345678901", defaultReleaseVersion, true))
          .thenReturn(snomedConcept);

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          false,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos()).isEmpty();
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when a concept entry has no code")
    void shouldThrowInvalidRequestExceptionWhenConceptEntryHasNoCode() {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
      includeElement.setSystem(SNOMED_SYSTEM);
      includeElement.addConcept();
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferences =
          vsExpandIncludeResult.getExplicitConceptReferences();
      SortedSet<ConceptBaseInfo> conceptBaseInfos = vsExpandIncludeResult.getConceptBaseInfos();

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  expandOperation.expandExtensionalInclude(
                      includeElement,
                      defaultReleaseVersion,
                      null,
                      true,
                      explicitConceptReferences,
                      conceptBaseInfos))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Missing code");
    }

    @Test
    @DisplayName("Should add expression when concept code is unknown but expression is valid")
    void shouldAddExpressionWhenConceptCodeIsUnknownButExpressionIsValid() throws Exception {
      // GIVEN
      String expressionCode = "404684003 |Clinical finding|";
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude(expressionCode);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      Expression expression = mock(Expression.class);

      when(snomed.getSnomedConcept(expressionCode, defaultReleaseVersion, true))
          .thenThrow(new UnknownConceptException("unknown concept"));
      when(expressionParser.parseExpression(expressionCode)).thenReturn(expression);

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly(expressionCode);
      assertThat(vsExpandIncludeResult.getExplicitConceptReferences().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should skip expression when result filter does not match expression or reference")
    void shouldSkipExpressionWhenResultFilterDoesNotMatchExpressionOrReference() throws Exception {
      // GIVEN
      String expressionCode = "404684003 |Clinical finding|";
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude(expressionCode);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      Expression expression = mock(Expression.class);

      when(snomed.getSnomedConcept(expressionCode, defaultReleaseVersion, true))
          .thenThrow(new UnknownConceptException("unknown concept"));
      when(expressionParser.parseExpression(expressionCode)).thenReturn(expression);

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultReleaseVersion,
          "not-matching",
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos()).isEmpty();
      assertThat(vsExpandIncludeResult.getExplicitConceptReferences().size()).isZero();
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when expression validation reports unknown concept")
    void shouldThrowInvalidRequestExceptionWhenExpressionValidationReportsUnknownConcept()
        throws Exception {
      // GIVEN
      String expressionCode = "404684003 |Clinical finding|";
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude(expressionCode);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      Expression expression = mock(Expression.class);

      when(snomed.getSnomedConcept(expressionCode, defaultReleaseVersion, true))
          .thenThrow(new UnknownConceptException("unknown concept"));
      when(expressionParser.parseExpression(expressionCode)).thenReturn(expression);
      doThrow(new UnknownConceptException("unknown in expression"))
          .when(scgProcessor)
          .validateAgainstMRCM(expression, defaultReleaseVersion);
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferences =
          vsExpandIncludeResult.getExplicitConceptReferences();
      SortedSet<ConceptBaseInfo> conceptBaseInfos = vsExpandIncludeResult.getConceptBaseInfos();

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  expandOperation.expandExtensionalInclude(
                      includeElement,
                      defaultReleaseVersion,
                      null,
                      true,
                      explicitConceptReferences,
                      conceptBaseInfos))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("expression contains a concept that is unknown");
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when expression is valid but expression processing is disabled")
    void shouldThrowWhenExpressionProcessingIsDisabledForExtensionalInclude() throws Exception {
      // GIVEN
      String expressionCode = "404684003 |Clinical finding|";
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude(expressionCode);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(snomed.getSnomedConcept(expressionCode, defaultReleaseVersion, true))
          .thenThrow(new UnknownConceptException("unknown concept"));
      when(snomedTerminologyManager.isExpressionProcessingEnabled()).thenReturn(false);

      ArrayListValuedHashMap<Integer, ConceptReferenceComponent> explicitConceptReferences =
          vsExpandIncludeResult.getExplicitConceptReferences();
      SortedSet<ConceptBaseInfo> conceptBaseInfos = vsExpandIncludeResult.getConceptBaseInfos();
      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  expandOperation.expandExtensionalInclude(
                      includeElement,
                      defaultReleaseVersion,
                      null,
                      true,
                      explicitConceptReferences,
                      conceptBaseInfos))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessage(
              String.format(
                  SnomedConstants.EXPRESSION_PROCESSING_DISABLED_MESSAGE, expressionCode));

      verify(expressionParser, never()).parseExpression(any());
    }
  }

  @Nested
  @DisplayName("expandIntensionalInclude Method Tests")
  class ExpandIntensionalIncludeTests {

    @Test
    @DisplayName("Should resolve concepts from constraint filter and add them to result")
    void shouldResolveConceptsFromConstraintFilterAndAddThemToResult() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createConstraintInclude("<<138875005");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(snomed.resolveExpressionConstraint("<<138875005", defaultReleaseVersion))
          .thenReturn(Set.of("100", "200"));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          true,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactlyInAnyOrder("100", "200");
    }

    @Test
    @DisplayName("Should apply AND semantics when multiple filters are provided")
    void shouldApplyAndSemanticsWhenMultipleFiltersAreProvided() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createConstraintInclude("first", "second");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(snomed.resolveExpressionConstraint("first", defaultReleaseVersion))
          .thenReturn(Set.of("100", "200"));
      when(snomed.resolveExpressionConstraint("second", defaultReleaseVersion))
          .thenReturn(Set.of("200", "300"));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          true,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("200");
    }

    @Test
    @DisplayName("Should apply result filter to intensional include result")
    void shouldApplyResultFilterToIntensionalIncludeResult() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createConstraintInclude("<<138875005");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(snomed.resolveExpressionConstraint("<<138875005", defaultReleaseVersion))
          .thenReturn(Set.of("100", "200"));
      when(snomed.getSnomedConceptWithDescription("100", defaultReleaseVersion, true))
          .thenReturn(createSnomedConcept("100", true));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultReleaseVersion,
          "200",
          true,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("200");
    }

    @Test
    @DisplayName("Should throw InvalidRequestException for unsupported filter property")
    void shouldThrowInvalidRequestExceptionForUnsupportedFilterProperty() {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
      includeElement.setSystem(SNOMED_SYSTEM);
      includeElement
          .addFilter()
          .setProperty("unsupported")
          .setOp(ValueSet.FilterOperator.EQUAL)
          .setValue("123");

      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      SortedSet<ConceptBaseInfo> conceptBaseInfos = vsExpandIncludeResult.getConceptBaseInfos();

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  expandOperation.expandIntensionalInclude(
                      includeElement, defaultReleaseVersion, null, true, conceptBaseInfos))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported property");
    }

    @Test
    @DisplayName("Should resolve concepts for concept is-a filter including self")
    void shouldResolveConceptsForConceptIsAFilterIncludingSelf() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement =
          createFilterInclude("concept", ValueSet.FilterOperator.ISA, "100");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(snomed.isConceptActive("100", defaultReleaseVersion)).thenReturn(true);
      when(snomedGraphRepresentation.getAllDescendantsOrSelf("100", defaultReleaseVersion))
          .thenReturn(Set.of("100", "101"));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          false,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactlyInAnyOrder("100", "101");
    }

    @Test
    @DisplayName("Should resolve concepts for concept descendent-of filter excluding self")
    void shouldResolveConceptsForConceptDescendentOfFilterExcludingSelf() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement =
          createFilterInclude("concept", ValueSet.FilterOperator.DESCENDENTOF, "100");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(snomed.isConceptActive("100", defaultReleaseVersion)).thenReturn(true);
      when(snomedGraphRepresentation.getAllDescendants("100", defaultReleaseVersion))
          .thenReturn(Set.of("101", "102"));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          false,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactlyInAnyOrder("101", "102");
    }

    @Test
    @DisplayName("Should resolve concepts for concept in filter using refset members")
    void shouldResolveConceptsForConceptInFilterUsingRefsetMembers() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement =
          createFilterInclude("concept", ValueSet.FilterOperator.IN, "refset-1");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      RefSet refSetMember = mock(RefSet.class);

      when(refSetMember.getReferencedComponentId()).thenReturn("999");
      when(snomedInMemoryRepresentation.getRefSetList("refset-1", defaultReleaseVersion, false))
          .thenReturn(List.of(refSetMember));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          false,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("999");
    }

    @Test
    @DisplayName("Should transform parent filter into childOf constraint")
    void shouldTransformParentFilterIntoChildOfConstraint() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement =
          createFilterInclude("parent", ValueSet.FilterOperator.EQUAL, "200");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(snomed.resolveExpressionConstraint("<!200", defaultReleaseVersion))
          .thenReturn(Set.of("100"));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          false,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("100");
    }

    @Test
    @DisplayName("Should transform child filter into parentOf constraint")
    void shouldTransformChildFilterIntoParentOfConstraint() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement =
          createFilterInclude("child", ValueSet.FilterOperator.EQUAL, "200");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(snomed.resolveExpressionConstraint(">!200", defaultReleaseVersion))
          .thenReturn(Set.of("300"));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultReleaseVersion,
          null,
          false,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("300");
    }

    @Test
    @DisplayName("Should throw InvalidRequestException for unsupported concept filter operator")
    void shouldThrowInvalidRequestExceptionForUnsupportedConceptFilterOperator() {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement =
          createFilterInclude("concept", ValueSet.FilterOperator.EQUAL, "123");
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
      SortedSet<ConceptBaseInfo> conceptBaseInfos = vsExpandIncludeResult.getConceptBaseInfos();

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  expandOperation.expandIntensionalInclude(
                      includeElement, defaultReleaseVersion, null, true, conceptBaseInfos))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported property/operation combination");
    }
  }

  @Nested
  @DisplayName("Restricted Mode Tests")
  class RestrictedModeTests {

    static Stream<Arguments> hierarchyBasedFilters() {
      return Stream.of(
          Arguments.of("constraint", ValueSet.FilterOperator.EQUAL, "<<138875005"),
          Arguments.of("parent", ValueSet.FilterOperator.EQUAL, "138875005"),
          Arguments.of("child", ValueSet.FilterOperator.EQUAL, "138875005"),
          Arguments.of("concept", ValueSet.FilterOperator.ISA, "138875005"),
          Arguments.of("concept", ValueSet.FilterOperator.DESCENDENTOF, "138875005"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("hierarchyBasedFilters")
    @DisplayName("Should reject hierarchy-based filters of provided ValueSets in restricted mode")
    void shouldRejectHierarchyBasedFiltersOfProvidedValueSetsInRestrictedMode(
        String property, ValueSet.FilterOperator operator, String value) {
      // GIVEN
      when(snomedTerminologyManager.isRestrictedMode()).thenReturn(true);
      ValueSet.ConceptSetComponent includeElement = createFilterInclude(property, operator, value);
      VsExpandIncludeParameter parameter = includeParameter(ProcessingContext.provided());

      // WHEN / THEN
      assertThatThrownBy(() -> expandOperation.expandInclude(includeElement, parameter))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("'" + property + " " + operator.toCode() + "'")
          .hasMessageContaining("restricted mode");
      verifyNoIntensionalExpansion();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("hierarchyBasedFilters")
    @DisplayName("Should allow hierarchy-based filters of stored ValueSets in restricted mode")
    void shouldAllowHierarchyBasedFiltersOfStoredValueSetsInRestrictedMode(
        String property, ValueSet.FilterOperator operator, String value) {
      // GIVEN
      lenient().when(snomedTerminologyManager.isRestrictedMode()).thenReturn(true);
      stubIntensionalExpansion();
      ValueSet.ConceptSetComponent includeElement = createFilterInclude(property, operator, value);

      // WHEN
      expandOperation.expandInclude(includeElement, includeParameter(ProcessingContext.stored()));

      // THEN
      verifyIntensionalExpansion(includeElement);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("hierarchyBasedFilters")
    @DisplayName(
        "Should allow hierarchy-based filters of provided ValueSets without restricted mode")
    void shouldAllowHierarchyBasedFiltersOfProvidedValueSetsWithoutRestrictedMode(
        String property, ValueSet.FilterOperator operator, String value) {
      // GIVEN
      when(snomedTerminologyManager.isRestrictedMode()).thenReturn(false);
      stubIntensionalExpansion();
      ValueSet.ConceptSetComponent includeElement = createFilterInclude(property, operator, value);

      // WHEN
      expandOperation.expandInclude(includeElement, includeParameter(ProcessingContext.provided()));

      // THEN
      verifyIntensionalExpansion(includeElement);
    }

    @Test
    @DisplayName("Should allow concept in filter of provided ValueSets in restricted mode")
    void shouldAllowConceptInFilterOfProvidedValueSetsInRestrictedMode() {
      // GIVEN
      when(snomedTerminologyManager.isRestrictedMode()).thenReturn(true);
      stubIntensionalExpansion();
      ValueSet.ConceptSetComponent includeElement =
          createFilterInclude("concept", ValueSet.FilterOperator.IN, "723264001");

      // WHEN
      expandOperation.expandInclude(includeElement, includeParameter(ProcessingContext.provided()));

      // THEN
      verifyIntensionalExpansion(includeElement);
    }

    @Test
    @DisplayName("Should reject hierarchy-based filter following a concept in filter")
    void shouldRejectHierarchyBasedFilterFollowingAConceptInFilter() {
      // GIVEN
      when(snomedTerminologyManager.isRestrictedMode()).thenReturn(true);
      ValueSet.ConceptSetComponent includeElement =
          createFilterInclude("concept", ValueSet.FilterOperator.IN, "723264001");
      includeElement
          .addFilter()
          .setProperty("concept")
          .setOp(ValueSet.FilterOperator.ISA)
          .setValue("138875005");
      VsExpandIncludeParameter parameter = includeParameter(ProcessingContext.provided());

      // WHEN / THEN
      assertThatThrownBy(() -> expandOperation.expandInclude(includeElement, parameter))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("'concept is-a'");
      verifyNoIntensionalExpansion();
    }

    @Test
    @DisplayName("Should expand extensional include of provided ValueSets in restricted mode")
    void shouldExpandExtensionalIncludeOfProvidedValueSetsInRestrictedMode() {
      // GIVEN
      when(snomedTerminologyManager.isRestrictedMode()).thenReturn(true);
      doNothing()
          .when(expandOperation)
          .expandExtensionalInclude(
              any(ValueSet.ConceptSetComponent.class),
              any(SnomedReleaseVersion.class),
              nullable(String.class),
              anyBoolean(),
              any(),
              any());
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude("138875005");

      // WHEN
      expandOperation.expandInclude(includeElement, includeParameter(ProcessingContext.provided()));

      // THEN
      verify(expandOperation)
          .expandExtensionalInclude(
              eq(includeElement),
              eq(defaultReleaseVersion),
              nullable(String.class),
              eq(true),
              any(),
              any());
    }

    private void stubIntensionalExpansion() {
      doNothing()
          .when(expandOperation)
          .expandIntensionalInclude(
              any(ValueSet.ConceptSetComponent.class),
              any(SnomedReleaseVersion.class),
              nullable(String.class),
              anyBoolean(),
              any());
    }

    private void verifyIntensionalExpansion(ValueSet.ConceptSetComponent includeElement) {
      verify(expandOperation)
          .expandIntensionalInclude(
              eq(includeElement),
              eq(defaultReleaseVersion),
              nullable(String.class),
              eq(true),
              any());
    }

    private void verifyNoIntensionalExpansion() {
      verify(expandOperation, never())
          .expandIntensionalInclude(any(), any(), any(), anyBoolean(), any());
      verify(snomed, never()).getSnomedGraph();
    }
  }

  @Nested
  @DisplayName("createExpansionContainsComponent Method Tests")
  class CreateExpansionContainsComponentTests {

    @Test
    @DisplayName("Should create expansion contains component for known concept")
    void shouldCreateExpansionContainsComponentForKnownConcept() throws Exception {
      // GIVEN
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo("123", false);
      SnomedConcept snomedConcept = createSnomedConceptWithDisplay("123", true, "Clinical finding");
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();

      when(snomed.getSnomedConceptWithDescription("123", defaultReleaseVersion, true))
          .thenReturn(snomedConcept);

      // WHEN
      ValueSet.ValueSetExpansionContainsComponent expansionContainsComponent =
          expandOperation.createExpansionContainsComponent(
              conceptBaseInfo,
              false,
              List.of(),
              createLanguageSelectionParameter(),
              vsExpandPartResult.getExplicitConceptReferences());

      // THEN
      assertThat(expansionContainsComponent.getSystem()).isEqualTo(SNOMED_SYSTEM);
      assertThat(expansionContainsComponent.getVersion()).isEqualTo(TEST_VERSION);
      assertThat(expansionContainsComponent.getCode()).isEqualTo("123");
      assertThat(expansionContainsComponent.getDisplay()).isEqualTo("Clinical finding");
      assertThat(expansionContainsComponent.getInactive()).isTrue();
    }

    @Test
    @DisplayName(
        "Should create expansion contains component for expression when concept is unknown")
    void shouldCreateExpansionContainsComponentForExpressionWhenConceptIsUnknown()
        throws Exception {
      // GIVEN
      String rawExpression = "404684003 |Clinical finding|";
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo(rawExpression, true);
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();
      Expression expression = mock(Expression.class);

      when(snomed.getSnomedConceptWithDescription(rawExpression, defaultReleaseVersion, true))
          .thenThrow(new UnknownConceptException("concept unknown"));
      when(expressionParser.parseExpression(rawExpression)).thenReturn(expression);
      when(expression.toString()).thenReturn("normalized-expression");
      when(scgProcessor.toTermString(expression, defaultReleaseVersion))
          .thenReturn("Expression display");

      // WHEN
      ValueSet.ValueSetExpansionContainsComponent expansionContainsComponent =
          expandOperation.createExpansionContainsComponent(
              conceptBaseInfo,
              false,
              List.of(),
              createLanguageSelectionParameter(),
              vsExpandPartResult.getExplicitConceptReferences());

      // THEN
      assertThat(expansionContainsComponent.getCode()).isEqualTo("normalized-expression");
      assertThat(expansionContainsComponent.getDisplay()).isEqualTo("Expression display");
      assertThat(expansionContainsComponent.getInactive()).isFalse();
    }

    @Test
    @DisplayName("Should include designations from explicit concept reference when requested")
    void shouldIncludeDesignationsFromExplicitConceptReferenceWhenRequested() throws Exception {
      // GIVEN
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo("123", true);
      SnomedConcept snomedConcept = createSnomedConcept("123", true);
      snomedConcept.setSnomedConceptDescriptionList(new java.util.ArrayList<>());
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();
      ValueSet.ConceptReferenceComponent conceptReferenceComponent =
          new ValueSet.ConceptReferenceComponent();
      conceptReferenceComponent.setCode("123");
      conceptReferenceComponent.setDisplay("Display from include");
      conceptReferenceComponent.setUserData(
          ResourceUserData
              .USERDATA_LANGUAGE,
          "en");
      vsExpandPartResult
          .getExplicitConceptReferences()
          .put(conceptBaseInfo.hashCode(), conceptReferenceComponent);

      when(snomed.getSnomedConceptWithDescription("123", defaultReleaseVersion, true))
          .thenReturn(snomedConcept);

      // WHEN
      ValueSet.ValueSetExpansionContainsComponent expansionContainsComponent =
          expandOperation.createExpansionContainsComponent(
              conceptBaseInfo,
              true,
              List.of(),
              createLanguageSelectionParameter(),
              vsExpandPartResult.getExplicitConceptReferences());

      // THEN
      assertThat(expansionContainsComponent.getDesignation())
          .extracting(ValueSet.ConceptReferenceDesignationComponent::getValue)
          .contains("Display from include");
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when expression designation resolution fails")
    void shouldThrowInvalidRequestExceptionWhenExpressionDesignationResolutionFails()
        throws Exception {
      // GIVEN
      String rawExpression = "404684003 |Clinical finding|";
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo(rawExpression, true);
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();
      Expression expression = mock(Expression.class);

      when(snomed.getSnomedConceptWithDescription(rawExpression, defaultReleaseVersion, true))
          .thenThrow(new UnknownConceptException("concept unknown"));
      when(expressionParser.parseExpression(rawExpression)).thenReturn(expression);
      when(scgProcessor.toTermString(expression, defaultReleaseVersion))
          .thenThrow(new UnknownConceptException("unknown in expression"));
      List<String> designations = List.of();
      LanguageSelectionParameter languageSelectionParameter = createLanguageSelectionParameter();
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferences =
          vsExpandPartResult.getExplicitConceptReferences();

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  expandOperation.createExpansionContainsComponent(
                      conceptBaseInfo,
                      false,
                      designations,
                      languageSelectionParameter,
                      explicitConceptReferences))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("not known to the SNOMED terminology service");
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when expression is valid but expression processing is disabled")
    void shouldThrowWhenExpressionProcessingIsDisabledForCreateExpansionContainsComponent()
        throws Exception {
      // GIVEN
      String rawExpression = "404684003 |Clinical finding|";
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo(rawExpression, true);
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();

      when(snomed.getSnomedConceptWithDescription(rawExpression, defaultReleaseVersion, true))
          .thenThrow(new UnknownConceptException("concept unknown"));
      when(snomedTerminologyManager.isExpressionProcessingEnabled()).thenReturn(false);

      List<String> designationValueList = List.of();
      LanguageSelectionParameter languageSelectionParameter = createLanguageSelectionParameter();
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap =
          vsExpandPartResult.getExplicitConceptReferences();
      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  expandOperation.createExpansionContainsComponent(
                      conceptBaseInfo,
                      false,
                      designationValueList,
                      languageSelectionParameter,
                      explicitConceptReferenceMap))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessage(
              String.format(SnomedConstants.EXPRESSION_PROCESSING_DISABLED_MESSAGE, rawExpression));

      verify(expressionParser, never()).parseExpression(any());
    }
  }

  private VsExpandPartResult expandProvidedInclude(
      ValueSet.ConceptSetComponent includeElement,
      boolean includeInactiveConcepts,
      String systemVersion,
      String checkSystemVersion,
      String forceSystemVersion) {
    return expandOperation.expandInclude(
        includeElement,
        VsExpandIncludeParameter.builder()
            .includeInactiveConcepts(includeInactiveConcepts)
            .version(systemVersion)
            .checkVersion(checkSystemVersion)
            .forceVersion(forceSystemVersion)
            .processingContext(ProcessingContext.provided())
            .build());
  }

  private static VsExpandIncludeParameter includeParameter(ProcessingContext processingContext) {
    return VsExpandIncludeParameter.builder()
        .includeInactiveConcepts(true)
        .processingContext(processingContext)
        .build();
  }

  private ValueSet.ConceptSetComponent createExtensionalInclude(String... codes) {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(SNOMED_SYSTEM);

    for (String code : codes) {
      includeElement.addConcept().setCode(code);
    }

    return includeElement;
  }

  private ValueSet.ConceptSetComponent createConstraintInclude(String... filterValues) {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(SNOMED_SYSTEM);

    for (String filterValue : filterValues) {
      includeElement
          .addFilter()
          .setProperty("constraint")
          .setOp(ValueSet.FilterOperator.EQUAL)
          .setValue(filterValue);
    }

    return includeElement;
  }

  private ValueSet.ConceptSetComponent createFilterInclude(
      String property, ValueSet.FilterOperator operator, String value) {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(SNOMED_SYSTEM);
    includeElement.addFilter().setProperty(property).setOp(operator).setValue(value);
    return includeElement;
  }

  private LanguageSelectionParameter createLanguageSelectionParameter() {
    return LanguageSelectionParameter.builder().build();
  }

  private ConceptBaseInfo createConceptBaseInfo(String code, boolean active) {
    return ConceptBaseInfo.builder()
        .system(SNOMED_SYSTEM)
        .version(TEST_VERSION)
        .code(code)
        .active(active)
        .build();
  }

  private SnomedConcept createSnomedConceptWithDisplay(
      String code, boolean active, String display) {
    SnomedConceptDescription description = new SnomedConceptDescription();
    description.setId("d-" + code);
    description.setActive(true);
    description.setLanguageCode("en");
    description.setTypeId(SnomedConstants.SC_SYNONYM);
    description.setTerm(display);
    description.setCaseSignificanceId(SnomedConstants.SC_CASESINSENSITIVE);

    SnomedLanguage language = new SnomedLanguage();
    language.setActive(true);
    language.setModuleId(TEST_MODULE_ID);
    language.setRefsetId("non-mapped-refset");
    language.setAcceptabilityId(SnomedConstants.SC_PREFERRED);
    description.setLanguageList(List.of(language));

    SnomedConcept snomedConcept = createSnomedConcept(code, active);
    snomedConcept.setSnomedConceptDescriptionList(List.of(description));
    return snomedConcept;
  }

  private SnomedConcept createSnomedConcept(String code, boolean active) {
    SnomedConcept snomedConcept = new SnomedConcept();
    snomedConcept.setId(code);
    snomedConcept.setActive(active);
    return snomedConcept;
  }
}

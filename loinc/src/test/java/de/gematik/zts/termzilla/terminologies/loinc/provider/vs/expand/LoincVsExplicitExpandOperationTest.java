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
package de.gematik.zts.termzilla.terminologies.loinc.provider.vs.expand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.loinc.Loinc;
import de.gematik.zts.termzilla.terminologies.loinc.LoincCodeSystemService;
import de.gematik.zts.termzilla.terminologies.loinc.LoincTerminologyManager;
import de.gematik.zts.termzilla.terminologies.loinc.LoincValueSetService;
import de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincDesignationManager;
import de.gematik.zts.termzilla.terminologies.loinc.memory.LoincInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ConceptVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;
import org.apache.commons.collections4.MultiValuedMap;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ValueSet;
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

/**
 * Unit tests for {@link LoincVsExplicitExpandOperation}.
 *
 * <p>Tests verify the expansion logic for ValueSet include elements, including:
 *
 * <ul>
 *   <li>Version selection strategy determination
 *   <li>Parameter validation (force-system-version, check-system-version, system-version)
 *   <li>Delegation to extensional and intensional expansion methods
 * </ul>
 *
 * <p><b>Note:</b> To mock the private methods {@code expandExtensionalInclude} and {@code
 * expandIntensionalInclude}, their visibility must be changed to package-protected.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoincVsExplicitExpandOperation Tests")
class LoincVsExplicitExpandOperationTest {

  private static final String LOINC_SYSTEM = "http://loinc.org";
  private static final String TEST_VERSION = "2.77";
  private static final String TEST_VERSION_2 = "2.76";

  @Mock private LoincValueSetService loincValueSetService;
  @Mock private LoincTerminologyManager loincTerminologyManager;
  @Mock private Loinc loinc;
  @Mock private LoincCodeSystemService loincCodeSystemService;
  @Mock private LoincInMemoryRepresentation loincInMemoryRepresentation;

  private Map<String, LoincVersion> fhirVersionToLoincVersionMap;
  private LoincVersion defaultLoincVersion;
  private LoincVersion testLoincVersion;

  private LoincVsExplicitExpandOperation expandOperation;

  @BeforeEach
  void setUp() {
    // GIVEN - Setup mock chain for constructor
    defaultLoincVersion =
        LoincVersion.builder()
            .versionNumber(277)
            .versionString(TEST_VERSION)
            .versionDate("2024-06-01")
            .build();

    testLoincVersion =
        LoincVersion.builder()
            .versionNumber(276)
            .versionString(TEST_VERSION_2)
            .versionDate("2024-01-01")
            .build();

    fhirVersionToLoincVersionMap = new HashMap<>();
    fhirVersionToLoincVersionMap.put(TEST_VERSION, defaultLoincVersion);
    fhirVersionToLoincVersionMap.put(TEST_VERSION_2, testLoincVersion);

    when(loincValueSetService.getLoincTerminologyManager()).thenReturn(loincTerminologyManager);
    when(loincTerminologyManager.getLoinc()).thenReturn(loinc);
    when(loincTerminologyManager.cs()).thenReturn(loincCodeSystemService);
    when(loincCodeSystemService.getFhirVersionToLoincVersionMap())
        .thenReturn(fhirVersionToLoincVersionMap);
    lenient().when(loinc.getSupportedLanguages()).thenReturn(List.of("en-US", "de-DE"));
    lenient().when(loinc.getDefaultLoincVersion()).thenReturn(defaultLoincVersion);
    lenient().when(loinc.getLoincInMemoryRepresentation()).thenReturn(loincInMemoryRepresentation);

    expandOperation = spy(new LoincVsExplicitExpandOperation(loincValueSetService));
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
          throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);

        // Mock the private methods to prevent actual execution
        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(
                includeElement,
                true, // includeInactiveConcepts
                null, // systemVersion
                null, // checkSystemVersion
                null, // forceSystemVersion
                null);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                LOINC_SYSTEM + "|" + TEST_VERSION,
                VsExpandPartResult.VersionSelectionStrategy.SYSTEM_DEFAULT);
      }

      @Test
      @DisplayName("Should use DEFINED strategy when include element has version defined")
      void shouldUseDefinedStrategyWhenIncludeHasVersion() throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(
                includeElement,
                true,
                null, // systemVersion
                null, // checkSystemVersion
                null, // forceSystemVersion
                null);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                LOINC_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.DEFINED);
      }

      @Test
      @DisplayName("Should use EXTERNALLY_SET strategy when system-version parameter is provided")
      void shouldUseExternallySetStrategyWhenSystemVersionProvided()
          throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(
                includeElement,
                true,
                TEST_VERSION_2, // systemVersion
                null, // checkSystemVersion
                null, // forceSystemVersion
                null);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                LOINC_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_SET);
      }

      @Test
      @DisplayName(
          "Should use EXTERNALLY_CHECK strategy when check-system-version parameter is provided")
      void shouldUseExternallyCheckStrategyWhenCheckSystemVersionProvided()
          throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(
                includeElement,
                true,
                null, // systemVersion
                TEST_VERSION_2, // checkSystemVersion
                null, // forceSystemVersion
                null);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                LOINC_SYSTEM + "|" + TEST_VERSION_2,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_CHECK);
      }

      @Test
      @DisplayName(
          "Should use EXTERNALLY_FORCE strategy when force-system-version parameter is provided")
      void shouldUseExternallyForceStrategyWhenForceSystemVersionProvided()
          throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(
                includeElement,
                true,
                null, // systemVersion
                null, // checkSystemVersion
                TEST_VERSION, // forceSystemVersion
                null);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                LOINC_SYSTEM + "|" + TEST_VERSION,
                VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_FORCE);
      }

      @Test
      @DisplayName(
          "Should prioritize EXTERNALLY_FORCE over DEFINED when both force-system-version and include version are provided")
      void shouldPrioritizeExternallyForceOverDefined() throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2); // Should be ignored due to force

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(
                includeElement,
                true,
                null,
                null,
                TEST_VERSION, // forceSystemVersion takes priority
                null);

        // THEN
        assertThat(vsExpandIncludeResult.getSystemVersionSelectionStrategy())
            .containsEntry(
                LOINC_SYSTEM + "|" + TEST_VERSION,
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
        includeElement.setSystem(LOINC_SYSTEM);
        includeElement.setVersion(TEST_VERSION);

        // WHEN / THEN
        assertThatThrownBy(
                () ->
                    expandOperation.expandInclude(
                        includeElement,
                        true,
                        null,
                        TEST_VERSION_2, // checkSystemVersion differs from include version
                        null,
                        null))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("check-system-version")
            .hasMessageContaining("does not match");
      }

      @Test
      @DisplayName("Should pass validation when check-system-version matches include version")
      void shouldPassValidationWhenCheckSystemVersionMatchesIncludeVersion()
          throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN / THEN
        assertThatCode(
                () ->
                    expandOperation.expandInclude(
                        includeElement,
                        true,
                        null,
                        TEST_VERSION_2, // Matches include version
                        null,
                        null))
            .doesNotThrowAnyException();
      }

      @Test
      @DisplayName("Should throw InvalidRequestException when requested version is not supported")
      void shouldThrowExceptionWhenVersionNotSupported() {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);
        includeElement.setVersion("99.99"); // Unsupported version

        // WHEN / THEN
        assertThatThrownBy(
                () -> expandOperation.expandInclude(includeElement, true, null, null, null, null))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("not supported");
      }
    }

    @Nested
    @DisplayName("Expansion Delegation Tests")
    class ExpansionDelegationTests {

      @Test
      @DisplayName("Should delegate to expandExtensionalInclude when include has concepts")
      void shouldDelegateToExpandExtensionalIncludeWhenIncludeHasConcepts()
          throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);
        includeElement.addConcept().setCode("12345-6");

        doNothing()
            .when(expandOperation)
            .expandExtensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(MultiValuedMap.class),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(includeElement, true, null, null, null, null);

        // THEN
        verify(expandOperation)
            .expandExtensionalInclude(
                eq(includeElement),
                eq(defaultLoincVersion),
                nullable(String.class),
                eq(true),
                eq(vsExpandIncludeResult.getExplicitConceptReferences()),
                eq(vsExpandIncludeResult.getConceptBaseInfos()));
      }

      @Test
      @DisplayName("Should delegate to expandIntensionalInclude when include has no concepts")
      void shouldDelegateToExpandIntensionalIncludeWhenIncludeHasNoConcepts()
          throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(includeElement, true, null, null, null, null);

        // THEN
        verify(expandOperation)
            .expandIntensionalInclude(
                eq(includeElement),
                eq(defaultLoincVersion),
                nullable(String.class),
                eq(true),
                eq(vsExpandIncludeResult.getConceptBaseInfos()));
      }

      @Test
      @DisplayName(
          "Should pass correct LoincVersion to expansion methods based on forceSystemVersion")
      void shouldPassCorrectVersionToExpansionMethodsBasedOnForceSystemVersion()
          throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        VsExpandPartResult vsExpandIncludeResult =
            expandOperation.expandInclude(includeElement, false, null, null, TEST_VERSION_2, null);

        // THEN
        verify(expandOperation)
            .expandIntensionalInclude(
                eq(includeElement),
                eq(testLoincVersion),
                nullable(String.class),
                eq(false),
                eq(vsExpandIncludeResult.getConceptBaseInfos()));
      }
    }

    @Nested
    @DisplayName("Default Version Lookup Tests")
    class DefaultVersionLookupTests {

      @Test
      @DisplayName("Should call getDefaultLoincVersion when no version is specified")
      void shouldCallGetDefaultLoincVersionWhenNoVersionSpecified() throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        expandOperation.expandInclude(includeElement, true, null, null, null, null);

        // THEN
        verify(loinc).getDefaultLoincVersion();
      }

      @Test
      @DisplayName("Should use fhirVersionToLoincVersionMap when version is specified")
      void shouldUseFhirVersionMapWhenVersionSpecified() throws NotImplementedException {
        // GIVEN
        ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
        includeElement.setSystem(LOINC_SYSTEM);
        includeElement.setVersion(TEST_VERSION_2);

        doNothing()
            .when(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                any(LoincVersion.class),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));

        // WHEN
        expandOperation.expandInclude(includeElement, true, null, null, null, null);

        // THEN - Verify the correct version was passed to the expansion method
        verify(expandOperation)
            .expandIntensionalInclude(
                any(ValueSet.ConceptSetComponent.class),
                eq(testLoincVersion),
                nullable(String.class),
                anyBoolean(),
                any(SortedSet.class));
      }
    }
  }

  @Nested
  @DisplayName("expandExtensionalInclude Method Tests")
  class ExpandExtensionalIncludeTests {

    private static Stream<Arguments> matchingFilterScenarios() {
      return Stream.of(
          Arguments.of("Display Match Value", "Unrelated definition", "display match"),
          Arguments.of(null, "Unrelated definition", "loinc.org"),
          Arguments.of(null, "Glucose concentration", "glucose"),
          Arguments.of(null, "Unrelated definition", "fallback designation"));
    }

    @Test
    @DisplayName("Should include concept when resultFilter matches concept code")
    void shouldIncludeConceptWhenResultFilterMatchesConceptCode() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude("12345-6", null);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("12345-6", "Non matching definition", true));

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultLoincVersion,
          "12345",
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("12345-6");
      verify(loinc).getLoincConcept("12345-6", defaultLoincVersion, true, Set.of("en-US", "de-DE"));
    }

    @ParameterizedTest
    @MethodSource("matchingFilterScenarios")
    @DisplayName("Should include concept when resultFilter matches a supported source")
    void shouldIncludeConceptWhenResultFilterMatchesASupportedSource(
        String conceptDisplay, String conceptDefinition, String resultFilter) throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement =
          createExtensionalInclude("12345-6", conceptDisplay);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("12345-6", conceptDefinition, true));

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultLoincVersion,
          resultFilter,
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("12345-6");
    }

    @Test
    @DisplayName("Should exclude concept when resultFilter does not match reference or concept")
    void shouldExcludeConceptWhenResultFilterDoesNotMatchReferenceOrConcept() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude("12345-6", null);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("12345-6", "Unrelated definition", true));

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultLoincVersion,
          "not-present",
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos()).isEmpty();
      assertThat(vsExpandIncludeResult.getExplicitConceptReferences().size()).isZero();
    }

    @Test
    @DisplayName("Should include concept when resultFilter matches concept reference designation")
    void shouldIncludeConceptWhenResultFilterMatchesConceptReferenceDesignation() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude("12345-6", null);
      includeElement.addConcept().setCode("9999-9");
      ValueSet.ConceptReferenceDesignationComponent designationComponent =
          includeElement.getConcept().getFirst().addDesignation();
      designationComponent.setValue("Reference designation value");

      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("12345-6", "Unrelated definition", true));
      when(loinc.getLoincConcept(eq("9999-9"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("9999-9", "Unrelated definition", true));

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultLoincVersion,
          "reference designation",
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("12345-6");
    }

    @Test
    @DisplayName("Should treat blank resultFilter as match all")
    void shouldTreatBlankResultFilterAsMatchAll() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = createExtensionalInclude("12345-6", null);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("12345-6", "Unrelated definition", true));

      // WHEN
      expandOperation.expandExtensionalInclude(
          includeElement,
          defaultLoincVersion,
          "   ",
          true,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("12345-6");
      verify(loinc).getLoincConcept("12345-6", defaultLoincVersion, true, Set.of());
    }
  }

  @Nested
  @DisplayName("expandIntensionalInclude Method Tests")
  class ExpandIntensionalIncludeTests {

    @Test
    @DisplayName("Should apply resultFilter to include only matching concepts")
    void shouldApplyResultFilterToIncludeOnlyMatchingConcepts() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
      includeElement.setSystem(LOINC_SYSTEM);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(loincInMemoryRepresentation.getConceptVersionList(defaultLoincVersion))
          .thenReturn(
              List.of(
                  createConceptVersion("1111-1", "ACTIVE"),
                  createConceptVersion("2222-2", "ACTIVE")));

      when(loinc.getLoincConcept(eq("1111-1"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("1111-1", "Glucose concentration", true));
      when(loinc.getLoincConcept(eq("2222-2"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("2222-2", "Potassium concentration", true));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultLoincVersion,
          "glucose",
          true,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos())
          .extracting(ConceptBaseInfo::getCode)
          .containsExactly("1111-1");
      verify(loinc).getLoincConcept("1111-1", defaultLoincVersion, true, Set.of("en-US", "de-DE"));
      verify(loinc).getLoincConcept("2222-2", defaultLoincVersion, true, Set.of("en-US", "de-DE"));
    }

    @Test
    @DisplayName("Should return empty result when no concept matches resultFilter")
    void shouldReturnEmptyResultWhenNoConceptMatchesResultFilter() throws Exception {
      // GIVEN
      ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
      includeElement.setSystem(LOINC_SYSTEM);
      VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

      when(loincInMemoryRepresentation.getConceptVersionList(defaultLoincVersion))
          .thenReturn(List.of(createConceptVersion("1111-1", "ACTIVE")));
      when(loinc.getLoincConcept(eq("1111-1"), eq(defaultLoincVersion), eq(true), any(Set.class)))
          .thenReturn(createLoincConcept("1111-1", "Unrelated definition", true));

      // WHEN
      expandOperation.expandIntensionalInclude(
          includeElement,
          defaultLoincVersion,
          "not-present",
          true,
          vsExpandIncludeResult.getConceptBaseInfos());

      // THEN
      assertThat(vsExpandIncludeResult.getConceptBaseInfos()).isEmpty();
    }
  }

  @Nested
  @DisplayName("createExpansionContainsComponent Method Tests")
  class CreateExpansionContainsComponentTests {

    @Test
    @DisplayName(
      "Should populate expansion contains component with display, extension and designations")
    void shouldPopulateExpansionContainsComponentWithDisplayExtensionAndDesignations()
      throws Exception {
      // GIVEN
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo("12345-6", true);
      LoincConcept loincConcept = createLoincConcept("12345-6", "Concept definition", true);
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();

      ValueSet.ConceptReferenceComponent conceptReferenceComponent =
        new ValueSet.ConceptReferenceComponent();
      conceptReferenceComponent.setCode("12345-6");
      conceptReferenceComponent.addExtension(
        new Extension("http://example.org/fhir/StructureDefinition/test-extension"));
      vsExpandPartResult
        .getExplicitConceptReferences()
        .put(conceptBaseInfo.hashCode(), conceptReferenceComponent);

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
        .thenReturn(loincConcept);

      // WHEN
      ValueSet.ValueSetExpansionContainsComponent expansionContainsComponent =
        expandOperation.createExpansionContainsComponent(
          conceptBaseInfo,
          true,
          List.of(),
          createLanguageSelectionParameter(),
          vsExpandPartResult.getExplicitConceptReferences());

      // THEN
      assertThat(expansionContainsComponent.getSystem()).isEqualTo(LOINC_SYSTEM);
      assertThat(expansionContainsComponent.getVersion()).isEqualTo(TEST_VERSION);
      assertThat(expansionContainsComponent.getCode()).isEqualTo("12345-6");
      assertThat(expansionContainsComponent.getDisplay()).isEqualTo("fallback designation");
      assertThat(expansionContainsComponent.getInactive()).isFalse();
      assertThat(expansionContainsComponent.getExtension())
        .extracting(Extension::getUrl)
        .containsExactly("http://example.org/fhir/StructureDefinition/test-extension");
      assertThat(expansionContainsComponent.getDesignation())
        .extracting(ValueSet.ConceptReferenceDesignationComponent::getValue)
        .containsExactly("fallback designation");
    }

    @Test
    @DisplayName("Should not include designations when includeDesignations is false")
    void shouldNotIncludeDesignationsWhenIncludeDesignationsIsFalse() throws Exception {
      // GIVEN
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo("12345-6", true);
      LoincConcept loincConcept = createLoincConcept("12345-6", "Concept definition", true);
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
        .thenReturn(loincConcept);

      // WHEN
      ValueSet.ValueSetExpansionContainsComponent expansionContainsComponent =
        expandOperation.createExpansionContainsComponent(
          conceptBaseInfo,
          false,
          List.of(),
          createLanguageSelectionParameter(),
          vsExpandPartResult.getExplicitConceptReferences());

      // THEN
      assertThat(expansionContainsComponent.getDisplay()).isEqualTo("fallback designation");
      assertThat(expansionContainsComponent.getDesignation()).isEmpty();
    }

    @Test
    @DisplayName("Should map unknown concept to InvalidRequestException")
    void shouldMapUnknownConceptToInvalidRequestException() throws Exception {
      // GIVEN
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo("12345-6", true);
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();
      List<String> designationParamValueList = List.of();
      LanguageSelectionParameter languageSelectionParameter = createLanguageSelectionParameter();
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap =
        vsExpandPartResult.getExplicitConceptReferences();

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
        .thenThrow(new UnknownConceptException("unknown concept"));

      // WHEN / THEN
      assertThatThrownBy(
        () ->
          expandOperation.createExpansionContainsComponent(
            conceptBaseInfo,
            true,
            designationParamValueList,
            languageSelectionParameter,
            explicitConceptReferenceMap))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("not known to the LOINC terminology service");
    }

    @Test
    @DisplayName("Should map inactive concept to InvalidRequestException")
    void shouldMapInactiveConceptToInvalidRequestException() throws Exception {
      // GIVEN
      ConceptBaseInfo conceptBaseInfo = createConceptBaseInfo("12345-6", true);
      VsExpandPartResult vsExpandPartResult = new VsExpandPartResult();
      List<String> designationParamValueList = List.of();
      LanguageSelectionParameter languageSelectionParameter = createLanguageSelectionParameter();
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap =
        vsExpandPartResult.getExplicitConceptReferences();

      when(loinc.getLoincConcept(eq("12345-6"), eq(defaultLoincVersion), eq(true), any(Set.class)))
        .thenThrow(new InactiveConceptException("inactive concept"));

      // WHEN / THEN
      assertThatThrownBy(
        () ->
          expandOperation.createExpansionContainsComponent(
            conceptBaseInfo,
            true,
            designationParamValueList,
            languageSelectionParameter,
            explicitConceptReferenceMap))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("inactive")
        .hasMessageContaining("includeInactive is set to false");
    }
  }
  @Nested
  @DisplayName("collectDesignations Method Tests")
  class CollectDesignationsTests {

    @Test
    @DisplayName("Should collect designations from the LOINC concept")
    void shouldCollectDesignationsFromLoincConcept() {
      // GIVEN
      LoincConcept concept = createLoincConcept("12345-6", "definition", true);
      ValueSet.ConceptReferenceComponent conceptReferenceComponent =
          new ValueSet.ConceptReferenceComponent();
      conceptReferenceComponent.setDisplay("display from reference");

      // WHEN
      LoincDesignationManager designationManager =
          expandOperation.collectDesignations(concept, List.of(conceptReferenceComponent));

      // THEN
      assertThat(designationManager.getFullDesignationList())
          .extracting(d -> d.getValue())
          .containsExactly("fallback designation");
    }

    @Test
    @DisplayName("Should currently ignore explicit concept references when collecting designations")
    void shouldCurrentlyIgnoreExplicitConceptReferencesWhenCollectingDesignations() {
      // GIVEN
      LoincConcept concept = createLoincConcept("12345-6", "definition", true);
      ValueSet.ConceptReferenceComponent conceptReferenceComponent =
          new ValueSet.ConceptReferenceComponent();
      conceptReferenceComponent.setDisplay("display from reference");
      conceptReferenceComponent.addDesignation().setValue("designation from reference");

      // WHEN
      LoincDesignationManager designationManager =
          expandOperation.collectDesignations(concept, List.of(conceptReferenceComponent));

      // THEN
      assertThat(designationManager.getFullDesignationList()).isNotEmpty();
      assertThat(designationManager.getFullDesignationList())
          .extracting(d -> d.getValue())
          .doesNotContain("display from reference", "designation from reference");
    }
  }

  private LanguageSelectionParameter createLanguageSelectionParameter() {
    return LanguageSelectionParameter.builder().build();
  }

  private ConceptBaseInfo createConceptBaseInfo(String code, boolean active) {
    return ConceptBaseInfo.builder()
        .system(LOINC_SYSTEM)
        .version(TEST_VERSION)
        .code(code)
        .active(active)
        .build();
  }

  private ValueSet.ConceptSetComponent createExtensionalInclude(String code, String display) {
    ValueSet.ConceptSetComponent includeElement = new ValueSet.ConceptSetComponent();
    includeElement.setSystem(LOINC_SYSTEM);
    ValueSet.ConceptReferenceComponent conceptReferenceComponent = includeElement.addConcept();
    conceptReferenceComponent.setCode(code);
    if (display != null) {
      conceptReferenceComponent.setDisplay(display);
    }
    return includeElement;
  }

  private LoincConcept createLoincConcept(String code, String definition, boolean active) {
    LoincConcept loincConcept = new LoincConcept();
    loincConcept.setCode(code);
    loincConcept.setDefinition(definition);
    loincConcept.setActive(active);

    LoincDesignation loincDesignation = new LoincDesignation();
    loincDesignation.setLanguage("en-US");
    loincDesignation.setType("LONG_COMMON_NAME");
    loincDesignation.setDesignation("fallback designation");

    List<LoincDesignation> designationList = new ArrayList<>();
    designationList.add(loincDesignation);
    loincConcept.setDesignationList(designationList);

    return loincConcept;
  }

  private ConceptVersion createConceptVersion(String loincNum, String status) {
    ConceptVersion conceptVersion = new ConceptVersion();
    conceptVersion.setLoincNum(loincNum);
    conceptVersion.setStatus(status);
    return conceptVersion;
  }
}

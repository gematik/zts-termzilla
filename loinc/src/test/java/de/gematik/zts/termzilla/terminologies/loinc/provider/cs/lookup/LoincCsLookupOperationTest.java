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
package de.gematik.zts.termzilla.terminologies.loinc.provider.cs.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptDesignation;
import de.gematik.zts.termzilla.api.interfaces.properties.BaseTypeConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.GenericConceptProperty;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.loinc.Loinc;
import de.gematik.zts.termzilla.terminologies.loinc.LoincCodeSystemService;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import de.gematik.zts.termzilla.terminologies.loinc.LoincTerminologyManager;
import de.gematik.zts.termzilla.terminologies.loinc.memory.LoincInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link LoincCsLookupOperation}.
 *
 * <p>Covers parameter dispatching, version resolution, result population, designation filtering,
 * and concept property inclusion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoincCsLookupOperation Tests")
class LoincCsLookupOperationTest {

  private static final String LOINC_SYSTEM = LoincConstants.TERMINOLOGY_CANONICAL_NAME;
  private static final String TEST_VERSION = "2.76";
  private static final String CS_DISPLAY_NAME = "LOINC";

  /** A numeric LOINC code (Term) — default designation type is {@code LONG_COMMON_NAME}. */
  private static final String CONCEPT_CODE = "2823-3";

  private static final String EN_DISPLAY = "Potassium [Moles/volume] in Serum or Plasma";
  private static final String DE_DISPLAY = "Kalium [Mol/Volumen] in Serum oder Plasma";
  private static final String LONG_COMMON_NAME = LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME;

  @Mock private LoincCodeSystemService loincCsService;
  @Mock private LoincTerminologyManager loincTerminologyManager;
  @Mock private Loinc loinc;
  @Mock private CodeSystem mockCodeSystem;
  @Mock private LoincInMemoryRepresentation loincInMemoryRepresentation;

  private LoincVersion defaultLoincVersion;
  private LoincCsLookupOperation lookupOperation;

  @BeforeEach
  void setUp() {
    defaultLoincVersion =
        LoincVersion.builder().versionNumber(276).versionString(TEST_VERSION).build();

    Map<String, LoincVersion> versionMap = new HashMap<>();
    versionMap.put(TEST_VERSION, defaultLoincVersion);

    when(loincCsService.getLoincTerminologyManager()).thenReturn(loincTerminologyManager);
    when(loincTerminologyManager.getLoinc()).thenReturn(loinc);

    lenient().when(loincCsService.getFhirVersionToLoincVersionMap()).thenReturn(versionMap);
    lenient().when(loinc.getDefaultLoincVersion()).thenReturn(defaultLoincVersion);
    lenient()
        .when(loincCsService.getVersionToCodeSystemMap())
        .thenReturn(Map.of(TEST_VERSION, mockCodeSystem));
    lenient().when(mockCodeSystem.getName()).thenReturn(CS_DISPLAY_NAME);

    // Provide empty in-memory maps so AdapterHelper can run without NullPointerException
    // when COMPONENT/SYSTEM/etc. properties are requested but no data is loaded
    lenient().when(loinc.getLoincInMemoryRepresentation()).thenReturn(loincInMemoryRepresentation);
    lenient()
        .when(loincInMemoryRepresentation.getComponentLoincNumberToLoincPartLinkVersionMap())
        .thenReturn(new ArrayListValuedHashMap<>());
    lenient()
        .when(loincInMemoryRepresentation.getPropertyLoincNumberToLoincPartLinkVersionMap())
        .thenReturn(new ArrayListValuedHashMap<>());
    lenient()
        .when(loincInMemoryRepresentation.getTimeLoincNumberToLoincPartLinkVersionMap())
        .thenReturn(new ArrayListValuedHashMap<>());
    lenient()
        .when(loincInMemoryRepresentation.getSystemLoincNumberToLoincPartLinkVersionMap())
        .thenReturn(new ArrayListValuedHashMap<>());
    lenient()
        .when(loincInMemoryRepresentation.getScaleLoincNumberToLoincPartLinkVersionMap())
        .thenReturn(new ArrayListValuedHashMap<>());

    lookupOperation = new LoincCsLookupOperation(loincCsService);
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a simple LOINC concept with a single English {@code LONG_COMMON_NAME} designation. */
  private LoincConcept createSimpleLoincConcept() {
    LoincConcept concept = new LoincConcept();
    concept.setCode(CONCEPT_CODE);
    concept.setActive(true);

    LoincDesignation enDesignation = new LoincDesignation();
    enDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
    enDesignation.setType(LONG_COMMON_NAME);
    enDesignation.setDesignation(EN_DISPLAY);
    concept.getDesignationList().add(enDesignation);

    return concept;
  }

  /**
   * Creates a LOINC concept with both an English and a German {@code LONG_COMMON_NAME} designation.
   */
  private LoincConcept createConceptWithMultilingualDesignations() {
    LoincConcept concept = new LoincConcept();
    concept.setCode(CONCEPT_CODE);
    concept.setActive(true);

    LoincDesignation enDesignation = new LoincDesignation();
    enDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
    enDesignation.setType(LONG_COMMON_NAME);
    enDesignation.setDesignation(EN_DISPLAY);
    concept.getDesignationList().add(enDesignation);

    LoincDesignation deDesignation = new LoincDesignation();
    deDesignation.setLanguage("de-DE");
    deDesignation.setType(LONG_COMMON_NAME);
    deDesignation.setDesignation(DE_DISPLAY);
    concept.getDesignationList().add(deDesignation);

    return concept;
  }

  /** Calls {@code lookupCode} with null version and an empty language selection. */
  private CsLookupResult doLookup(String code, Set<String> properties)
      throws UnsupportedTerminologyException {
    return lookupOperation.lookupCode(
        LOINC_SYSTEM, null, code, LanguageSelectionParameter.builder().build(), properties);
  }

  // ==========================================================================
  // Tests
  // ==========================================================================

  @Nested
  @DisplayName("lookupCode(CsLookupParameter) Tests")
  class ParameterDispatchTests {

    @Test
    @DisplayName("Should extract system, version, and code from Coding and return a result")
    void shouldExtractFieldsFromCodingAndDelegate()
        throws UnknownConceptException,
            InactiveConceptException,
            UnsupportedTerminologyException,
            NotImplementedException {
      // GIVEN
      Coding coding = new Coding(LOINC_SYSTEM, CONCEPT_CODE, null);
      CsLookupParameter param = CsLookupParameter.builder().codingParam(coding).build();
      when(loinc.getLoincConcept(eq(CONCEPT_CODE), any(), anyBoolean(), any()))
          .thenReturn(createSimpleLoincConcept());

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(param);

      // THEN
      assertThat(result.isFound()).isTrue();
      assertThat(result.getSearchedForCode()).isEqualTo(CONCEPT_CODE);
      assertThat(result.getSearchedForSystem()).isEqualTo(LOINC_SYSTEM);
    }

    @Test
    @DisplayName("Should process system and code parameters directly")
    void shouldProcessSystemAndCodeParameters()
        throws UnknownConceptException,
            InactiveConceptException,
            UnsupportedTerminologyException,
            NotImplementedException {
      // GIVEN
      CsLookupParameter param =
          CsLookupParameter.builder()
              .systemParam(new UriType(LOINC_SYSTEM))
              .codeParam(new CodeType(CONCEPT_CODE))
              .build();
      when(loinc.getLoincConcept(eq(CONCEPT_CODE), any(), anyBoolean(), any()))
          .thenReturn(createSimpleLoincConcept());

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(param);

      // THEN
      assertThat(result.isFound()).isTrue();
    }

    @Test
    @DisplayName("Should add url as default property when no property parameter is present")
    void shouldAddUrlAsDefaultPropertyWhenNoPropertyParamPresent()
        throws UnknownConceptException,
            InactiveConceptException,
            UnsupportedTerminologyException,
            NotImplementedException {
      // GIVEN
      CsLookupParameter param =
          CsLookupParameter.builder()
              .systemParam(new UriType(LOINC_SYSTEM))
              .codeParam(new CodeType(CONCEPT_CODE))
              .propertyParam(null)
              .build();
      when(loinc.getLoincConcept(eq(CONCEPT_CODE), any(), anyBoolean(), any()))
          .thenReturn(createSimpleLoincConcept());

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(param);

      // THEN
      assertThat(result.getProperties())
          .extracting(GenericConceptProperty::getPropertyName)
          .contains("url");
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when neither system nor coding is present")
    void shouldThrowWhenNeitherSystemNorCodingPresent() {
      // GIVEN
      CsLookupParameter param = CsLookupParameter.builder().build();

      // WHEN / THEN
      assertThatThrownBy(() -> lookupOperation.lookupCode(param))
          .isInstanceOf(InvalidRequestException.class);
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Version resolution Tests")
  class VersionResolutionTests {

    @Test
    @DisplayName("Should use default LOINC version when no version is provided")
    void shouldUseDefaultVersionWhenVersionIsNull()
        throws UnknownConceptException, InactiveConceptException, UnsupportedTerminologyException {
      // GIVEN
      when(loinc.getLoincConcept(any(), eq(defaultLoincVersion), anyBoolean(), any()))
          .thenReturn(createSimpleLoincConcept());

      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getCodeSystemVersion()).isEqualTo(TEST_VERSION);
    }

    @Test
    @DisplayName("Should use the specified LOINC version when it is present in the version map")
    void shouldUseSpecifiedVersionWhenPresentInVersionMap()
        throws UnknownConceptException, InactiveConceptException, UnsupportedTerminologyException {
      // GIVEN
      when(loinc.getLoincConcept(any(), eq(defaultLoincVersion), anyBoolean(), any()))
          .thenReturn(createSimpleLoincConcept());

      // WHEN
      CsLookupResult result =
          lookupOperation.lookupCode(
              LOINC_SYSTEM,
              TEST_VERSION,
              CONCEPT_CODE,
              LanguageSelectionParameter.builder().build(),
              Set.of("url"));

      // THEN
      assertThat(result.getCodeSystemVersion()).isEqualTo(TEST_VERSION);
    }

    @Test
    @DisplayName("Should throw UnsupportedTerminologyException for an unknown LOINC version")
    void shouldThrowForUnknownVersion() {
      // GIVEN
      String unknownVersion = "9.99";

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  lookupOperation.lookupCode(
                      LOINC_SYSTEM,
                      unknownVersion,
                      CONCEPT_CODE,
                      LanguageSelectionParameter.builder().build(),
                      Set.of("url")))
          .isInstanceOf(UnsupportedTerminologyException.class)
          .hasMessageContaining(unknownVersion);
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Result population Tests")
  class ResultPopulationTests {

    @BeforeEach
    void setUpConceptLookup() throws UnknownConceptException, InactiveConceptException {
      // lenient: some tests override this stub with a different concept
      lenient()
          .when(loinc.getLoincConcept(eq(CONCEPT_CODE), any(), anyBoolean(), any()))
          .thenReturn(createSimpleLoincConcept());
    }

    @Test
    @DisplayName("Should set found=true when the concept is resolved")
    void shouldSetFoundTrueWhenConceptResolved() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.isFound()).isTrue();
    }

    @Test
    @DisplayName("Should set the code system display name from the CodeSystem resource")
    void shouldSetCodeSystemDisplayName() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getCodeSystemDisplayName()).isEqualTo(CS_DISPLAY_NAME);
    }

    @Test
    @DisplayName("Should set the code system version to the resolved LOINC version string")
    void shouldSetCodeSystemVersion() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getCodeSystemVersion()).isEqualTo(TEST_VERSION);
    }

    @Test
    @DisplayName(
        "Should set the code display from the LONG_COMMON_NAME designation via LoincDesignationManager")
    void shouldSetCodeDisplayFromDesignationManager() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getCodeDisplay()).isEqualTo(EN_DISPLAY);
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Designation handling Tests")
  class DesignationTests {

    @BeforeEach
    void setUpConceptWithDesignations() throws UnknownConceptException, InactiveConceptException {
      when(loinc.getLoincConcept(eq(CONCEPT_CODE), any(), anyBoolean(), any()))
          .thenReturn(createConceptWithMultilingualDesignations());
    }

    @Test
    @DisplayName("Should include all designations when 'designation' property is requested")
    void shouldIncludeAllDesignationsWhenDesignationPropertyRequested()
        throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("designation"));

      // THEN
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactlyInAnyOrder(EN_DISPLAY, DE_DISPLAY);
    }

    @Test
    @DisplayName("Should include only German designations when lang.de-DE is requested")
    void shouldIncludeOnlyMatchingDesignationsForLangX() throws UnsupportedTerminologyException {
      // WHEN - request only German designations
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("lang.de-DE"));

      // THEN - only the German designation is included
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactly(DE_DISPLAY);
    }

    @Test
    @DisplayName(
        "Should fall back to all designations when lang.X contains an unsupported language code")
    void shouldIncludeAllDesignationsWhenLangXIsUnsupported()
        throws UnsupportedTerminologyException {
      // GIVEN - "lang.xyz" is not a supported BCP 47 language code
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("lang.xyz"));

      // THEN - both designations are included as fallback
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactlyInAnyOrder(EN_DISPLAY, DE_DISPLAY);
    }

    @Test
    @DisplayName(
        "Should include no designations when neither 'designation' nor lang.X is requested")
    void shouldIncludeNoDesignationsWhenNotRequested() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getDesignations()).isEmpty();
    }

    @Test
    @DisplayName(
        "Should include designations in the default response when no properties are requested")
    void shouldIncludeDesignationsInDefaultResponseWhenNoPropertiesAreRequested()
        throws UnsupportedTerminologyException {
      // GIVEN - empty property set triggers default expansion which includes "designation"
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, new HashSet<>());

      // THEN
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactlyInAnyOrder(EN_DISPLAY, DE_DISPLAY);
    }

    @Test
    @DisplayName(
        "Should filter to lang.X language when both 'designation' and lang.X are requested")
    void shouldFilterToLangXWhenDesignationAndLangXAreRequested()
        throws UnsupportedTerminologyException {
      // GIVEN - when valid lang codes are extracted, language filtering applies even if
      // 'designation'
      // is also requested (the lang code set is non-empty → filter wins)
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("designation", "lang.de-DE"));

      // THEN - only German designations are returned because lang.de-DE yields a non-empty code set
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactly(DE_DISPLAY);
    }

    @Test
    @DisplayName("Should map language and use coding to the ConceptDesignation output")
    void shouldMapDesignationFieldsCorrectly() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("designation"));

      // THEN
      assertThat(result.getDesignations())
          .filteredOn(d -> EN_DISPLAY.equals(d.getValue()))
          .hasSize(1)
          .first()
          .satisfies(
              d -> {
                assertThat(d.getLanguage()).isEqualTo(LoincConstants.LOINC_LANGUAGE);
                assertThat(d.getUseSystem()).isEqualTo(LoincConstants.TERMINOLOGY_CANONICAL_NAME);
                assertThat(d.getUseCode()).isEqualTo(LONG_COMMON_NAME);
              });
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Concept property Tests")
  class ConceptPropertyTests {

    @BeforeEach
    void setUpConceptLookup() throws UnknownConceptException, InactiveConceptException {
      // lenient: some tests override this stub with a different concept
      lenient()
          .when(loinc.getLoincConcept(eq(CONCEPT_CODE), any(), anyBoolean(), any()))
          .thenReturn(createSimpleLoincConcept());
    }

    @Test
    @DisplayName("Should add url property with the LOINC canonical URL when requested")
    void shouldAddUrlPropertyWhenRequested() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "url".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .satisfies(
              p -> {
                StringType value = (StringType) ((BaseTypeConceptProperty) p).getValue();
                assertThat(value.getValue()).isEqualTo(LoincConstants.TERMINOLOGY_CANONICAL_NAME);
              });
    }

    @Test
    @DisplayName("Should not add url property when it is not requested")
    void shouldNotAddUrlPropertyWhenNotRequested() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("inactive"));

      // THEN
      assertThat(result.getProperties())
          .extracting(GenericConceptProperty::getPropertyName)
          .isNotEmpty()
          .doesNotContain("url");
    }

    @Test
    @DisplayName("Should add inactive=false for an active concept")
    void shouldAddInactiveFalseForActiveConcept() throws UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("inactive"));

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "inactive".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .satisfies(
              p -> {
                BooleanType value = (BooleanType) ((BaseTypeConceptProperty) p).getValue();
                assertThat(value.booleanValue()).isFalse();
              });
    }

    @Test
    @DisplayName("Should add inactive=true for an inactive concept")
    void shouldAddInactiveTrueForInactiveConcept()
        throws UnknownConceptException, InactiveConceptException, UnsupportedTerminologyException {
      // GIVEN
      LoincConcept inactiveConcept = new LoincConcept();
      inactiveConcept.setCode(CONCEPT_CODE);
      inactiveConcept.setActive(false);
      LoincDesignation designation = new LoincDesignation();
      designation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      designation.setType(LONG_COMMON_NAME);
      designation.setDesignation(EN_DISPLAY);
      inactiveConcept.getDesignationList().add(designation);
      when(loinc.getLoincConcept(eq(CONCEPT_CODE), any(), anyBoolean(), any()))
          .thenReturn(inactiveConcept);

      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("inactive"));

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "inactive".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .satisfies(
              p -> {
                BooleanType value = (BooleanType) ((BaseTypeConceptProperty) p).getValue();
                assertThat(value.booleanValue()).isTrue();
              });
    }

    @Test
    @DisplayName("Should include url in default property set when no properties are requested")
    void shouldIncludeUrlInDefaultPropertySetWhenNoPropertiesRequested()
        throws UnsupportedTerminologyException {
      // GIVEN - empty property set triggers default expansion which includes "url"
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, new HashSet<>());

      // THEN
      assertThat(result.getProperties())
          .extracting(GenericConceptProperty::getPropertyName)
          .contains("url");
    }

    @Test
    @DisplayName("Should include inactive in default property set when no properties are requested")
    void shouldIncludeInactiveInDefaultPropertySetWhenNoPropertiesRequested()
        throws UnsupportedTerminologyException {
      // GIVEN - empty property set triggers default expansion which includes "inactive"
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, new HashSet<>());

      // THEN
      assertThat(result.getProperties())
          .extracting(GenericConceptProperty::getPropertyName)
          .contains("inactive");
    }

    @Test
    @DisplayName("Should not throw when COMPONENT property is requested but no data is loaded")
    void shouldNotThrowWhenComponentPropertyRequestedWithNoData() {
      // GIVEN - empty in-memory maps (set up in parent @BeforeEach)
      // WHEN / THEN - no exception is thrown; no property is added since the map is empty
      assertThatCode(() -> doLookup(CONCEPT_CODE, Set.of("COMPONENT"))).doesNotThrowAnyException();
    }
  }
}

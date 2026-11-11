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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.lookup;

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_SUFFICENTLYDEFINED;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createConcept;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createDescription;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createLanguage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptDesignation;
import de.gematik.zts.termzilla.api.interfaces.properties.BaseTypeConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties;
import de.gematik.zts.termzilla.api.interfaces.properties.GenericConceptProperty;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.common.constants.CanonicalUrl;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.graph.SnomedGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import de.gematik.zts.termzilla.terminologies.snomed.scg.DemisSCGProcessor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.snomed.languages.scg.SCGException;
import org.snomed.languages.scg.SCGExpressionParser;
import org.snomed.languages.scg.domain.model.Expression;

/**
 * Unit tests for {@link SnomedCodeSystemLookupOperation}.
 *
 * <p>Covers parameter dispatching, version resolution, result population, designation filtering,
 * concept property inclusion, and the post-coordinated expression fallback path.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SnomedCodeSystemLookupOperation Tests")
class SnomedCodeSystemLookupOperationTest {

  private static final String MODULE_CORE_ID = SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
  private static final String MODULE_DE_ID =
      SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;
  private static final String TEST_DATE = "20240101";
  private static final String CONCEPT_CODE = "73211009";
  private static final String EXPRESSION_CODE =
      "73211009 |Diabetes mellitus| : 246454002 |Occurrence| = 255407002 |Neonatal|";
  private static final String SYNONYM_TYPE_ID = SnomedConstants.SC_SYNONYM;
  private static final String PREFERRED = SnomedConstants.SC_PREFERRED;
  private static final String REFSET_EN_GB = SnomedReleaseVersionConstants.EN_GB;
  private static final String REFSET_DE_DE = SnomedReleaseVersionConstants.DE_DE;
  private static final String EN_TERM = "Diabetes mellitus";
  private static final String DE_TERM = "Diabetes mellitus (Deutsch)";

  @Mock private SnomedCodeSystemService snomedCsService;
  @Mock private SnomedTerminologyManager snomedTerminologyManager;
  @Mock private Snomed snomed;
  @Mock private SnomedGraphRepresentation snomedGraph;
  @Mock private SCGExpressionParser expressionParser;
  @Mock private DemisSCGProcessor scgProcessor;

  private SnomedReleaseVersion defaultReleaseVersion;
  private SnomedCodeSystemLookupOperation lookupOperation;

  @BeforeEach
  void setUp() throws Exception {
    defaultReleaseVersion = new SnomedReleaseVersion(MODULE_CORE_ID, TEST_DATE, new HashMap<>());
    Map<String, SnomedReleaseVersion> versionMap = new HashMap<>();
    versionMap.put(defaultReleaseVersion.getFhirVersion(), defaultReleaseVersion);

    when(snomedCsService.getSnomedTerminologyManager()).thenReturn(snomedTerminologyManager);
    when(snomedTerminologyManager.getSnomed()).thenReturn(snomed);
    lenient().when(snomedTerminologyManager.isExpressionProcessingEnabled()).thenReturn(true);
    when(snomedTerminologyManager.getExpressionParser()).thenReturn(expressionParser);
    when(snomedTerminologyManager.getScgProcessor()).thenReturn(scgProcessor);

    lenient()
        .when(snomedCsService.getFhirVersionToSnomedReleaseVersionMap())
        .thenReturn(versionMap);
    lenient().when(snomed.getDefaultSnomedReleaseVersion()).thenReturn(defaultReleaseVersion);
    lenient().when(snomed.getSnomedGraph()).thenReturn(snomedGraph);
    lenient().when(snomedGraph.getDirectChildren(any(), any())).thenReturn(new HashSet<>());
    lenient().when(snomedGraph.getDirectParents(any(), any())).thenReturn(new HashSet<>());

    lookupOperation = new SnomedCodeSystemLookupOperation(snomedCsService);
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a concept with a single preferred English synonym. */
  private SnomedConcept createSimpleConcept() {
    SnomedConcept concept = createConcept(CONCEPT_CODE);
    concept.setActive(true);
    concept.setModuleId(MODULE_CORE_ID);
    concept.setDefinitionStatusId("900000000000074008");

    SnomedConceptDescription description = createDescription("en", SYNONYM_TYPE_ID, EN_TERM);
    description.getLanguageList().add(createLanguage(MODULE_CORE_ID, REFSET_EN_GB, PREFERRED));
    concept.getSnomedConceptDescriptionList().add(description);

    return concept;
  }

  /** Creates a concept with both an English and a German preferred synonym. */
  private SnomedConcept createConceptWithEnAndDeDesignations() {
    SnomedConcept concept = createConcept(CONCEPT_CODE);
    concept.setActive(true);
    concept.setModuleId(MODULE_CORE_ID);
    concept.setDefinitionStatusId("900000000000074008");

    SnomedConceptDescription enDescription = createDescription("en", SYNONYM_TYPE_ID, EN_TERM);
    enDescription.getLanguageList().add(createLanguage(MODULE_CORE_ID, REFSET_EN_GB, PREFERRED));
    concept.getSnomedConceptDescriptionList().add(enDescription);

    SnomedConceptDescription deDescription = createDescription("de", SYNONYM_TYPE_ID, DE_TERM);
    deDescription.getLanguageList().add(createLanguage(MODULE_DE_ID, REFSET_DE_DE, PREFERRED));
    concept.getSnomedConceptDescriptionList().add(deDescription);

    return concept;
  }

  /**
   * Calls the primary string-based lookupCode with default version and empty language selection.
   */
  private CsLookupResult doLookup(String code, Set<String> properties)
      throws InvalidRequestException, UnsupportedTerminologyException {
    return lookupOperation.lookupCode(
        SnomedConstants.TERMINOLOGY_CANONICAL_NAME,
        null,
        code,
        LanguageSelectionParameter.builder().build(),
        properties);
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
        throws UnknownConceptException, UnsupportedTerminologyException, NotImplementedException {
      // GIVEN
      Coding coding = new Coding(SnomedConstants.TERMINOLOGY_CANONICAL_NAME, CONCEPT_CODE, null);
      CsLookupParameter param = CsLookupParameter.builder().codingParam(coding).build();
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(createSimpleConcept());

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(param);

      // THEN
      assertThat(result.isFound()).isTrue();
      assertThat(result.getSearchedForCode()).isEqualTo(CONCEPT_CODE);
      assertThat(result.getSearchedForSystem())
          .isEqualTo(SnomedConstants.TERMINOLOGY_CANONICAL_NAME);
    }

    @Test
    @DisplayName("Should process system and code parameters directly")
    void shouldProcessSystemAndCodeParameters()
        throws UnknownConceptException, UnsupportedTerminologyException, NotImplementedException {
      // GIVEN
      CsLookupParameter param =
          CsLookupParameter.builder()
              .systemParam(new UriType(SnomedConstants.TERMINOLOGY_CANONICAL_NAME))
              .codeParam(new CodeType(CONCEPT_CODE))
              .build();
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(createSimpleConcept());

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(param);

      // THEN
      assertThat(result.isFound()).isTrue();
    }

    @Test
    @DisplayName("Should add url as default property when no property parameter is present")
    void shouldAddUrlAsDefaultPropertyWhenNoPropertyParamPresent()
        throws UnknownConceptException, UnsupportedTerminologyException, NotImplementedException {
      // GIVEN
      CsLookupParameter param =
          CsLookupParameter.builder()
              .systemParam(new UriType(SnomedConstants.TERMINOLOGY_CANONICAL_NAME))
              .codeParam(new CodeType(CONCEPT_CODE))
              .propertyParam(null)
              .build();
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(createSimpleConcept());

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
    @DisplayName("Should use default release version when no version is provided")
    void shouldUseDefaultVersionWhenVersionIsNull()
        throws UnknownConceptException, UnsupportedTerminologyException {
      // GIVEN
      when(snomed.getSnomedConceptWithDescription(any(), eq(defaultReleaseVersion), anyBoolean()))
          .thenReturn(createSimpleConcept());

      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getCodeSystemVersion()).isEqualTo(defaultReleaseVersion.getFhirVersion());
    }

    @Test
    @DisplayName("Should use the specified release version when it is present in the version map")
    void shouldUseSpecifiedVersionWhenPresentInVersionMap()
        throws UnknownConceptException, UnsupportedTerminologyException {
      // GIVEN
      String fhirVersion = defaultReleaseVersion.getFhirVersion();
      when(snomed.getSnomedConceptWithDescription(any(), eq(defaultReleaseVersion), anyBoolean()))
          .thenReturn(createSimpleConcept());

      // WHEN
      CsLookupResult result =
          lookupOperation.lookupCode(
              SnomedConstants.TERMINOLOGY_CANONICAL_NAME,
              fhirVersion,
              CONCEPT_CODE,
              LanguageSelectionParameter.builder().build(),
              Set.of("url"));

      // THEN
      assertThat(result.getCodeSystemVersion()).isEqualTo(fhirVersion);
    }

    @Test
    @DisplayName("Should throw UnsupportedTerminologyException for an unknown version")
    void shouldThrowForUnknownVersion() {
      // GIVEN
      String unknownVersion = "http://snomed.info/sct/900000000000207008/version/19991231";

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  lookupOperation.lookupCode(
                      SnomedConstants.TERMINOLOGY_CANONICAL_NAME,
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
    void setUpConceptLookup() throws UnknownConceptException {
      // lenient: some tests override this stub with a different concept
      lenient()
          .when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(createSimpleConcept());
    }

    @Test
    @DisplayName("Should set found=true when the concept is resolved")
    void shouldSetFoundTrueWhenConceptResolved()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.isFound()).isTrue();
    }

    @Test
    @DisplayName("Should set the code system display name to the SNOMED CT terminology name")
    void shouldSetCodeSystemDisplayName()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getCodeSystemDisplayName())
          .isEqualTo(SnomedConstants.TERMINOLOGY_DISPLAY_NAME);
    }

    @Test
    @DisplayName("Should set the code system version to the resolved FHIR version string")
    void shouldSetCodeSystemVersion()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getCodeSystemVersion()).isEqualTo(defaultReleaseVersion.getFhirVersion());
    }

    @Test
    @DisplayName("Should set the code display from the preferred English designation")
    void shouldSetCodeDisplayFromPreferredDesignation()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getCodeDisplay()).isEqualTo(EN_TERM);
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Designation handling Tests")
  class DesignationTests {

    @BeforeEach
    void setUpConceptWithDesignations() throws UnknownConceptException {
      SnomedConcept conceptWithBothDesignations = createConceptWithEnAndDeDesignations();
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(conceptWithBothDesignations);
    }

    @Test
    @DisplayName("Should include all designations when 'designation' property is requested")
    void shouldIncludeAllDesignationsWhenDesignationPropertyRequested()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("designation"));

      // THEN
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactlyInAnyOrder(EN_TERM, DE_TERM);
    }

    @Test
    @DisplayName("Should include only matching language designations when lang.de is requested")
    void shouldIncludeOnlyMatchingDesignationsForLangX()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN - request only German designations
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("lang.de"));

      // THEN - only the German designation is included
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactly(DE_TERM);
    }

    @Test
    @DisplayName(
        "Should fall back to all designations when lang.X contains no supported language code")
    void shouldIncludeAllDesignationsWhenLangXIsUnsupported()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // GIVEN - "lang.xyz" is not a supported BCP 47 language code
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("lang.xyz"));

      // THEN - both designations are included as fallback
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactlyInAnyOrder(EN_TERM, DE_TERM);
    }

    @Test
    @DisplayName(
        "Should filter to lang.X language when both 'designation' and lang.X are requested")
    void shouldIncludeAllDesignationsWhenDesignationAndLangXAreRequested()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // GIVEN - when valid lang codes are extracted, language filtering applies even if
      // 'designation'
      // is also requested (the lang code set is non-empty → filter wins)
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("designation", "lang.de"));

      // THEN - only German designations are returned because lang.de yields a non-empty code set
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactly(DE_TERM);
    }

    @Test
    @DisplayName(
        "Should include designations in the default response when no properties are requested")
    void shouldIncludeDesignationsInDefaultResponseWhenNoPropertiesAreRequested()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // GIVEN - empty property set triggers default expansion, which includes "designation"

      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, new HashSet<>());

      // THEN
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactlyInAnyOrder(EN_TERM, DE_TERM);
    }

    @Test
    @DisplayName(
        "Should include no designations when neither 'designation' nor lang.X is requested")
    void shouldIncludeNoDesignationsWhenNotRequested()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("url"));

      // THEN
      assertThat(result.getDesignations()).isEmpty();
    }

    @Test
    @DisplayName(
        "Should include English designations when lang.en is requested via BCP 47 expansion")
    void shouldIncludeEnglishDesignationsViaBaseLanguageExpansion()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // GIVEN - "lang.en" expands to include "en-GB", "en-US", etc.
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("lang.en"));

      // THEN
      assertThat(result.getDesignations())
          .extracting(ConceptDesignation::getValue)
          .containsExactly(EN_TERM);
    }

    @Test
    @DisplayName("Should map language and use coding to the ConceptDesignation output")
    void shouldMapDesignationFieldsCorrectly()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("designation"));

      // THEN
      assertThat(result.getDesignations())
          .filteredOn(d -> EN_TERM.equals(d.getValue()))
          .hasSize(1)
          .first()
          .satisfies(
              d -> {
                assertThat(d.getLanguage()).isEqualTo("en-GB");
                assertThat(d.getUseSystem()).isEqualTo(SnomedConstants.TERMINOLOGY_CANONICAL_NAME);
                assertThat(d.getUseCode()).isEqualTo(SYNONYM_TYPE_ID);
              });
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Concept property Tests")
  class ConceptPropertyTests {

    @BeforeEach
    void setUpConceptLookup() throws UnknownConceptException {
      // lenient: some tests override this stub with a different concept
      lenient()
          .when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(createSimpleConcept());
    }

    @Test
    @DisplayName("Should add url property with SNOMED CT canonical URL when requested")
    void shouldAddUrlPropertyWhenRequested()
        throws InvalidRequestException, UnsupportedTerminologyException {
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
                assertThat(value.getValue()).isEqualTo(CanonicalUrl.SNOMEDCT);
              });
    }

    @Test
    @DisplayName("Should not add url property when it is not requested")
    void shouldNotAddUrlPropertyWhenNotRequested()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("inactive"));

      // THEN
      assertThat(result.getProperties())
          .isNotEmpty()
          .extracting(GenericConceptProperty::getPropertyName)
          .doesNotContain("url");
    }

    @Test
    @DisplayName("Should add inactive=false for an active concept")
    void shouldAddInactiveFalseForActiveConcept()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("inactive"));

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> ConceptProperties.PROPERTY_NAME_INACTIVE.equals(p.getPropertyName()))
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
        throws UnknownConceptException, InvalidRequestException, UnsupportedTerminologyException {
      // GIVEN
      SnomedConcept inactiveConcept = createConcept(CONCEPT_CODE);
      inactiveConcept.setActive(false);
      inactiveConcept.setModuleId(MODULE_CORE_ID);
      inactiveConcept.setDefinitionStatusId("900000000000074008");
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(inactiveConcept);

      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of("inactive"));

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> ConceptProperties.PROPERTY_NAME_INACTIVE.equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .satisfies(
              p -> {
                BooleanType value = (BooleanType) ((BaseTypeConceptProperty) p).getValue();
                assertThat(value.booleanValue()).isTrue();
              });
    }

    @Test
    @DisplayName("Should add sufficientlyDefined=true when the definition status matches")
    void shouldAddSufficientlyDefinedTrueWhenStatusMatches()
        throws UnknownConceptException, InvalidRequestException, UnsupportedTerminologyException {
      // GIVEN
      SnomedConcept sufficientlyDefinedConcept = createSimpleConcept();
      sufficientlyDefinedConcept.setDefinitionStatusId(SC_SUFFICENTLYDEFINED);
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(sufficientlyDefinedConcept);

      // WHEN
      CsLookupResult result =
          doLookup(CONCEPT_CODE, Set.of(ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED));

      // THEN
      assertThat(result.getProperties())
          .filteredOn(
              p -> ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED.equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .satisfies(
              p -> {
                BooleanType value = (BooleanType) ((BaseTypeConceptProperty) p).getValue();
                assertThat(value.booleanValue()).isTrue();
              });
    }

    @Test
    @DisplayName("Should add moduleId property with the concept's module identifier")
    void shouldAddModuleIdProperty()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result =
          doLookup(CONCEPT_CODE, Set.of(ConceptProperties.PROPERTY_NAME_MODULE_ID));

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> ConceptProperties.PROPERTY_NAME_MODULE_ID.equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .satisfies(
              p -> {
                StringType value = (StringType) ((BaseTypeConceptProperty) p).getValue();
                assertThat(value.getValue()).isEqualTo(MODULE_CORE_ID);
              });
    }

    @Test
    @DisplayName("Should add child properties for each direct child concept")
    void shouldAddChildPropertiesForDirectChildren()
        throws InvalidRequestException, UnsupportedTerminologyException, UnknownConceptException {
      // GIVEN
      when(snomedGraph.getDirectChildren(eq(CONCEPT_CODE), any()))
          .thenReturn(new HashSet<>(Set.of("111111001", "222222002")));

      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, Set.of(ConceptProperties.PROPERTY_NAME_CHILD));

      // THEN
      assertThat(result.getProperties())
          .extracting(GenericConceptProperty::getPropertyName)
          .containsOnly(
              ConceptProperties.PROPERTY_NAME_CHILD, ConceptProperties.PROPERTY_NAME_CHILD);
      assertThat(result.getProperties())
          .filteredOn(p -> ConceptProperties.PROPERTY_NAME_CHILD.equals(p.getPropertyName()))
          .hasSize(2);
    }

    @Test
    @DisplayName("Should add parent properties for each direct parent concept")
    void shouldAddParentPropertiesForDirectParents()
        throws InvalidRequestException, UnsupportedTerminologyException, UnknownConceptException {
      // GIVEN
      when(snomedGraph.getDirectParents(eq(CONCEPT_CODE), any()))
          .thenReturn(new HashSet<>(Set.of("999999001")));

      // WHEN
      CsLookupResult result =
          doLookup(CONCEPT_CODE, Set.of(ConceptProperties.PROPERTY_NAME_PARENT));

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> ConceptProperties.PROPERTY_NAME_PARENT.equals(p.getPropertyName()))
          .hasSize(1);
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when child lookup fails with unknown concept")
    void shouldThrowWhenChildLookupFailsWithUnknownConcept() throws Exception {
      // GIVEN
      when(snomedGraph.getDirectChildren(eq(CONCEPT_CODE), any()))
          .thenThrow(new UnknownConceptException("unknown concept"));

      Set<String> properties = Set.of(ConceptProperties.PROPERTY_NAME_CHILD);
      // WHEN / THEN
      assertThatThrownBy(() -> doLookup(CONCEPT_CODE, properties))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unknown concept");
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when parent lookup fails with unknown concept")
    void shouldThrowWhenParentLookupFailsWithUnknownConcept() throws Exception {
      // GIVEN
      when(snomedGraph.getDirectParents(eq(CONCEPT_CODE), any()))
          .thenThrow(new UnknownConceptException("unknown concept"));

      Set<String> properties = Set.of(ConceptProperties.PROPERTY_NAME_PARENT);
      // WHEN / THEN
      assertThatThrownBy(() -> doLookup(CONCEPT_CODE, properties))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unknown concept");
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Expression fallback Tests")
  class ExpressionFallbackTests {

    private Expression mockExpression;

    @BeforeEach
    void setUpConceptNotFound() throws UnknownConceptException {
      when(snomed.getSnomedConceptWithDescription(any(), any(), anyBoolean()))
          .thenThrow(new UnknownConceptException("not found"));
      mockExpression = mock(Expression.class);
    }

    @Test
    @DisplayName("Should set found=true and populate display when a valid expression is resolved")
    void shouldSucceedForValidExpression() throws Exception {
      // GIVEN
      String termString = "Diabetes mellitus (neonatal onset)";
      when(expressionParser.parseExpression(EXPRESSION_CODE)).thenReturn(mockExpression);
      when(scgProcessor.toTermString(eq(mockExpression), any())).thenReturn(termString);
      when(mockExpression.toString()).thenReturn(EXPRESSION_CODE);

      // WHEN
      CsLookupResult result = doLookup(EXPRESSION_CODE, Set.of("url"));

      // THEN
      assertThat(result.isFound()).isTrue();
      assertThat(result.getCodeDisplay()).isEqualTo(termString);
    }

    @Test
    @DisplayName(
        "Should not include designations for expressions: concept property block is skipped")
    void shouldIncludeExpressionDesignationsWhenRequested() throws Exception {
      // GIVEN
      String termString = "Diabetes mellitus (neonatal onset)";
      when(expressionParser.parseExpression(EXPRESSION_CODE)).thenReturn(mockExpression);
      when(scgProcessor.toTermString(eq(mockExpression), any())).thenReturn(termString);
      when(mockExpression.toString()).thenReturn(EXPRESSION_CODE);

      // WHEN
      CsLookupResult result = doLookup(EXPRESSION_CODE, Set.of("designation"));

      // THEN - addConceptInformation is skipped for expressions, so designations are not populated
      assertThat(result.getDesignations()).isEmpty();
    }

    @Test
    @DisplayName(
        "Should not add concept-specific properties (child, parent, inactive) for expressions")
    void shouldNotAddConceptPropertiesForExpressions() throws Exception {
      // GIVEN
      when(expressionParser.parseExpression(EXPRESSION_CODE)).thenReturn(mockExpression);
      when(scgProcessor.toTermString(eq(mockExpression), any())).thenReturn("some term");
      when(mockExpression.toString()).thenReturn(EXPRESSION_CODE);

      // WHEN
      CsLookupResult result =
          doLookup(EXPRESSION_CODE, Set.of("child", "parent", "inactive", "moduleId"));

      // THEN - no concept properties are added for expressions
      assertThat(result.getProperties()).isEmpty();
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when expression is valid but expression processing is disabled")
    void shouldThrowWhenExpressionProcessingIsDisabled() throws Exception {
      // GIVEN
      when(snomedTerminologyManager.isExpressionProcessingEnabled()).thenReturn(false);

      Set<String> properties = Set.of("url");
      // WHEN / THEN
      assertThatThrownBy(() -> doLookup(EXPRESSION_CODE, properties))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessage(
              String.format(
                  SnomedConstants.EXPRESSION_PROCESSING_DISABLED_MESSAGE, EXPRESSION_CODE));

      verify(expressionParser, never()).parseExpression(any());
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when code is neither a concept nor a valid expression")
    void shouldThrowWhenCodeIsNeitherConceptNorExpression() {
      // GIVEN - expression parsing also fails
      when(expressionParser.parseExpression(any()))
          .thenThrow(new SCGException("invalid expression", new RuntimeException("test")));

      // WHEN / THEN
      Set<String> properties = Set.of("url");
      assertThatThrownBy(() -> doLookup("invalid-code", properties))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(
              "neither a valid SNOMED CT concept code nor a valid SNOMED CT expression");
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Restricted mode Tests")
  class RestrictedModeTests {

    @BeforeEach
    void setUpRestrictedMode() throws UnknownConceptException {
      when(snomedTerminologyManager.isRestrictedMode()).thenReturn(true);
      lookupOperation = new SnomedCodeSystemLookupOperation(snomedCsService);

      lenient()
          .when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(createSimpleConcept());
    }

    /** Creates a lookup request for {@link #CONCEPT_CODE} with the given properties. */
    private CsLookupParameter createLookupParameter(String... properties) {
      return CsLookupParameter.builder()
          .systemParam(new UriType(SnomedConstants.TERMINOLOGY_CANONICAL_NAME))
          .codeParam(new CodeType(CONCEPT_CODE))
          .propertyParam(Arrays.stream(properties).map(CodeType::new).toList())
          .build();
    }

    @ParameterizedTest(name = "property ''{0}''")
    @ValueSource(strings = {"child", "parent", "sufficientlyDefined"})
    @DisplayName("Should reject properties exposing relationships or definition status")
    void shouldRejectRestrictedProperty(String property) {
      // GIVEN
      CsLookupParameter param =
          createLookupParameter(ConceptProperties.PROPERTY_NAME_URL, property);

      // WHEN / THEN
      assertThatThrownBy(() -> lookupOperation.lookupCode(param))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("[" + property + "]")
          .hasMessageContaining("restricted mode");

      verifyNoInteractions(snomedGraph);
    }

    @Test
    @DisplayName("Should reject restricted properties requested via a Coding")
    void shouldRejectRestrictedPropertyForCodingRequest() {
      // GIVEN
      CsLookupParameter param =
          CsLookupParameter.builder()
              .codingParam(
                  new Coding(SnomedConstants.TERMINOLOGY_CANONICAL_NAME, CONCEPT_CODE, null))
              .propertyParam(List.of(new CodeType(ConceptProperties.PROPERTY_NAME_PARENT)))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> lookupOperation.lookupCode(param))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("[parent]");

      verifyNoInteractions(snomedGraph);
    }

    @Test
    @DisplayName("Should list all requested restricted properties in the error message")
    void shouldListAllRestrictedPropertiesInMessage() {
      // GIVEN
      CsLookupParameter param = createLookupParameter("parent", "child", "inactive");

      // WHEN / THEN
      assertThatThrownBy(() -> lookupOperation.lookupCode(param))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("[child, parent]");
    }

    @Test
    @DisplayName("Should return only unrestricted properties when no properties are requested")
    void shouldReturnOnlyUnrestrictedDefaultProperties()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, new HashSet<>());

      // THEN
      assertThat(result.isFound()).isTrue();
      assertThat(result.getProperties())
          .extracting(GenericConceptProperty::getPropertyName)
          .contains(
              ConceptProperties.PROPERTY_NAME_URL,
              ConceptProperties.PROPERTY_NAME_INACTIVE,
              ConceptProperties.PROPERTY_NAME_MODULE_ID)
          .doesNotContain(
              ConceptProperties.PROPERTY_NAME_CHILD,
              ConceptProperties.PROPERTY_NAME_PARENT,
              ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED);
      assertThat(result.getDesignations()).isNotEmpty();

      verifyNoInteractions(snomedGraph);
    }

    @Test
    @DisplayName("Should reject expressions with a message referring to restricted mode")
    void shouldRejectExpressionsWithRestrictedModeMessage() throws UnknownConceptException {
      // GIVEN
      when(snomedTerminologyManager.isExpressionProcessingEnabled()).thenReturn(false);
      when(snomed.getSnomedConceptWithDescription(eq(EXPRESSION_CODE), any(), anyBoolean()))
          .thenThrow(new UnknownConceptException("not found"));
      Set<String> properties = Set.of(ConceptProperties.PROPERTY_NAME_URL);

      // WHEN / THEN
      assertThatThrownBy(() -> doLookup(EXPRESSION_CODE, properties))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessage(
              String.format(
                  SnomedConstants.EXPRESSION_PROCESSING_RESTRICTED_MESSAGE, EXPRESSION_CODE));

      verifyNoInteractions(expressionParser, scgProcessor);
    }

    @Test
    @DisplayName("Should return explicitly requested unrestricted properties")
    void shouldReturnRequestedUnrestrictedProperties()
        throws InvalidRequestException, UnsupportedTerminologyException {
      // WHEN
      CsLookupResult result =
          doLookup(CONCEPT_CODE, Set.of("inactive", "moduleId", "designation", "lang.en"));

      // THEN
      assertThat(result.getProperties())
          .extracting(GenericConceptProperty::getPropertyName)
          .containsExactlyInAnyOrder(
              ConceptProperties.PROPERTY_NAME_INACTIVE, ConceptProperties.PROPERTY_NAME_MODULE_ID);
      assertThat(result.getDesignations()).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("lookupCode(String, ...) - Default properties outside restricted mode Tests")
  class UnrestrictedDefaultPropertyTests {

    @Test
    @DisplayName("Should include relationship and definition status properties by default")
    void shouldIncludeRelationshipPropertiesByDefault()
        throws UnknownConceptException, InvalidRequestException, UnsupportedTerminologyException {
      // GIVEN
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(createSimpleConcept());
      when(snomedGraph.getDirectParents(eq(CONCEPT_CODE), any())).thenReturn(Set.of("1"));
      when(snomedGraph.getDirectChildren(eq(CONCEPT_CODE), any())).thenReturn(Set.of("2"));

      // WHEN
      CsLookupResult result = doLookup(CONCEPT_CODE, new HashSet<>());

      // THEN
      assertThat(result.getProperties())
          .extracting(GenericConceptProperty::getPropertyName)
          .contains(
              ConceptProperties.PROPERTY_NAME_CHILD,
              ConceptProperties.PROPERTY_NAME_PARENT,
              ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED);
    }

    @Test
    @DisplayName("Should report sufficientlyDefined=false when the definition status is missing")
    void shouldHandleMissingDefinitionStatus()
        throws UnknownConceptException, InvalidRequestException, UnsupportedTerminologyException {
      // GIVEN
      SnomedConcept concept = createSimpleConcept();
      concept.setDefinitionStatusId(null);
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
          .thenReturn(concept);

      // WHEN
      CsLookupResult result =
          doLookup(CONCEPT_CODE, Set.of(ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED));

      // THEN
      assertThat(result.getProperties())
          .singleElement()
          .satisfies(
              p -> {
                BooleanType value = (BooleanType) ((BaseTypeConceptProperty) p).getValue();
                assertThat(value.booleanValue()).isFalse();
              });
    }
  }
}

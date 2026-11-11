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
package de.gematik.zts.termzilla.terminologies.loinc.provider.cs.validatecode;

import static org.assertj.core.api.Assertions.assertThat;
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
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.loinc.Loinc;
import de.gematik.zts.termzilla.terminologies.loinc.LoincCodeSystemService;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import de.gematik.zts.termzilla.terminologies.loinc.LoincTerminologyManager;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
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
 * Unit tests for {@link LoincCsValidateCodeOperation}.
 *
 * <p>Covers unsupported parameter rejection, version resolution, code lookup, display validation
 * with and without language constraints, BCP-47 language expansion, case-insensitive matching,
 * concept type handling (Term, Answer, AnswerList), and language-aware display selection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoincCsValidateCodeOperation Tests")
class LoincCsValidateCodeOperationTest {

  private static final String TEST_VERSION = "2.81";

  /** Numeric LOINC Term — default designation type: {@code LONG_COMMON_NAME}. */
  private static final String TERM_CODE = "2823-3";

  private static final String TERM_EN = "Potassium [Moles/volume] in Serum or Plasma";

  /** LOINC Answer — default designation type: {@code DisplayText}. */
  private static final String LA_CODE = "LA33-6";

  private static final String LA_DISPLAY = "Yes";

  /** LOINC AnswerList — default designation type: {@code AnswerListName}. */
  private static final String LL_CODE = "LL1000-0";

  private static final String LL_DISPLAY = "PhenX05_13_30D bread amt";

  /** Bilingual numeric LOINC Term — {@code LONG_COMMON_NAME} in en-US and de-DE. */
  private static final String BILINGUAL_CODE = "14394-1";

  private static final String BILINGUAL_EN = "Urea nitrogen [Mass/volume] in Pleural fluid";
  private static final String BILINGUAL_DE =
      "Harnstoff-Stickstoff [Masse/Volumen] in Pleuraflüssigkeit";

  @Mock private LoincCodeSystemService loincCodeSystemService;
  @Mock private LoincTerminologyManager loincTerminologyManager;
  @Mock private Loinc loinc;

  private LoincVersion defaultLoincVersion;
  private LoincCsValidateCodeOperation operation;

  @BeforeEach
  void setUp() {
    defaultLoincVersion =
        LoincVersion.builder().versionNumber(281).versionString(TEST_VERSION).build();

    Map<String, LoincVersion> versionMap = new HashMap<>();
    versionMap.put(TEST_VERSION, defaultLoincVersion);

    when(loincCodeSystemService.getLoincTerminologyManager()).thenReturn(loincTerminologyManager);
    when(loincTerminologyManager.getLoinc()).thenReturn(loinc);

    lenient().when(loinc.getDefaultLoincVersion()).thenReturn(defaultLoincVersion);
    lenient().when(loincCodeSystemService.getFhirVersionToLoincVersionMap()).thenReturn(versionMap);
    lenient()
        .when(loinc.getSupportedLanguages())
        .thenReturn(List.of(LoincConstants.LOINC_LANGUAGE, "de-DE"));

    operation = new LoincCsValidateCodeOperation(loincCodeSystemService);
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a LOINC concept with a single en-US designation of the given type. */
  private static LoincConcept createConcept(String code, String designationType, String display) {
    LoincConcept concept = new LoincConcept();
    concept.setCode(code);
    concept.setActive(true);
    concept
        .getDesignationList()
        .add(createDesignation(LoincConstants.LOINC_LANGUAGE, designationType, display));
    return concept;
  }

  /**
   * Creates a bilingual LOINC Term concept for {@code 14394-1} with en-US and de-DE {@code
   * LONG_COMMON_NAME} designations.
   */
  private static LoincConcept createBilingualTermConcept() {
    LoincConcept concept = new LoincConcept();
    concept.setCode(BILINGUAL_CODE);
    concept.setActive(true);
    concept
        .getDesignationList()
        .add(
            createDesignation(
                LoincConstants.LOINC_LANGUAGE,
                LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME,
                BILINGUAL_EN));
    concept
        .getDesignationList()
        .add(
            createDesignation(
                "de-DE", LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, BILINGUAL_DE));
    return concept;
  }

  private static LoincDesignation createDesignation(String language, String type, String value) {
    LoincDesignation designation = new LoincDesignation();
    designation.setLanguage(language);
    designation.setType(type);
    designation.setDesignation(value);
    return designation;
  }

  private static CsValidateCodeParameter buildParam(String code) {
    return CsValidateCodeParameter.builder()
        .urlParam(new UriType(LoincConstants.TERMINOLOGY_CANONICAL_NAME))
        .codeParam(new CodeType(code))
        .build();
  }

  private static CsValidateCodeParameter buildParamWithDisplay(String code, String display) {
    return CsValidateCodeParameter.builder()
        .urlParam(new UriType(LoincConstants.TERMINOLOGY_CANONICAL_NAME))
        .codeParam(new CodeType(code))
        .displayParam(new StringType(display))
        .build();
  }

  private static CsValidateCodeParameter buildParamWithDisplayAndLanguage(
      String code, String display, String displayLanguage) {
    return CsValidateCodeParameter.builder()
        .urlParam(new UriType(LoincConstants.TERMINOLOGY_CANONICAL_NAME))
        .codeParam(new CodeType(code))
        .displayParam(display != null ? new StringType(display) : null)
        .displayLanguageParam(new CodeType(displayLanguage))
        .build();
  }

  private static CsValidateCodeParameter buildParamWithVersion(String code, String version) {
    return CsValidateCodeParameter.builder()
        .urlParam(new UriType(LoincConstants.TERMINOLOGY_CANONICAL_NAME))
        .codeParam(new CodeType(code))
        .versionParam(new StringType(version))
        .build();
  }

  private void stubConcept(LoincConcept concept)
      throws UnknownConceptException, InactiveConceptException {
    when(loinc.getLoincConcept(eq(concept.getCode()), any(LoincVersion.class), anyBoolean(), any()))
        .thenReturn(concept);
  }

  private void stubConceptNotFound(String code)
      throws UnknownConceptException, InactiveConceptException {
    when(loinc.getLoincConcept(eq(code), any(LoincVersion.class), anyBoolean(), any()))
        .thenThrow(new UnknownConceptException("Code '" + code + "' not found."));
  }

  // ==========================================================================
  // Test classes
  // ==========================================================================

  @Nested
  @DisplayName("Unsupported parameters")
  class UnsupportedParametersTest {

    @Test
    @DisplayName("Should throw NotImplementedException when date parameter is provided")
    void shouldThrowNotImplementedExceptionWhenDateParameterIsProvided() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(LoincConstants.TERMINOLOGY_CANONICAL_NAME))
              .codeParam(new CodeType(TERM_CODE))
              .dateParam(new DateTimeType("2024-01-01"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> operation.validateCode(param, LanguageParameter.parse(null)))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("date");
    }

    @Test
    @DisplayName("Should throw NotImplementedException when abstract parameter is set to false")
    void shouldThrowNotImplementedExceptionWhenAbstractParameterIsFalse() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(LoincConstants.TERMINOLOGY_CANONICAL_NAME))
              .codeParam(new CodeType(TERM_CODE))
              .abstractParam(new BooleanType(false))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> operation.validateCode(param, LanguageParameter.parse(null)))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("abstract");
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Code parameter validation")
  class CodeValidationTest {

    @Test
    @DisplayName("Should throw InvalidRequestException when code parameter is null")
    void shouldThrowInvalidRequestExceptionWhenCodeIsNull() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(LoincConstants.TERMINOLOGY_CANONICAL_NAME))
              .build();
      LanguageParameter languageParameter = LanguageParameter.parse(null);

      // WHEN / THEN
      assertThatThrownBy(() -> operation.validateCode(param, languageParameter))
          .isInstanceOf(InvalidRequestException.class);
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Version resolution")
  class VersionResolutionTest {

    @Test
    @DisplayName("Should use the default LOINC version when no version parameter is provided")
    void shouldUseDefaultVersionWhenVersionParamIsAbsent()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(TERM_CODE, LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, TERM_EN);
      when(loinc.getLoincConcept(eq(TERM_CODE), eq(defaultLoincVersion), anyBoolean(), any()))
          .thenReturn(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(buildParam(TERM_CODE), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should use the specified LOINC version when it is known")
    void shouldUseSpecifiedVersionWhenPresent()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(TERM_CODE, LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, TERM_EN);
      when(loinc.getLoincConcept(eq(TERM_CODE), eq(defaultLoincVersion), anyBoolean(), any()))
          .thenReturn(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithVersion(TERM_CODE, TEST_VERSION), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when the requested LOINC version is unknown")
    void shouldThrowForUnknownVersion() {
      // GIVEN
      String unknownVersion = "9.99";
      CsValidateCodeParameter param = buildParamWithVersion(TERM_CODE, unknownVersion);
      LanguageParameter languageParameter = LanguageParameter.parse(null);

      // WHEN / THEN
      assertThatThrownBy(() -> operation.validateCode(param, languageParameter))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(unknownVersion);
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Code lookup")
  class CodeLookupTest {

    @Test
    @DisplayName("Should return result=false when the code is not found in LOINC")
    void shouldReturnFalseWhenCodeNotFound()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      stubConceptNotFound(TERM_CODE);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(buildParam(TERM_CODE), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains(TERM_CODE);
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Concept type handling")
  class ConceptTypeHandlingTest {

    @Test
    @DisplayName(
        "Should return result=true and display 'Yes' for Answer LA33-6 with DisplayText designation")
    void shouldValidateAndSelectDisplayForAnswerConcept()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(LA_CODE, LoincConstants.LOINC_DESIGNATION_DISPLAYTEXT, LA_DISPLAY);
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplay(LA_CODE, LA_DISPLAY),
              LanguageParameter.parse(LoincConstants.LOINC_LANGUAGE));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo(LA_DISPLAY);
    }

    @Test
    @DisplayName(
        "Should select display from DisplayText designation for LA* code when no display is validated")
    void shouldSelectDisplayFromDisplayTextDesignationForAnswerCode()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(LA_CODE, LoincConstants.LOINC_DESIGNATION_DISPLAYTEXT, LA_DISPLAY);
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParam(LA_CODE), LanguageParameter.parse(LoincConstants.LOINC_LANGUAGE));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo(LA_DISPLAY);
    }

    @Test
    @DisplayName(
        "Should return result=true and display 'PhenX05_13_30D bread amt' for AnswerList LL1000-0 with AnswerListName designation")
    void shouldValidateAndSelectDisplayForAnswerListConcept()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(LL_CODE, LoincConstants.LOINC_DESIGNATION_ANSWERLIST_NAME, LL_DISPLAY);
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplay(LL_CODE, LL_DISPLAY),
              LanguageParameter.parse(LoincConstants.LOINC_LANGUAGE));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo(LL_DISPLAY);
    }

    @Test
    @DisplayName(
        "Should select display from AnswerListName designation for LL* code when no display is validated")
    void shouldSelectDisplayFromAnswerListNameDesignationForAnswerListCode()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(LL_CODE, LoincConstants.LOINC_DESIGNATION_ANSWERLIST_NAME, LL_DISPLAY);
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParam(LL_CODE), LanguageParameter.parse(LoincConstants.LOINC_LANGUAGE));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo(LL_DISPLAY);
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Display validation – without displayLanguage")
  class DisplayValidationWithoutLanguageTest {

    @Test
    @DisplayName("Should return result=true when display is null")
    void shouldReturnTrueWhenDisplayIsNull()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(TERM_CODE, LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, TERM_EN);
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(buildParam(TERM_CODE), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return result=true when display matches exactly")
    void shouldReturnTrueWhenDisplayMatchesExactly()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(TERM_CODE, LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, TERM_EN);
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplay(TERM_CODE, TERM_EN), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=true for case-insensitive match (LOINC has no concept-specific case significance)")
    void shouldReturnTrueForCaseInsensitiveMatch()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(TERM_CODE, LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, TERM_EN);
      stubConcept(concept);

      // WHEN – display provided in lower case
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplay(TERM_CODE, TERM_EN.toLowerCase()),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return result=false when display does not match any designation")
    void shouldReturnFalseWhenDisplayDoesNotMatch()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(TERM_CODE, LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, TERM_EN);
      stubConcept(concept);
      String wrongDisplay = "Totally Wrong Display";

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplay(TERM_CODE, wrongDisplay), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains(wrongDisplay);
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Display validation – with displayLanguage")
  class DisplayValidationWithLanguageTest {

    @Test
    @DisplayName(
        "Should return result=false when no designation exists for the requested displayLanguage")
    void shouldReturnFalseWhenNoDesignationExistsForRequestedLanguage()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN – TERM_CODE has only en-US; "de" is not available
      LoincConcept concept =
          createConcept(TERM_CODE, LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, TERM_EN);
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplayAndLanguage(TERM_CODE, "Irgendwas", "de"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("de").contains(TERM_CODE);
    }

    @Test
    @DisplayName("Should return result=true when display matches the de-DE designation directly")
    void shouldReturnTrueWhenDisplayMatchesDeDeDesignation()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept = createBilingualTermConcept();
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplayAndLanguage(BILINGUAL_CODE, BILINGUAL_DE, "de-DE"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=true via BCP-47 expansion: base 'de' matches 'de-DE' designation")
    void shouldReturnTrueViaBcp47ExpansionFromBaseToRegionalVariant()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN – designation is tagged "de-DE"; displayLanguage is the base language "de"
      LoincConcept concept = createBilingualTermConcept();
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplayAndLanguage(BILINGUAL_CODE, BILINGUAL_DE, "de"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return result=true when display matches the en-US designation")
    void shouldReturnTrueWhenDisplayMatchesEnUsDesignation()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept = createBilingualTermConcept();
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplayAndLanguage(
                  BILINGUAL_CODE, BILINGUAL_EN, LoincConstants.LOINC_LANGUAGE),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=false when display does not match the designation for the requested language")
    void shouldReturnFalseWhenDisplayMismatchForRequestedLanguage()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN – requesting "de-DE" but providing the English display value
      LoincConcept concept = createBilingualTermConcept();
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParamWithDisplayAndLanguage(BILINGUAL_CODE, BILINGUAL_EN, "de-DE"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains(BILINGUAL_EN).contains("de-DE");
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Display selection in result")
  class DisplaySelectionTest {

    @Test
    @DisplayName("Should return en-US display when system default language is en-US")
    void shouldReturnEnUsDisplayWhenSystemDefaultIsEnUs()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept =
          createConcept(TERM_CODE, LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME, TERM_EN);
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParam(TERM_CODE), LanguageParameter.parse(LoincConstants.LOINC_LANGUAGE));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo(TERM_EN);
    }

    @Test
    @DisplayName("Should return German display for 14394-1 when system default language is de-DE")
    void shouldReturnGermanDisplayForBilingualConceptWhenSystemDefaultIsDeDE()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept = createBilingualTermConcept();
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(buildParam(BILINGUAL_CODE), LanguageParameter.parse("de-DE"));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo(BILINGUAL_DE);
    }

    @Test
    @DisplayName("Should return English display for 14394-1 when system default language is en-US")
    void shouldReturnEnglishDisplayForBilingualConceptWhenSystemDefaultIsEnUs()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept = createBilingualTermConcept();
      stubConcept(concept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              buildParam(BILINGUAL_CODE), LanguageParameter.parse(LoincConstants.LOINC_LANGUAGE));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo(BILINGUAL_EN);
    }

    @Test
    @DisplayName(
        "Should fall back to en-US display for 14394-1 when no language preference is set (LOINC standard fallback)")
    void shouldFallBackToEnUsForBilingualConceptWhenNoLanguagePreferenceSet()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN
      LoincConcept concept = createBilingualTermConcept();
      stubConcept(concept);

      // WHEN – no system default language, no Accept-Language header
      ValidateCodeResult result =
          operation.validateCode(buildParam(BILINGUAL_CODE), LanguageParameter.parse(null));

      // THEN – en-US is the LOINC standard language and acts as final fallback
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo(BILINGUAL_EN);
    }

    @Test
    @DisplayName("Should set display=null when the concept has no designations")
    void shouldSetDisplayNullWhenConceptHasNoDesignations()
        throws UnknownConceptException,
            InactiveConceptException,
            NotImplementedException,
            InvalidRequestException {
      // GIVEN – concept without any designations
      LoincConcept emptyConceptConcept = new LoincConcept();
      emptyConceptConcept.setCode(TERM_CODE);
      emptyConceptConcept.setActive(true);
      when(loinc.getLoincConcept(eq(TERM_CODE), any(LoincVersion.class), anyBoolean(), any()))
          .thenReturn(emptyConceptConcept);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(buildParam(TERM_CODE), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isNull();
    }
  }
}

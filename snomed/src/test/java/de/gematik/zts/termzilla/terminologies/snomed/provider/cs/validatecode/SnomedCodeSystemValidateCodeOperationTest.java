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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.validatecode;

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESINSENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_INITIALCHARACTERCASEINSENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_PREFERRED;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_SYNONYM;
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
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import de.gematik.zts.termzilla.terminologies.snomed.scg.DemisSCGProcessor;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.snomed.languages.scg.SCGException;
import org.snomed.languages.scg.SCGExpressionParser;
import org.snomed.languages.scg.domain.model.Expression;

/**
 * Unit tests for {@link SnomedCodeSystemValidateCodeOperation}.
 *
 * <p>Covers unsupported parameter rejection, version resolution, code and expression validation,
 * display validation with and without language constraints (including BCP-47 language expansion),
 * case-significance-aware matching, and display selection fallback behaviour.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SnomedCodeSystemValidateCodeOperation Tests")
class SnomedCodeSystemValidateCodeOperationTest {

  private static final String MODULE_CORE_ID = SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
  private static final String MODULE_DE_ID =
      SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;
  private static final String TEST_DATE = "20240101";
  private static final String CONCEPT_CODE = "73211009";
  private static final String EXPRESSION_CODE =
      "73211009 |Diabetes mellitus| : 246454002 |Occurrence| = 255407002 |Neonatal|";
  private static final String EN_TERM = "Diabetes mellitus";
  private static final String DE_TERM = "Diabetes mellitus (Deutsch)";
  private static final String REFSET_EN_GB = SnomedReleaseVersionConstants.EN_GB;
  private static final String REFSET_DE_DE = SnomedReleaseVersionConstants.DE_DE;

  @Mock private SnomedCodeSystemService snomedCsService;
  @Mock private SnomedTerminologyManager snomedTerminologyManager;
  @Mock private Snomed snomed;
  @Mock private SCGExpressionParser expressionParser;
  @Mock private DemisSCGProcessor scgProcessor;

  private SnomedReleaseVersion defaultReleaseVersion;
  private SnomedCodeSystemValidateCodeOperation validateCodeOperation;

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

    validateCodeOperation = new SnomedCodeSystemValidateCodeOperation(snomedCsService);
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a concept with a single case-insensitive preferred English synonym (en-GB). */
  private SnomedConcept createSimpleEnConcept() {
    SnomedConcept concept = createConcept(CONCEPT_CODE);
    SnomedConceptDescription description = createDescription("en", SC_SYNONYM, EN_TERM);
    description.setCaseSignificanceId(SC_CASESINSENSITIVE);
    description.getLanguageList().add(createLanguage(MODULE_CORE_ID, REFSET_EN_GB, SC_PREFERRED));
    concept.getSnomedConceptDescriptionList().add(description);
    return concept;
  }

  /** Creates a concept with a case-sensitive preferred English synonym (en-GB). */
  private SnomedConcept createCaseSensitiveConcept() {
    SnomedConcept concept = createConcept(CONCEPT_CODE);
    SnomedConceptDescription description = createDescription("en", SC_SYNONYM, EN_TERM);
    description.setCaseSignificanceId(SC_CASESENSITIVE);
    description.getLanguageList().add(createLanguage(MODULE_CORE_ID, REFSET_EN_GB, SC_PREFERRED));
    concept.getSnomedConceptDescriptionList().add(description);
    return concept;
  }

  /**
   * Creates a concept with an initial-character-case-insensitive preferred English synonym (en-GB).
   */
  private SnomedConcept createInitialCharCaseInsensitiveConcept() {
    SnomedConcept concept = createConcept(CONCEPT_CODE);
    SnomedConceptDescription description = createDescription("en", SC_SYNONYM, EN_TERM);
    description.setCaseSignificanceId(SC_INITIALCHARACTERCASEINSENSITIVE);
    description.getLanguageList().add(createLanguage(MODULE_CORE_ID, REFSET_EN_GB, SC_PREFERRED));
    concept.getSnomedConceptDescriptionList().add(description);
    return concept;
  }

  /**
   * Creates a concept with both a preferred English synonym (en-GB) and a preferred German synonym
   * (de-DE), both case-insensitive.
   */
  private SnomedConcept createBilingualConcept() {
    SnomedConcept concept = createConcept(CONCEPT_CODE);

    SnomedConceptDescription enDesc = createDescription("en", SC_SYNONYM, EN_TERM);
    enDesc.setCaseSignificanceId(SC_CASESINSENSITIVE);
    enDesc.getLanguageList().add(createLanguage(MODULE_CORE_ID, REFSET_EN_GB, SC_PREFERRED));
    concept.getSnomedConceptDescriptionList().add(enDesc);

    SnomedConceptDescription deDesc = createDescription("de", SC_SYNONYM, DE_TERM);
    deDesc.setCaseSignificanceId(SC_CASESINSENSITIVE);
    deDesc.getLanguageList().add(createLanguage(MODULE_DE_ID, REFSET_DE_DE, SC_PREFERRED));
    concept.getSnomedConceptDescriptionList().add(deDesc);

    return concept;
  }

  /** Stubs a successful concept lookup for {@link #CONCEPT_CODE}. */
  private void stubConcept(SnomedConcept concept) throws Exception {
    when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_CODE), any(), anyBoolean()))
        .thenReturn(concept);
  }

  /** Stubs the concept lookup to throw an {@link UnknownConceptException} for any code. */
  private void stubConceptNotFound() throws Exception {
    when(snomed.getSnomedConceptWithDescription(any(), any(), anyBoolean()))
        .thenThrow(new UnknownConceptException("unknown"));
  }

  /** Builds a minimal validate-code parameter containing only the given code. */
  private CsValidateCodeParameter buildParam(String code) {
    return CsValidateCodeParameter.builder().codeParam(new CodeType(code)).build();
  }

  /** Builds a validate-code parameter with the given code and display. */
  private CsValidateCodeParameter buildParamWithDisplay(String code, String display) {
    return CsValidateCodeParameter.builder()
        .codeParam(new CodeType(code))
        .displayParam(new StringType(display))
        .build();
  }

  /** Builds a validate-code parameter with code, display, and displayLanguage. */
  private CsValidateCodeParameter buildParamWithDisplayAndLanguage(
      String code, String display, String displayLanguage) {
    return CsValidateCodeParameter.builder()
        .codeParam(new CodeType(code))
        .displayParam(new StringType(display))
        .displayLanguageParam(new CodeType(displayLanguage))
        .build();
  }

  // ==========================================================================
  // Tests
  // ==========================================================================

  @Nested
  @DisplayName("Unsupported parameter tests")
  class UnsupportedParametersTest {

    @Test
    @DisplayName("Should throw NotImplementedException when date parameter is provided")
    void shouldThrowNotImplementedExceptionWhenDateParamIsProvided() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .codeParam(new CodeType(CONCEPT_CODE))
              .dateParam(new DateTimeType("2024-01-01"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> validateCodeOperation.validateCode(param, null))
          .isInstanceOf(NotImplementedException.class);
    }

    @Test
    @DisplayName("Should throw NotImplementedException when coding parameter is provided")
    void shouldThrowNotImplementedExceptionWhenCodingParamIsProvided() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder().codingParam(new Coding()).build();

      // WHEN / THEN
      assertThatThrownBy(() -> validateCodeOperation.validateCode(param, null))
          .isInstanceOf(NotImplementedException.class);
    }

    @Test
    @DisplayName("Should throw NotImplementedException when codeableConcept parameter is provided")
    void shouldThrowNotImplementedExceptionWhenCodeableConceptParamIsProvided() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder().codeableConceptParam(new CodeableConcept()).build();

      // WHEN / THEN
      assertThatThrownBy(() -> validateCodeOperation.validateCode(param, null))
          .isInstanceOf(NotImplementedException.class);
    }

    @Test
    @DisplayName("Should throw NotImplementedException when abstract parameter is set to false")
    void shouldThrowNotImplementedExceptionWhenAbstractParamIsFalse() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .codeParam(new CodeType(CONCEPT_CODE))
              .abstractParam(new BooleanType(false))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> validateCodeOperation.validateCode(param, null))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("abstract");
    }
  }

  @Nested
  @DisplayName("Version resolution tests")
  class VersionResolutionTest {

    @Test
    @DisplayName("Should use default release version when no version parameter is provided")
    void shouldUseDefaultVersionWhenNoVersionParamIsProvided() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param = buildParam(CONCEPT_CODE);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should use the specified release version when version parameter is provided")
    void shouldUseSpecifiedVersionWhenVersionParamIsProvided() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .codeParam(new CodeType(CONCEPT_CODE))
              .versionParam(new StringType(defaultReleaseVersion.getFhirVersion()))
              .build();

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when an unknown version is requested")
    void shouldThrowInvalidRequestExceptionWhenUnknownVersionIsProvided() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .codeParam(new CodeType(CONCEPT_CODE))
              .versionParam(new StringType("http://snomed.info/sct/999999/version/20990101"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> validateCodeOperation.validateCode(param, null))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("20990101");
    }
  }

  @Nested
  @DisplayName("Code validation tests")
  class CodeValidationTest {

    @Test
    @DisplayName("Should return true for a known SNOMED concept without display")
    void shouldReturnTrueForKnownConceptWithoutDisplay() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param = buildParam(CONCEPT_CODE);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getMessage()).isNull();
    }

    @Test
    @DisplayName("Should return true for a valid SCG expression without display")
    void shouldReturnTrueForValidExpressionWithoutDisplay() throws Exception {
      // GIVEN
      stubConceptNotFound();
      Expression expression = mock(Expression.class);
      when(expressionParser.parseExpression(EXPRESSION_CODE)).thenReturn(expression);
      when(scgProcessor.toTermString(any(), any())).thenReturn("Neonatal diabetes mellitus");
      CsValidateCodeParameter param = buildParam(EXPRESSION_CODE);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return false when code is neither a valid concept nor a valid expression")
    void shouldReturnFalseForUnknownCodeAndInvalidExpression() throws Exception {
      // GIVEN
      stubConceptNotFound();
      when(expressionParser.parseExpression(CONCEPT_CODE))
          .thenThrow(new SCGException("invalid expression", new RuntimeException()));
      CsValidateCodeParameter param = buildParam(CONCEPT_CODE);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("neither a valid SNOMED conceptId");
    }

    @Test
    @DisplayName("Should return result=false when expression processing is disabled")
    void shouldReturnFalseWhenExpressionProcessingIsDisabled() throws Exception {
      // GIVEN
      stubConceptNotFound();
      when(snomedTerminologyManager.isExpressionProcessingEnabled()).thenReturn(false);
      CsValidateCodeParameter param = buildParam(EXPRESSION_CODE);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage())
          .isEqualTo(
              String.format(
                  SnomedConstants.EXPRESSION_PROCESSING_DISABLED_MESSAGE, EXPRESSION_CODE));

      verify(expressionParser, never()).parseExpression(any());
    }

    @Test
    @DisplayName("Should return result=false with restricted mode message in restricted mode")
    void shouldReturnFalseWithRestrictedModeMessageInRestrictedMode() throws Exception {
      // GIVEN
      stubConceptNotFound();
      when(snomedTerminologyManager.isExpressionProcessingEnabled()).thenReturn(false);
      when(snomedTerminologyManager.isRestrictedMode()).thenReturn(true);
      CsValidateCodeParameter param = buildParam(EXPRESSION_CODE);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage())
          .isEqualTo(
              String.format(
                  SnomedConstants.EXPRESSION_PROCESSING_RESTRICTED_MESSAGE, EXPRESSION_CODE))
          .doesNotContain("expressionProcessingEnabled")
          .doesNotContain("MRCM");

      verifyNoInteractions(expressionParser, scgProcessor);
    }
  }

  @Nested
  @DisplayName("Expression display validation tests")
  class ExpressionDisplayTest {

    @Test
    @DisplayName("Should return false when display parameter is provided for an SCG expression")
    void shouldReturnFalseWhenDisplayIsProvidedForExpression() throws Exception {
      // GIVEN
      stubConceptNotFound();
      Expression expression = mock(Expression.class);
      when(expressionParser.parseExpression(EXPRESSION_CODE)).thenReturn(expression);
      when(scgProcessor.toTermString(any(), any())).thenReturn("Neonatal diabetes mellitus");
      CsValidateCodeParameter param = buildParamWithDisplay(EXPRESSION_CODE, "some display");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("display parameter is currently not supported");
    }
  }

  @Nested
  @DisplayName("Display validation without language constraint tests")
  class DisplayValidationWithoutLanguageTest {

    @Test
    @DisplayName("Should return true when display is null")
    void shouldReturnTrueWhenDisplayIsNull() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param = buildParam(CONCEPT_CODE);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getMessage()).isNull();
    }

    @Test
    @DisplayName("Should return true for case-insensitive display match")
    void shouldReturnTrueForCaseInsensitiveDisplayMatch() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept()); // SNOMED_CONCEPT_CASESINSENSITIVE
      CsValidateCodeParameter param = buildParamWithDisplay(CONCEPT_CODE, "diabetes mellitus");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return true for exact case-sensitive display match")
    void shouldReturnTrueForExactCaseSensitiveDisplayMatch() throws Exception {
      // GIVEN
      stubConcept(createCaseSensitiveConcept()); // SNOMED_CONCEPT_CASESENSITIVE
      CsValidateCodeParameter param = buildParamWithDisplay(CONCEPT_CODE, EN_TERM);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return false for case-sensitive display mismatch")
    void shouldReturnFalseForCaseSensitiveDisplayMismatch() throws Exception {
      // GIVEN
      stubConcept(createCaseSensitiveConcept()); // SNOMED_CONCEPT_CASESENSITIVE
      CsValidateCodeParameter param = buildParamWithDisplay(CONCEPT_CODE, EN_TERM.toLowerCase());

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains(EN_TERM.toLowerCase());
    }

    @Test
    @DisplayName(
        "Should return true when initial character matches case-insensitively and remainder matches exactly")
    void shouldReturnTrueForInitialCharCaseInsensitiveDisplayMatch() throws Exception {
      // GIVEN
      // EN_TERM = "Diabetes mellitus"; with INITIAL_CHAR_CASE_INSENSITIVE, "d" matches "D"
      // and "iabetes mellitus" == "iabetes mellitus" exactly
      stubConcept(createInitialCharCaseInsensitiveConcept());
      CsValidateCodeParameter param = buildParamWithDisplay(CONCEPT_CODE, "diabetes mellitus");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return false when display does not match any designation")
    void shouldReturnFalseWhenDisplayDoesNotMatchAnyDesignation() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param = buildParamWithDisplay(CONCEPT_CODE, "Unknown term");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("Unknown term");
    }
  }

  @Nested
  @DisplayName("Display validation with language constraint tests")
  class DisplayValidationWithLanguageTest {

    @Test
    @DisplayName("Should return true when display matches the designation in the given language")
    void shouldReturnTrueWhenDisplayMatchesInGivenLanguage() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept()); // en-GB designation
      CsValidateCodeParameter param =
          buildParamWithDisplayAndLanguage(CONCEPT_CODE, EN_TERM, "en-GB");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return false when no designation exists for the given language")
    void shouldReturnFalseWhenNoDesignationExistsForGivenLanguage() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept()); // only en-GB designation, no de-DE
      CsValidateCodeParameter param =
          buildParamWithDisplayAndLanguage(CONCEPT_CODE, EN_TERM, "de-DE");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("de-DE");
    }

    @Test
    @DisplayName(
        "Should return false when display does not match the designation in the given language")
    void shouldReturnFalseWhenDisplayDoesNotMatchForGivenLanguage() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param =
          buildParamWithDisplayAndLanguage(CONCEPT_CODE, "Wrong display", "en-GB");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("Wrong display").contains("en-GB");
    }

    @Test
    @DisplayName("Should match en-GB designation when displayLanguage 'en' is expanded via BCP-47")
    void shouldMatchDesignationViaBcp47LanguageExpansion() throws Exception {
      // GIVEN
      // Concept has en-GB designation; "en" expands to {en, en-GB, en-US} via BCP-47 expansion
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param = buildParamWithDisplayAndLanguage(CONCEPT_CODE, EN_TERM, "en");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should match de-DE designation when displayLanguage 'de' is expanded via BCP-47")
    void shouldMatchGermanDesignationViaBaseLanguageExpansion() throws Exception {
      // GIVEN
      // Concept has de-DE designation; "de" expands to {de, de-DE, de-AT, de-CH} via BCP-47
      // expansion
      stubConcept(createBilingualConcept());
      CsValidateCodeParameter param = buildParamWithDisplayAndLanguage(CONCEPT_CODE, DE_TERM, "de");

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.isResult()).isTrue();
    }
  }

  @Nested
  @DisplayName("Display selection tests")
  class DisplaySelectionTest {

    @Test
    @DisplayName("Should return the English display when no language preference is given")
    void shouldReturnEnglishDisplayWhenNoLanguagePreferenceIsGiven() throws Exception {
      // GIVEN
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param = buildParam(CONCEPT_CODE);

      // WHEN
      ValidateCodeResult result = validateCodeOperation.validateCode(param, null);

      // THEN
      assertThat(result.getDisplay()).isEqualTo(EN_TERM);
    }

    @Test
    @DisplayName("Should return the German display when system default language is de-DE")
    void shouldReturnGermanDisplayWhenSystemDefaultIsDeDe() throws Exception {
      // GIVEN
      stubConcept(createBilingualConcept());
      CsValidateCodeParameter param = buildParam(CONCEPT_CODE);

      // WHEN
      ValidateCodeResult result =
          validateCodeOperation.validateCode(param, LanguageParameter.parse("de-DE"));

      // THEN
      assertThat(result.getDisplay()).isEqualTo(DE_TERM);
    }

    @Test
    @DisplayName(
        "Should fall back to release version default language when no matching designation is found")
    void shouldFallbackToReleaseVersionDefaultLanguageWhenNoMatchFound() throws Exception {
      // GIVEN
      // Concept has only an English (en-GB) designation; system default is de-DE — no de match,
      // so the operation falls back to the release version's default language (en for CORE module)
      stubConcept(createSimpleEnConcept());
      CsValidateCodeParameter param = buildParam(CONCEPT_CODE);

      // WHEN
      ValidateCodeResult result =
          validateCodeOperation.validateCode(param, LanguageParameter.parse("de-DE"));

      // THEN
      assertThat(result.getDisplay()).isEqualTo(EN_TERM);
    }
  }
}

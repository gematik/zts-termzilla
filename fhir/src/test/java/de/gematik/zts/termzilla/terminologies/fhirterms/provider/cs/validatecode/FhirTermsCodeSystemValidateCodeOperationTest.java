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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.validatecode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
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
 * Unit tests for {@link FhirTermsCodeSystemValidateCodeOperation}.
 *
 * <p>Verifies unsupported parameter handling, code lookup result states, display validation with
 * and without language constraints, case-sensitivity behaviour, and language-aware display
 * selection in the result.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FhirTermsCodeSystemValidateCodeOperation Tests")
class FhirTermsCodeSystemValidateCodeOperationTest {

  private static final String SCOPE = "fhir";
  private static final String SYSTEM = "http://example.org/CodeSystem/test";
  private static final String VERSION = "1.0.0";
  private static final String CS_LANGUAGE = "en";
  private static final String CODE_A1 = "A1";

  private static final String DESIGNATION_USAGE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/designation-usage";
  private static final String DISPLAY_USE_CODE = "display";

  @Mock private FhirTermsTerminologyManager terminologyManager;
  @Mock private FhirTermsInMemoryRepresentation inMemoryRepresentation;

  private FhirTermsCodeSystemValidateCodeOperation operation;

  @BeforeEach
  void setUp() {
    when(terminologyManager.getFhirTermsInMemoryRepresentation())
        .thenReturn(inMemoryRepresentation);
    operation = new FhirTermsCodeSystemValidateCodeOperation(terminologyManager);
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a base CodeSystem with URL, version, English language, and case-sensitive=true. */
  private static CodeSystem createCodeSystem() {
    CodeSystem cs = new CodeSystem();
    cs.setUrl(SYSTEM);
    cs.setVersion(VERSION);
    cs.setLanguage(CS_LANGUAGE);
    cs.setCaseSensitive(true);
    return cs;
  }

  /** Creates a concept with the given code and optional display. */
  private static CodeSystem.ConceptDefinitionComponent createConcept(String code, String display) {
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode(code);
    if (display != null) {
      concept.setDisplay(display);
    }
    return concept;
  }

  /** Creates a designation with the given language, use-system, use-code, and value. */
  private static CodeSystem.ConceptDefinitionDesignationComponent createDesignation(
      String language, String useSystem, String useCode, String value) {
    CodeSystem.ConceptDefinitionDesignationComponent designation =
        new CodeSystem.ConceptDefinitionDesignationComponent();
    if (language != null) {
      designation.setLanguage(language);
    }
    designation.setUse(new Coding(useSystem, useCode, null));
    designation.setValue(value);
    return designation;
  }

  private static CsValidateCodeParameter buildParam(String code) {
    return CsValidateCodeParameter.builder()
        .urlParam(new UriType(SYSTEM))
        .codeParam(new CodeType(code))
        .build();
  }

  private static CsValidateCodeParameter buildParamWithDisplay(String code, String display) {
    return CsValidateCodeParameter.builder()
        .urlParam(new UriType(SYSTEM))
        .codeParam(new CodeType(code))
        .displayParam(new StringType(display))
        .build();
  }

  private static CsValidateCodeParameter buildParamWithDisplayAndLanguage(
      String code, String display, String displayLanguage) {
    return CsValidateCodeParameter.builder()
        .urlParam(new UriType(SYSTEM))
        .codeParam(new CodeType(code))
        .displayParam(display != null ? new StringType(display) : null)
        .displayLanguageParam(new CodeType(displayLanguage))
        .build();
  }

  private void stubCodeSystem(CodeSystem codeSystem)
      throws UnsupportedTerminologyException, UnknownScopeException {
    doReturn(codeSystem)
        .when(inMemoryRepresentation)
        .getResourceByUrlAndVersion(
            CodeSystem.class, SCOPE, SYSTEM, null, ResourceRetrievalMode.FULL);
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
              .urlParam(new UriType(SYSTEM))
              .codeParam(new CodeType(CODE_A1))
              .dateParam(new DateTimeType("2024-01-01"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> operation.validateCode(SCOPE, param, LanguageParameter.parse(null)))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("date");
    }

    @Test
    @DisplayName("Should throw NotImplementedException when abstract parameter is set to false")
    void shouldThrowNotImplementedExceptionWhenAbstractParameterIsFalse() {
      // GIVEN
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(SYSTEM))
              .codeParam(new CodeType(CODE_A1))
              .abstractParam(new BooleanType(false))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> operation.validateCode(SCOPE, param, LanguageParameter.parse(null)))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("abstract");
    }

    @Test
    @DisplayName("Should not throw when abstract parameter is set to true")
    void shouldNotThrowWhenAbstractParameterIsTrue() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      stubCodeSystem(cs);
      CsValidateCodeParameter param =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(SYSTEM))
              .codeParam(new CodeType(CODE_A1))
              .abstractParam(new BooleanType(true))
              .build();

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(SCOPE, param, LanguageParameter.parse(null));

      // THEN
      assertThat(result).isNotNull();
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Code lookup")
  class CodeLookupTest {

    @Test
    @DisplayName("Should return result=false when code is not found in the CodeSystem")
    void shouldReturnFalseResultWhenCodeNotFound() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      stubCodeSystem(cs);
      CsValidateCodeParameter param = buildParam("UNKNOWN");

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(SCOPE, param, LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("UNKNOWN").contains(SYSTEM);
    }

    @Test
    @DisplayName("Should return result=true when code is found and no display is provided")
    void shouldReturnTrueResultWhenCodeIsFound() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      cs.addConcept(createConcept(CODE_A1, "Concept A"));
      stubCodeSystem(cs);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(SCOPE, buildParam(CODE_A1), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Display selection in result")
  class DisplaySelectionTest {

    @Test
    @DisplayName(
        "Should return concept display when language preference matches the CodeSystem language")
    void shouldReturnConceptDisplayWhenLanguageMatchesCsLanguage() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem(); // CS_LANGUAGE = "en"
      cs.addConcept(createConcept(CODE_A1, "Concept A"));
      stubCodeSystem(cs);

      // WHEN – systemDefaultLanguage matches CS language "en"
      ValidateCodeResult result =
          operation.validateCode(SCOPE, buildParam(CODE_A1), LanguageParameter.parse(CS_LANGUAGE));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo("Concept A");
    }

    @Test
    @DisplayName(
        "Should return display-use designation when concept has no primary display and language preference matches")
    void shouldReturnDisplayUseDesignationWhenConceptHasNoPrimaryDisplay() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, null);
      concept.addDesignation(
          createDesignation(
              CS_LANGUAGE, DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Explicit Display"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN – systemDefaultLanguage matches the designation language "en"
      ValidateCodeResult result =
          operation.validateCode(SCOPE, buildParam(CODE_A1), LanguageParameter.parse(CS_LANGUAGE));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo("Explicit Display");
    }

    @Test
    @DisplayName(
        "Should fall back to any display-use designation when no designation matches the language preference")
    void shouldFallbackToAnyDisplayUseDesignationWhenNoLanguageMatch() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem(); // CS_LANGUAGE = "en"
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, null);
      concept.addDesignation(
          createDesignation(
              "fr", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Désignation française"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN – systemDefaultLanguage "de" does not match available languages "en" (CS) or "fr"
      ValidateCodeResult result =
          operation.validateCode(SCOPE, buildParam(CODE_A1), LanguageParameter.parse("de"));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo("Désignation française");
    }

    @Test
    @DisplayName("Should fall back to any designation when no display-use designation exists")
    void shouldFallbackToAnyDesignationWhenNoDisplayUseDesignationExists() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, null);
      concept.addDesignation(
          createDesignation(
              CS_LANGUAGE, "http://snomed.info/sct", "900000000000013009", "Fully Specified Name"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN – no display-use designation available; any designation is used as last resort
      ValidateCodeResult result =
          operation.validateCode(SCOPE, buildParam(CODE_A1), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getDisplay()).isEqualTo("Fully Specified Name");
    }

    @Test
    @DisplayName("Should set a message when no designation at all is available for the concept")
    void shouldSetMessageWhenNeitherDisplayNorDesignationExists() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      cs.addConcept(createConcept(CODE_A1, null)); // no display, no designations
      stubCodeSystem(cs);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(SCOPE, buildParam(CODE_A1), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getMessage()).contains(CODE_A1);
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Display validation – without displayLanguage")
  class DisplayValidationWithoutLanguageTest {

    @Test
    @DisplayName("Should return result=true when provided display matches the concept display")
    void shouldReturnTrueWhenDisplayMatchesConceptDisplay() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      cs.setCaseSensitive(false);
      cs.addConcept(createConcept(CODE_A1, "Concept A"));
      stubCodeSystem(cs);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE, buildParamWithDisplay(CODE_A1, "Concept A"), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName("Should return result=true when provided display matches an explicit designation")
    void shouldReturnTrueWhenDisplayMatchesExplicitDesignation() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      cs.setCaseSensitive(false);
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, "Concept A");
      concept.addDesignation(
          createDesignation("de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Konzept A"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN – provide the German designation value
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE, buildParamWithDisplay(CODE_A1, "Konzept A"), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=true for case-insensitive match on a non-case-sensitive CodeSystem")
    void shouldReturnTrueForCaseInsensitiveMatchOnNonCaseSensitiveCodeSystem() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      cs.setCaseSensitive(false);
      cs.addConcept(createConcept(CODE_A1, "Concept A"));
      stubCodeSystem(cs);

      // WHEN – provide lower-cased display
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE, buildParamWithDisplay(CODE_A1, "concept a"), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=false and include case-sensitivity hint for wrong-case display on a case-sensitive CodeSystem")
    void shouldReturnFalseWithHintForWrongCaseOnCaseSensitiveCodeSystem() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      cs.setCaseSensitive(true);
      cs.addConcept(createConcept(CODE_A1, "Concept A"));
      stubCodeSystem(cs);

      // WHEN – provide lower-cased display that only differs in case
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE, buildParamWithDisplay(CODE_A1, "concept a"), LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).containsIgnoringCase("case-sensitive");
    }

    @Test
    @DisplayName(
        "Should return result=false without case-sensitivity hint for a mismatching display on a non-case-sensitive CodeSystem")
    void shouldReturnFalseWithoutHintForMismatchOnNonCaseSensitiveCodeSystem() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      cs.setCaseSensitive(false);
      cs.addConcept(createConcept(CODE_A1, "Concept A"));
      stubCodeSystem(cs);

      // WHEN – display that does not match at all
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE,
              buildParamWithDisplay(CODE_A1, "Totally Wrong"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).doesNotContainIgnoringCase("case-sensitive");
    }
  }

  // --------------------------------------------------------------------------

  @Nested
  @DisplayName("Display validation – with displayLanguage")
  class DisplayValidationWithLanguageTest {

    @Test
    @DisplayName(
        "Should return result=true when display matches a designation for the requested language")
    void shouldReturnTrueWhenDisplayMatchesDesignationForRequestedLanguage() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, "Concept A");
      concept.addDesignation(
          createDesignation("de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Konzept A"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE,
              buildParamWithDisplayAndLanguage(CODE_A1, "Konzept A", "de"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=false when display does not match the designation for the requested language")
    void shouldReturnFalseWhenDisplayMismatchForRequestedLanguage() throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, "Concept A");
      concept.addDesignation(
          createDesignation("de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Konzept A"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN – display differs from the "de" designation
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE,
              buildParamWithDisplayAndLanguage(CODE_A1, "Falsches Display", "de"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("Falsches Display");
    }

    @Test
    @DisplayName("Should return result=false when no designation exists for the requested language")
    void shouldReturnFalseWhenNoDesignationExistsForRequestedLanguage() throws Exception {
      // GIVEN – CS has "en" concept display and "fr" designation, no "de"
      CodeSystem cs = createCodeSystem(); // CS_LANGUAGE = "en"
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, "Concept A");
      concept.addDesignation(
          createDesignation("fr", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Concept français"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN – request "de" which is not available in any designation
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE,
              buildParamWithDisplayAndLanguage(CODE_A1, "Konzept A", "de"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("de").contains(CODE_A1);
    }

    @Test
    @DisplayName(
        "Should match designation via BCP 47 language expansion from base language to regional variant")
    void shouldMatchDesignationViaBcp47ExpansionFromBaseToRegionalVariant() throws Exception {
      // GIVEN – designation has "de-DE", request is "de" which expands to include "de-DE"
      CodeSystem cs = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, "Concept A");
      concept.addDesignation(
          createDesignation("de-DE", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Konzept A (DE)"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE,
              buildParamWithDisplayAndLanguage(CODE_A1, "Konzept A (DE)", "de"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName(
        "Should validate against concept display when displayLanguage matches the CodeSystem language")
    void shouldValidateAgainstConceptDisplayWhenDisplayLanguageMatchesCsLanguage()
        throws Exception {
      // GIVEN – concept display is added as a "en" designation by
      // collectDesignationsFromFhirConcept
      CodeSystem cs = createCodeSystem(); // CS_LANGUAGE = "en"
      cs.addConcept(createConcept(CODE_A1, "Concept A"));
      stubCodeSystem(cs);

      // WHEN – displayLanguage "en" matches CS language, so concept display is a candidate
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE,
              buildParamWithDisplayAndLanguage(CODE_A1, "Concept A", CS_LANGUAGE),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isTrue();
    }

    @Test
    @DisplayName(
        "Should include case-sensitivity hint when display has wrong case on a case-sensitive CodeSystem with displayLanguage")
    void shouldIncludeHintForWrongCaseWithDisplayLanguageOnCaseSensitiveCodeSystem()
        throws Exception {
      // GIVEN
      CodeSystem cs = createCodeSystem(); // caseSensitive = true
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE_A1, "Concept A");
      concept.addDesignation(
          createDesignation("de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Konzept A"));
      cs.addConcept(concept);
      stubCodeSystem(cs);

      // WHEN – correct language, but wrong case
      ValidateCodeResult result =
          operation.validateCode(
              SCOPE,
              buildParamWithDisplayAndLanguage(CODE_A1, "konzept a", "de"),
              LanguageParameter.parse(null));

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).containsIgnoringCase("case-sensitive");
    }
  }
}

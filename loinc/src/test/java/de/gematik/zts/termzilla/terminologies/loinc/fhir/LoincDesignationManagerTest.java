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
package de.gematik.zts.termzilla.terminologies.loinc.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.terminologies.common.Designation.DesignationSource;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link LoincDesignationManager}.
 *
 * <p>Verifies LOINC-specific behavior: en-US fallback, CS designation selection logic (only default
 * designation type), VS display use filtering, and default designation type resolution for
 * different LOINC concept kinds (Terms, Parts, Answers, Answer Lists).
 */
@DisplayName("LoincDesignationManager Tests")
class LoincDesignationManagerTest {

  private static final String DESIGNATION_USAGE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/designation-usage";
  private static final String DISPLAY_USE_CODE = "display";

  private static final String LOINC_SYSTEM = LoincConstants.TERMINOLOGY_CANONICAL_NAME;
  private static final String LONG_COMMON_NAME = LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME;
  private static final String SHORT_NAME = LoincConstants.LOINC_DESIGNATION_SHORT_NAME;
  private static final String PART_NAME = LoincConstants.LOINC_DESIGNATION_PART_NAME;
  private static final String DISPLAY_TEXT = LoincConstants.LOINC_DESIGNATION_DISPLAYTEXT;
  private static final String ANSWERLIST_NAME = LoincConstants.LOINC_DESIGNATION_ANSWERLIST_NAME;

  private LoincDesignationManager manager;

  @BeforeEach
  void setUp() {
    manager = new LoincDesignationManager(LONG_COMMON_NAME);
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a VS designation with HL7 display use coding. */
  private static LoincFhirDesignation createVsDisplayDesignation(String language, String value) {
    LoincFhirDesignation.LoincFhirDesignationBuilder<?, ?> builder =
        LoincFhirDesignation.builder()
            .source(DesignationSource.VS)
            .value(value)
            .use(new Coding(DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, null));

    if (language != null) {
      builder.language(new CodeType(language));
    }

    return builder.build();
  }

  /** Creates a CS designation with LOINC-native use coding. */
  private static LoincFhirDesignation createCsDesignation(
      String language, String value, String designationType) {
    LoincFhirDesignation.LoincFhirDesignationBuilder<?, ?> builder =
        LoincFhirDesignation.builder()
            .source(DesignationSource.CS)
            .value(value)
            .use(new Coding(LOINC_SYSTEM, designationType, null))
            .type(designationType);

    if (language != null) {
      builder.language(new CodeType(language));
    }

    return builder.build();
  }

  private static LanguageSelectionParameter lspWithDisplayLanguage(String language) {
    return LanguageSelectionParameter.builder()
        .displayLanguageParameter(LanguageParameter.parse(language))
        .build();
  }

  // ==========================================================================
  // Tests
  // ==========================================================================

  @Nested
  @DisplayName("getDisplay() - English Fallback Tests")
  class EnglishFallbackTests {

    @Test
    @DisplayName("Should fall back to en-US as last resort when all other parameters miss")
    void shouldFallBackToEnUsAsLastResort() {
      // GIVEN
      LoincFhirDesignation enUsDesignation =
          createCsDesignation("en-US", "Glucose [Mass/volume] in Blood", LONG_COMMON_NAME);
      manager.addDesignation(enUsDesignation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .acceptLanguageHeader(LanguageParameter.parse("fr-FR"))
              .systemDefaultLanguage(LanguageParameter.parse("ja-JP"))
              .vsLanguage(LanguageParameter.parse("es-ES"))
              .build();

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should fall back to en-US as the ultimate last resort")
          .isEqualTo(enUsDesignation);
    }

    @Test
    @DisplayName("Should fall back to en-US when all parameters are null")
    void shouldFallBackToEnUsWhenAllParametersNull() {
      // GIVEN
      LoincFhirDesignation enUsDesignation =
          createCsDesignation("en-US", "Glucose [Mass/volume] in Blood", LONG_COMMON_NAME);
      manager.addDesignation(enUsDesignation);
      LanguageSelectionParameter lsp = LanguageSelectionParameter.builder().build();

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should fall back to en-US when no selection parameters are set")
          .isEqualTo(enUsDesignation);
    }

    @Test
    @DisplayName("Should not fall through to en-US fallback when displayLanguage is set but misses")
    void shouldNotFallThroughToEnUsFallbackWhenDisplayLanguageMisses() {
      // GIVEN
      manager.addDesignation(
          createCsDesignation("en-US", "Glucose [Mass/volume] in Blood", LONG_COMMON_NAME));

      LanguageSelectionParameter lsp = lspWithDisplayLanguage("fr-FR");

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("displayLanguage is strict - must not fall through to en-US fallback")
          .isNull();
    }
  }

  @Nested
  @DisplayName("getDisplay() - CS Designation Selection Tests")
  class CsDesignationSelectionTests {

    @Test
    @DisplayName("Should only select designations matching the default designation type")
    void shouldOnlySelectDefaultDesignationType() {
      // GIVEN
      LoincFhirDesignation longCommonName =
          createCsDesignation("de-DE", "Glukose [Masse/Volumen] im Blut", LONG_COMMON_NAME);
      LoincFhirDesignation shortName = createCsDesignation("de-DE", "Glukose Bld-mCnc", SHORT_NAME);
      manager.addDesignation(shortName);
      manager.addDesignation(longCommonName);

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("Should select LONG_COMMON_NAME, not ShortName")
          .isEqualTo(longCommonName);
    }

    @Test
    @DisplayName(
        "Should not select CS designation with non-default type even if it is the only one")
    void shouldNotSelectNonDefaultType() {
      // GIVEN
      LoincFhirDesignation shortName = createCsDesignation("de-DE", "Glukose Bld-mCnc", SHORT_NAME);
      manager.addDesignation(shortName);

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("Non-default designation type should not be selected for display")
          .isNull();
    }

    @Test
    @DisplayName("Should not select CS designation with null type")
    void shouldNotSelectCsDesignationWithNullType() {
      // GIVEN
      LoincFhirDesignation noType =
          LoincFhirDesignation.builder()
              .source(DesignationSource.CS)
              .language(new CodeType("de-DE"))
              .value("No type")
              .use(new Coding(LOINC_SYSTEM, LONG_COMMON_NAME, null))
              .build();
      manager.addDesignation(noType);

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("CS designation without type should not be selected").isNull();
    }
  }

  @Nested
  @DisplayName("getDisplay() - VS Display Use Filter Tests")
  class VsDisplayUseFilterTests {

    @Test
    @DisplayName("Should select VS designation with correct display use coding")
    void shouldSelectVsDesignationWithDisplayUse() {
      // GIVEN
      LoincFhirDesignation vsDesignation = createVsDisplayDesignation("de-DE", "VS Display");
      manager.addDesignation(vsDesignation);

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("VS designation with display use should be selected")
          .isEqualTo(vsDesignation);
    }

    @Test
    @DisplayName("Should ignore VS designations without display use coding")
    void shouldIgnoreVsDesignationsWithoutDisplayUse() {
      // GIVEN
      LoincFhirDesignation vsNoUse =
          LoincFhirDesignation.builder()
              .source(DesignationSource.VS)
              .language(new CodeType("de-DE"))
              .value("VS without use")
              .build();
      manager.addDesignation(vsNoUse);

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("VS designation without display use should be ignored").isNull();
    }

    @Test
    @DisplayName("Should ignore VS designations with wrong use system")
    void shouldIgnoreVsDesignationsWithWrongUseSystem() {
      // GIVEN
      LoincFhirDesignation wrongSystem =
          LoincFhirDesignation.builder()
              .source(DesignationSource.VS)
              .language(new CodeType("de-DE"))
              .value("Wrong system")
              .use(new Coding("http://other-system.org", DISPLAY_USE_CODE, null))
              .build();
      manager.addDesignation(wrongSystem);

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("VS designation with wrong use system should be ignored").isNull();
    }

    @Test
    @DisplayName("Should ignore VS designations with wrong use code")
    void shouldIgnoreVsDesignationsWithWrongUseCode() {
      // GIVEN
      LoincFhirDesignation wrongCode =
          LoincFhirDesignation.builder()
              .source(DesignationSource.VS)
              .language(new CodeType("de-DE"))
              .value("Wrong code")
              .use(new Coding(DESIGNATION_USAGE_SYSTEM, "definition", null))
              .build();
      manager.addDesignation(wrongCode);

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("VS designation with wrong use code should be ignored").isNull();
    }

    @Test
    @DisplayName("Should prefer VS display designation over CS designation for same language")
    void shouldPreferVsOverCsForSameLanguage() {
      // GIVEN
      LoincFhirDesignation csDesignation =
          createCsDesignation("de-DE", "CS Display", LONG_COMMON_NAME);
      LoincFhirDesignation vsDesignation = createVsDisplayDesignation("de-DE", "VS Display");
      manager.addDesignation(csDesignation);
      manager.addDesignation(vsDesignation);

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("VS display designation should be preferred over CS designation")
          .isEqualTo(vsDesignation);
    }
  }

  @Nested
  @DisplayName("resolveDefaultDesignationType() - Default Designation Type Tests")
  class DefaultDesignationTypeTests {

    @Test
    @DisplayName("Should resolve LONG_COMMON_NAME for numeric LOINC term codes")
    void shouldResolveLongCommonNameForTerms() {
      // WHEN
      String result = LoincDesignationManager.resolveDefaultDesignationType("2345-7");

      // THEN
      assertThat(result)
          .as("Numeric LOINC codes should resolve to LONG_COMMON_NAME")
          .isEqualTo(LONG_COMMON_NAME);
    }

    @Test
    @DisplayName("Should resolve PartName for LP codes")
    void shouldResolvePartNameForLpCodes() {
      // WHEN
      String result = LoincDesignationManager.resolveDefaultDesignationType("LP14693-7");

      // THEN
      assertThat(result).as("LP codes should resolve to PartName").isEqualTo(PART_NAME);
    }

    @Test
    @DisplayName("Should resolve DisplayText for LA codes")
    void shouldResolveDisplayTextForLaCodes() {
      // WHEN
      String result = LoincDesignationManager.resolveDefaultDesignationType("LA6270-8");

      // THEN
      assertThat(result).as("LA codes should resolve to DisplayText").isEqualTo(DISPLAY_TEXT);
    }

    @Test
    @DisplayName("Should resolve AnswerListName for LL codes")
    void shouldResolveAnswerListNameForLlCodes() {
      // WHEN
      String result = LoincDesignationManager.resolveDefaultDesignationType("LL715-4");

      // THEN
      assertThat(result).as("LL codes should resolve to AnswerListName").isEqualTo(ANSWERLIST_NAME);
    }

    @Test
    @DisplayName("Should use Part-specific default type when manager is configured for Parts")
    void shouldSelectPartNameDesignationWhenManagerConfiguredForParts() {
      // GIVEN
      LoincDesignationManager partManager = new LoincDesignationManager(PART_NAME);
      LoincFhirDesignation partNameDesignation = createCsDesignation("en-US", "Glucose", PART_NAME);
      LoincFhirDesignation longCommonName =
          createCsDesignation("en-US", "Glucose [Mass/volume] in Blood", LONG_COMMON_NAME);
      partManager.addDesignation(longCommonName);
      partManager.addDesignation(partNameDesignation);

      // WHEN
      LoincFhirDesignation result = partManager.getDisplay(lspWithDisplayLanguage("en-US"));

      // THEN
      assertThat(result)
          .as("Part-configured manager should select PartName, not LONG_COMMON_NAME")
          .isEqualTo(partNameDesignation);
    }

    @Test
    @DisplayName(
        "Should use DisplayText-specific default type when manager is configured for Answers")
    void shouldSelectDisplayTextDesignationWhenManagerConfiguredForAnswers() {
      // GIVEN
      LoincDesignationManager answerManager = new LoincDesignationManager(DISPLAY_TEXT);
      LoincFhirDesignation displayText = createCsDesignation("en-US", "Never", DISPLAY_TEXT);
      LoincFhirDesignation longCommonName =
          createCsDesignation("en-US", "Never (answer)", LONG_COMMON_NAME);
      answerManager.addDesignation(longCommonName);
      answerManager.addDesignation(displayText);

      // WHEN
      LoincFhirDesignation result = answerManager.getDisplay(lspWithDisplayLanguage("en-US"));

      // THEN
      assertThat(result)
          .as("Answer-configured manager should select DisplayText, not LONG_COMMON_NAME")
          .isEqualTo(displayText);
    }
  }

  @Nested
  @DisplayName("getDisplay() - Combination Language Scenario Tests")
  class CombinationLanguageScenarioTests {

    @ParameterizedTest
    @DisplayName("Should return expected display for CS default-type designations and VS display")
    @CsvSource(
        nullValues = "NULL",
        delimiter = '|',
        value = {
          // systemDefault | acceptLanguage | displayLanguage | vsLanguage | expected
          // --- displayLanguage strict tests ---
          "de-DE | NULL | de-DE | NULL | CS default type (de-DE)",
          "de-DE | NULL | de-DE | de-DE | VS display (de-DE)",
          "de-DE | NULL | de-DE | en-US | CS default type (de-DE)",
          "de-DE | NULL | de-AT | en-US | NULL",
          "de-DE | NULL | en-US | NULL | CS default type (en-US)",
          "de-DE | NULL | fr-FR | NULL | NULL",
          // --- acceptLanguage tests ---
          "de-DE | de-DE | NULL | NULL | CS default type (de-DE)",
          "de-DE | de-DE | NULL | de-DE | VS display (de-DE)",
          "de-DE | en-US | NULL | NULL | CS default type (en-US)",
          "de-DE | en-US | NULL | en-US | VS display (en-US)",
          "de-DE | fr-FR | NULL | NULL | CS default type (de-DE)",
          // --- systemDefault tests ---
          "de-DE | NULL | NULL | NULL | CS default type (de-DE)",
          "de-DE | NULL | NULL | de-DE | VS display (de-DE)",
          "de-DE | NULL | NULL | en-US | CS default type (de-DE)",
          // --- vsLanguage tests ---
          "NULL | NULL | NULL | de-DE | VS display (de-DE)",
          "NULL | NULL | NULL | en-US | VS display (en-US)",
          "NULL | NULL | NULL | fr-FR | VS display (fr-FR)",
          // --- base language expansion ---
          "de | NULL | NULL | NULL | CS default type (de-DE)",
          "en | NULL | NULL | NULL | CS default type (en-US)",
          "NULL | de | NULL | NULL | CS default type (de-DE)",
          "NULL | en | NULL | NULL | CS default type (en-US)",
          // --- en-US fallback ---
          "NULL | NULL | NULL | NULL | CS default type (en-US)",
          "ja-JP | NULL | NULL | NULL | CS default type (en-US)",
          "ja-JP | NULL | NULL | es-ES | VS display (es-ES)",
        })
    void shouldReturnExpectedDisplay(
        String systemDefaultLanguage,
        String acceptLanguageHeader,
        String displayLanguageParameter,
        String vsLanguage,
        String expectedTerm) {
      // GIVEN
      manager.addDesignation(
          createCsDesignation("de-DE", "CS default type (de-DE)", LONG_COMMON_NAME));
      manager.addDesignation(
          createCsDesignation("de-DE", "CS non-default type (de-DE)", SHORT_NAME));
      manager.addDesignation(
          createCsDesignation("en-US", "CS default type (en-US)", LONG_COMMON_NAME));
      manager.addDesignation(
          createCsDesignation("en-US", "CS non-default type (en-US)", SHORT_NAME));

      if (vsLanguage != null) {
        manager.addDesignation(
            createVsDisplayDesignation(vsLanguage, "VS display (" + vsLanguage + ")"));
      }

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .systemDefaultLanguage(
                  systemDefaultLanguage != null
                      ? LanguageParameter.parse(systemDefaultLanguage)
                      : null)
              .acceptLanguageHeader(
                  acceptLanguageHeader != null
                      ? LanguageParameter.parse(acceptLanguageHeader)
                      : null)
              .displayLanguageParameter(
                  displayLanguageParameter != null
                      ? LanguageParameter.parse(displayLanguageParameter)
                      : null)
              .vsLanguage(vsLanguage != null ? LanguageParameter.parse(vsLanguage) : null)
              .build();

      // WHEN
      LoincFhirDesignation result = manager.getDisplay(lsp);

      // THEN
      if (expectedTerm == null) {
        assertThat(result).as("Expected no display designation to be found").isNull();
      } else {
        assertThat(result).as("Expected a designation to be found").isNotNull();
        assertThat(result.getValue())
            .as("Should have display: " + expectedTerm)
            .isEqualTo(expectedTerm);
      }
    }
  }

  @Nested
  @DisplayName("selectDesignations() - Designation Selection Tests")
  class SelectDesignationsTests {

    @Test
    @DisplayName("Should return all designations when parameter list is empty")
    void shouldReturnAllWhenParameterListIsEmpty() {
      // GIVEN
      LoincFhirDesignation designation1 =
          createCsDesignation("en-US", "Long Name", LONG_COMMON_NAME);
      LoincFhirDesignation designation2 = createCsDesignation("en-US", "Short Name", SHORT_NAME);
      manager.addDesignation(designation1);
      manager.addDesignation(designation2);

      // WHEN
      Set<LoincFhirDesignation> result = manager.selectDesignations(List.of());

      // THEN
      assertThat(result)
          .as("Empty parameter list should return all designations")
          .containsExactlyInAnyOrder(designation1, designation2);
    }

    @Test
    @DisplayName("Should select designations by use system|code (LOINC system)")
    void shouldSelectByLoincSystemAndCode() {
      // GIVEN
      LoincFhirDesignation longCommonName =
          createCsDesignation("en-US", "Long Name", LONG_COMMON_NAME);
      LoincFhirDesignation shortName = createCsDesignation("en-US", "Short Name", SHORT_NAME);
      manager.addDesignation(longCommonName);
      manager.addDesignation(shortName);

      // WHEN
      Set<LoincFhirDesignation> result =
          manager.selectDesignations(List.of(LOINC_SYSTEM + "|" + LONG_COMMON_NAME));

      // THEN
      assertThat(result)
          .as("Should select only LONG_COMMON_NAME designation via system|code")
          .containsExactly(longCommonName);
    }

    @Test
    @DisplayName("Should select designations by language")
    void shouldSelectByLanguage() {
      // GIVEN
      LoincFhirDesignation enUs = createCsDesignation("en-US", "English Name", LONG_COMMON_NAME);
      LoincFhirDesignation deDe = createCsDesignation("de-DE", "German Name", LONG_COMMON_NAME);
      manager.addDesignation(enUs);
      manager.addDesignation(deDe);

      // WHEN
      Set<LoincFhirDesignation> result = manager.selectDesignations(List.of("de-DE"));

      // THEN
      assertThat(result)
          .as("Should select only designations matching language de-DE")
          .containsExactly(deDe);
    }
  }

  @Nested
  @DisplayName("toDesignationComponent() - FHIR Conversion Tests")
  class ToDesignationComponentTests {

    @Test
    @DisplayName("Should correctly convert LoincFhirDesignation to FHIR component")
    void shouldConvertToFhirComponent() {
      // GIVEN
      Coding use = new Coding(LOINC_SYSTEM, LONG_COMMON_NAME, null);
      LoincFhirDesignation designation =
          LoincFhirDesignation.builder()
              .source(DesignationSource.CS)
              .language(new CodeType("en-US"))
              .use(use)
              .value("Glucose [Mass/volume] in Blood")
              .type(LONG_COMMON_NAME)
              .build();

      // WHEN
      org.hl7.fhir.r4.model.ValueSet.ConceptReferenceDesignationComponent result =
          LoincDesignationManager.toDesignationComponent(designation);

      // THEN
      assertThat(result.getLanguage()).as("Language should be en-US").isEqualTo("en-US");
      assertThat(result.getUse()).as("Use coding should be preserved").isEqualTo(use);
      assertThat(result.getValue())
          .as("Value should be preserved")
          .isEqualTo("Glucose [Mass/volume] in Blood");
    }

    @Test
    @DisplayName("Should handle null language in designation conversion")
    void shouldHandleNullLanguage() {
      // GIVEN
      LoincFhirDesignation designation =
          LoincFhirDesignation.builder()
              .source(DesignationSource.CS)
              .use(new Coding(LOINC_SYSTEM, LONG_COMMON_NAME, null))
              .value("Some value")
              .type(LONG_COMMON_NAME)
              .build();

      // WHEN
      org.hl7.fhir.r4.model.ValueSet.ConceptReferenceDesignationComponent result =
          LoincDesignationManager.toDesignationComponent(designation);

      // THEN
      assertThat(result.getLanguage()).as("Language should be null").isNull();
      assertThat(result.getValue()).as("Value should be preserved").isEqualTo("Some value");
    }
  }
}

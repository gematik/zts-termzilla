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
package de.gematik.zts.termzilla.terminologies.fhirterms.helper;

import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_LANGUAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.gematik.zts.termzilla.terminologies.common.Designation;
import de.gematik.zts.termzilla.terminologies.common.Designation.DesignationSource;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link FhirDesignationManager}.
 *
 * <p>Verifies FHIR-specific behavior: the four-level display fallback chain ({@code
 * getDisplayFallback()}) and the full combination language scenario matrix. Generic base class
 * behavior (priority chain, language matching, source preference, etc.) is covered by {@code
 * DesignationManagerTest} in the {@code termzilla-common} module.
 */
@DisplayName("FhirDesignationManager Tests")
class FhirDesignationManagerTest {

  private static final String DESIGNATION_USAGE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/designation-usage";
  private static final String DISPLAY_USE_CODE = "display";
  private static final String CS_LANGUAGE = "en";
  private static final String VS_LANGUAGE = "de";

  private FhirDesignationManager manager;

  @BeforeEach
  void setUp() {
    manager = new FhirDesignationManager();
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  private static FhirDesignation createDesignation(
      DesignationSource source, String language, String value, String useCode) {
    FhirDesignation.FhirDesignationBuilder<?, ?> builder =
        FhirDesignation.builder().source(source).value(value);

    if (language != null) {
      builder.language(new CodeType(language));
    }

    if (useCode != null) {
      builder.use(new Coding(DESIGNATION_USAGE_SYSTEM, useCode, null));
    }

    return builder.build();
  }

  private static CodeSystem.ConceptDefinitionComponent createConcept(String code, String display) {
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode(code);
    if (display != null) {
      concept.setDisplay(display);
    }
    return concept;
  }

  private static CodeSystem.ConceptDefinitionDesignationComponent createCsDesignation(
      String language, String useCode, String useSystem, String value) {
    CodeSystem.ConceptDefinitionDesignationComponent d =
        new CodeSystem.ConceptDefinitionDesignationComponent();
    if (language != null) {
      d.setLanguage(language);
    }
    if (useCode != null) {
      d.setUse(new Coding(useSystem, useCode, null));
    }
    d.setValue(value);
    return d;
  }

  private static ValueSet.ConceptReferenceComponent createVsConceptReference(
      String display, String language) {
    ValueSet.ConceptReferenceComponent ref = new ValueSet.ConceptReferenceComponent();
    if (display != null) {
      ref.setDisplay(display);
    }
    ref.setUserData(USERDATA_LANGUAGE, language);
    return ref;
  }

  private static ValueSet.ConceptReferenceDesignationComponent createVsDesignation(
      String language, String value) {
    ValueSet.ConceptReferenceDesignationComponent d =
        new ValueSet.ConceptReferenceDesignationComponent();
    if (language != null) {
      d.setLanguage(language);
    }
    d.setValue(value);
    return d;
  }

  private static String languageOf(Designation d) {
    return d.getLanguage() != null ? d.getLanguage().getValue() : null;
  }

  // ==========================================================================
  // Tests
  // ==========================================================================

  @Nested
  @DisplayName("getDisplayFallback() Tests")
  class GetDisplayFallbackTests {

    private static final LanguageSelectionParameter EMPTY_LSP =
        LanguageSelectionParameter.builder().build();

    // --- Priority tests ---

    @Test
    @DisplayName("Should prefer VS display-use over CS display-use (level 1 vs 2)")
    void shouldPreferVsDisplayUseOverCsDisplayUse() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en", "CS display", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "de", "VS display", DISPLAY_USE_CODE));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("VS display");
      assertThat(result.getSource()).isEqualTo(DesignationSource.VS);
    }

    @Test
    @DisplayName("Should return CS display-use when no VS display-use exists (level 2)")
    void shouldReturnCsDisplayUseWhenNoVsDisplayUse() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "de-DE", "CS display", DISPLAY_USE_CODE));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("CS display");
      assertThat(result.getSource()).isEqualTo(DesignationSource.CS);
    }

    @Test
    @DisplayName("Should return any VS designation when no display-use exists (level 3)")
    void shouldReturnAnyVsDesignationWhenNoDisplayUseExists() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en", "CS other", "other-use"));
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "de", "VS other", "other-use"));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("VS other");
      assertThat(result.getSource()).isEqualTo(DesignationSource.VS);
    }

    @Test
    @DisplayName("Should return any CS designation when no VS designation exists (level 4)")
    void shouldReturnAnyCsDesignationWhenNoVsDesignationExists() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "fr", "CS other", "other-use"));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("CS other");
      assertThat(result.getSource()).isEqualTo(DesignationSource.CS);
    }

    @Test
    @DisplayName("Should return null when no designations exist at all")
    void shouldReturnNullWhenNoDesignationsExist() {
      // GIVEN — empty manager

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNull();
    }

    // --- Cross-level priority tests ---

    @Test
    @DisplayName("Should prefer display-use over non-display-use within same source")
    void shouldPreferDisplayUseOverNonDisplayUseWithinSameSource() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "de", "VS synonym", "synonym"));
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "en", "VS display", DISPLAY_USE_CODE));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("VS display");
    }

    @Test
    @DisplayName("Should prefer VS display-use over CS non-display-use")
    void shouldPreferVsDisplayUseOverCsNonDisplayUse() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en", "CS synonym", "synonym"));
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "de", "VS display", DISPLAY_USE_CODE));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("VS display");
    }

    @Test
    @DisplayName("Should prefer CS display-use over VS non-display-use")
    void shouldPreferCsDisplayUseOverVsNonDisplayUse() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en", "CS display", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "de", "VS synonym", "synonym"));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("CS display");
    }

    @Test
    @DisplayName("Should prefer VS non-display-use over CS non-display-use")
    void shouldPreferVsNonDisplayUseOverCsNonDisplayUse() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en", "CS synonym", "synonym"));
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "de", "VS synonym", "synonym"));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("VS synonym");
      assertThat(result.getSource()).isEqualTo(DesignationSource.VS);
    }

    // --- Edge case tests ---

    @Test
    @DisplayName("Should not treat designation without use coding as display-use")
    void shouldNotTreatDesignationWithoutUseCodingAsDisplayUse() {
      // GIVEN
      manager.addDesignation(createDesignation(DesignationSource.VS, "de", "VS no use", null));
      manager.addDesignation(createDesignation(DesignationSource.CS, "en", "CS no use", null));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN — falls through levels 1+2 (no display-use), matches level 3 (any VS)
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("VS no use");
      assertThat(result.getSource()).isEqualTo(DesignationSource.VS);
    }

    @Test
    @DisplayName("Should find fallback regardless of language")
    void shouldFindFallbackRegardlessOfLanguage() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "fr-FR", "CS display fr", DISPLAY_USE_CODE));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("CS display fr");
    }

    @Test
    @DisplayName("Should return first designation when multiple exist at same fallback level")
    void shouldReturnFirstDesignationAtSameFallbackLevel() {
      // GIVEN
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "de", "VS display de", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(DesignationSource.VS, "en", "VS display en", DISPLAY_USE_CODE));

      // WHEN
      FhirDesignation result = manager.getDisplay(EMPTY_LSP);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getValue()).isEqualTo("VS display de");
    }
  }

  @Nested
  @DisplayName("getDisplay() - Combination Language Scenario Tests")
  class CombinationLanguageScenarioTests {

    @ParameterizedTest
    @DisplayName("Should return expected display")
    @CsvSource(
        nullValues = "NULL",
        delimiter = '|',
        value = {
          "de-DE | NULL | NULL | NULL | CS use=display Term (de-DE)",
          "de-DE | NULL | NULL | de-DE | VS use=display Term (de-DE)",
          "de-DE | NULL | NULL | de-AT | CS use=display Term (de-DE)",
          "de-DE | NULL | NULL | de-CH | CS use=display Term (de-DE)",
          "de-DE | NULL | NULL | de | VS use=display Term (de)",
          "de-DE | NULL | NULL | en-US | CS use=display Term (de-DE)",
          "de-DE | NULL | NULL | en-GB | CS use=display Term (de-DE)",
          "de-DE | NULL | NULL | en | CS use=display Term (de-DE)",
          "de-DE | NULL | NULL | es | CS use=display Term (de-DE)",
          "de-DE | NULL | NULL | es-ES | CS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | NULL | CS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | de-DE | VS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | de-AT | CS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | de-CH | CS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | de | VS use=display Term (de)",
          "de-DE | de-DE | NULL | en-US | CS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | en-GB | CS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | en | CS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | es | CS use=display Term (de-DE)",
          "de-DE | de-DE | NULL | es-ES | CS use=display Term (de-DE)",
          "de-DE | de-AT | NULL | NULL | CS use=display Term (de-AT)",
          "de-DE | de-AT | NULL | de-DE | CS use=display Term (de-AT)",
          "de-DE | de-AT | NULL | de-AT | VS use=display Term (de-AT)",
          "de-DE | de-AT | NULL | de-CH | CS use=display Term (de-AT)",
          "de-DE | de-AT | NULL | de | VS use=display Term (de)",
          "de-DE | de-AT | NULL | en-US | CS use=display Term (de-AT)",
          "de-DE | de-AT | NULL | en-GB | CS use=display Term (de-AT)",
          "de-DE | de-AT | NULL | en | CS use=display Term (de-AT)",
          "de-DE | de-AT | NULL | es | CS use=display Term (de-AT)",
          "de-DE | de-AT | NULL | es-ES | CS use=display Term (de-AT)",
          "de-DE | de | NULL | NULL | CS use=display Term (de)",
          "de-DE | de | NULL | de-DE | VS use=display Term (de-DE)",
          "de-DE | de | NULL | de-AT | VS use=display Term (de-AT)",
          "de-DE | de | NULL | de-CH | VS use=display Term (de-CH)",
          "de-DE | de | NULL | de | VS use=display Term (de)",
          "de-DE | de | NULL | en-US | CS use=display Term (de)",
          "de-DE | de | NULL | en-GB | CS use=display Term (de)",
          "de-DE | de | NULL | en | CS use=display Term (de)",
          "de-DE | de | NULL | es | CS use=display Term (de)",
          "de-DE | de | NULL | es-ES | CS use=display Term (de)",
          "de-DE | en-US | NULL | NULL | CS use=display Term (en-US)",
          "de-DE | en-US | NULL | de-DE | CS use=display Term (en-US)",
          "de-DE | en-US | NULL | de-AT | CS use=display Term (en-US)",
          "de-DE | en-US | NULL | de-CH | CS use=display Term (en-US)",
          "de-DE | en-US | NULL | de | CS use=display Term (en-US)",
          "de-DE | en-US | NULL | en-US | VS use=display Term (en-US)",
          "de-DE | en-US | NULL | en-GB | CS use=display Term (en-US)",
          "de-DE | en-US | NULL | en | VS use=display Term (en)",
          "de-DE | en-US | NULL | es | CS use=display Term (en-US)",
          "de-DE | en-US | NULL | es-ES | CS use=display Term (en-US)",
          "de-DE | en-GB | NULL | NULL | CS use=display Term (en-GB)",
          "de-DE | en-GB | NULL | de-DE | CS use=display Term (en-GB)",
          "de-DE | en-GB | NULL | de-AT | CS use=display Term (en-GB)",
          "de-DE | en-GB | NULL | de-CH | CS use=display Term (en-GB)",
          "de-DE | en-GB | NULL | de | CS use=display Term (en-GB)",
          "de-DE | en-GB | NULL | en-US | CS use=display Term (en-GB)",
          "de-DE | en-GB | NULL | en-GB | VS use=display Term (en-GB)",
          "de-DE | en-GB | NULL | en | VS use=display Term (en)",
          "de-DE | en-GB | NULL | es | CS use=display Term (en-GB)",
          "de-DE | en-GB | NULL | es-ES | CS use=display Term (en-GB)",
          "de-DE | en | NULL | NULL | CS use=display Term (en)",
          "de-DE | en | NULL | de-DE | CS use=display Term (en)",
          "de-DE | en | NULL | de-AT | CS use=display Term (en)",
          "de-DE | en | NULL | de-CH | CS use=display Term (en)",
          "de-DE | en | NULL | de | CS use=display Term (en)",
          "de-DE | en | NULL | en-US | VS use=display Term (en-US)",
          "de-DE | en | NULL | en-GB | VS use=display Term (en-GB)",
          "de-DE | en | NULL | en | VS use=display Term (en)",
          "de-DE | en | NULL | es | CS use=display Term (en)",
          "de-DE | en | NULL | es-ES | CS use=display Term (en)",
          "de-DE | es | NULL | NULL | VS use=display Term (null)",
          "de-DE | es | NULL | de-DE | VS use=display Term (de-DE)",
          "de-DE | es | NULL | de-AT | CS use=display Term (de-DE)",
          "de-DE | es | NULL | de-CH | CS use=display Term (de-DE)",
          "de-DE | es | NULL | de | VS use=display Term (de)",
          "de-DE | es | NULL | en-US | CS use=display Term (de-DE)",
          "de-DE | es | NULL | en-GB | CS use=display Term (de-DE)",
          "de-DE | es | NULL | en | CS use=display Term (de-DE)",
          "de-DE | es | NULL | es | VS use=display Term (es)",
          "de-DE | es | NULL | es-ES | VS use=display Term (es-ES)",
          "de-DE | NULL | de-DE | NULL | CS use=display Term (de-DE)",
          "de-DE | NULL | de-DE | de-DE | VS use=display Term (de-DE)",
          "de-DE | NULL | de-DE | de-AT | CS use=display Term (de-DE)",
          "de-DE | NULL | de-DE | de-CH | CS use=display Term (de-DE)",
          "de-DE | NULL | de-DE | de | VS use=display Term (de)",
          "de-DE | NULL | de-DE | en-US | CS use=display Term (de-DE)",
          "de-DE | NULL | de-DE | en-GB | CS use=display Term (de-DE)",
          "de-DE | NULL | de-DE | en | CS use=display Term (de-DE)",
          "de-DE | NULL | de-DE | es | CS use=display Term (de-DE)",
          "de-DE | NULL | de-DE | es-ES | CS use=display Term (de-DE)",
          "de-DE | NULL | de-AT | NULL | CS use=display Term (de-AT)",
          "de-DE | NULL | de-AT | de-DE | CS use=display Term (de-AT)",
          "de-DE | NULL | de-AT | de-AT | VS use=display Term (de-AT)",
          "de-DE | NULL | de-AT | de-CH | CS use=display Term (de-AT)",
          "de-DE | NULL | de-AT | de | VS use=display Term (de)",
          "de-DE | NULL | de-AT | en-US | CS use=display Term (de-AT)",
          "de-DE | NULL | de-AT | en-GB | CS use=display Term (de-AT)",
          "de-DE | NULL | de-AT | en | CS use=display Term (de-AT)",
          "de-DE | NULL | de-AT | es | CS use=display Term (de-AT)",
          "de-DE | NULL | de-AT | es-ES | CS use=display Term (de-AT)",
          "de-DE | NULL | de | NULL | CS use=display Term (de)",
          "de-DE | NULL | de | de-DE | VS use=display Term (de-DE)",
          "de-DE | NULL | de | de-AT | VS use=display Term (de-AT)",
          "de-DE | NULL | de | de-CH | VS use=display Term (de-CH)",
          "de-DE | NULL | de | de | VS use=display Term (de)",
          "de-DE | NULL | de | en-US | CS use=display Term (de)",
          "de-DE | NULL | de | en-GB | CS use=display Term (de)",
          "de-DE | NULL | de | en | CS use=display Term (de)",
          "de-DE | NULL | de | es | CS use=display Term (de)",
          "de-DE | NULL | de | es-ES | CS use=display Term (de)",
          "de-DE | NULL | es | NULL | VS use=display Term (null)",
          "de-DE | NULL | es | de-DE | NULL",
          "de-DE | NULL | es | de-AT | NULL",
          "de-DE | NULL | es | de-CH | NULL",
          "de-DE | NULL | es | de | NULL",
          "de-DE | NULL | es | en-US | NULL",
          "de-DE | NULL | es | en-GB | NULL",
          "de-DE | NULL | es | en | NULL",
          "de-DE | NULL | es | es | VS use=display Term (es)",
          "de-DE | NULL | es | es-ES | VS use=display Term (es-ES)",
          "es-ES | NULL | NULL | NULL | VS use=display Term (null)",
          "es-ES | NULL | NULL | de-DE | VS use=display Term (de-DE)",
          "es-ES | NULL | NULL | de-AT | VS use=display Term (de-AT)",
          "es-ES | NULL | NULL | de-CH | VS use=display Term (de-CH)",
          "es-ES | NULL | NULL | de | VS use=display Term (de)",
          "es-ES | NULL | NULL | en-US | VS use=display Term (en-US)",
          "es-ES | NULL | NULL | en-GB | VS use=display Term (en-GB)",
          "es-ES | NULL | NULL | en | VS use=display Term (en)",
          "es-ES | NULL | NULL | es | VS use=display Term (es)",
          "es-ES | NULL | NULL | es-ES | VS use=display Term (es-ES)",
        })
    void shouldRespectQualityOrder(
        String systemDefaultLanguage,
        String acceptLanguageHeader,
        String displayLanguageParameter,
        String vsLanguage,
        String expectedTerm) {
      // GIVEN

      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "de", "CS use=display Term (de)", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "de", "CS use=other-use Term (de)", "other-use"));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "de", "CS use=NULL Term (de)", null));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "de-DE", "CS use=display Term (de-DE)", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "de-DE", "CS use=other-use Term (de-DE)", "other-use"));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "de-DE", "CS use=NULL Term (de-DE)", null));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "de-AT", "CS use=display Term (de-AT)", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "de-AT", "CS use=other-use Term (de-AT)", "other-use"));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "de-AT", "CS use=NULL Term (de-AT)", null));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "de-CH", "CS use=display Term (de-CH)", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "de-CH", "CS use=other-use Term (de-CH)", "other-use"));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "de-CH", "CS use=NULL Term (de-CH)", null));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "en", "CS use=display Term (en)", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en", "CS use=other-use Term (en)", "other-use"));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en", "CS use=NULL Term (en)", null));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "en-US", "CS use=display Term (en-US)", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "en-US", "CS use=other-use Term (en-US)", "other-use"));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en-US", "CS use=NULL Term (en-US)", null));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "en-GB", "CS use=display Term (en-GB)", DISPLAY_USE_CODE));
      manager.addDesignation(
          createDesignation(
              DesignationSource.CS, "en-GB", "CS use=other-use Term (en-GB)", "other-use"));
      manager.addDesignation(
          createDesignation(DesignationSource.CS, "en-GB", "CS use=NULL Term (de-AT)", null));

      manager.addDesignation(
          createDesignation(
              DesignationSource.VS,
              vsLanguage,
              "VS use=display Term (" + vsLanguage + ")",
              DISPLAY_USE_CODE));

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
      FhirDesignation result = manager.getDisplay(lsp);

      // THEN
      if (expectedTerm == null) {
        assertThat(result).as("Expected no display designation to be found").isNull();
      } else {
        assertThat(result.getValue())
            .as("Should have display: " + expectedTerm)
            .isEqualTo(expectedTerm);
      }
    }
  }

  @Nested
  @DisplayName("collectDesignationsFromFhirConcept() Tests")
  class CollectDesignationsFromFhirConceptTests {

    @Test
    @DisplayName("Should add CS display as designation with CS language")
    void shouldAddCsDisplayAsDesignation() {
      // GIVEN
      CodeSystem.ConceptDefinitionComponent concept = createConcept("A1", "Display A1");

      // WHEN
      manager.collectDesignationsFromFhirConcept(concept, CS_LANGUAGE);
      List<FhirDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      FhirDesignation designation = result.getFirst();
      assertThat(designation.getSource()).isEqualTo(DesignationSource.CS);
      assertThat(languageOf(designation)).isEqualTo(CS_LANGUAGE);
      assertThat(designation.getUse()).isNull();
      assertThat(designation.getValue()).isEqualTo("Display A1");
    }

    @Test
    @DisplayName("Should not add CS display designation when concept has no display")
    void shouldNotAddCsDisplayWhenConceptHasNoDisplay() {
      // GIVEN
      CodeSystem.ConceptDefinitionComponent concept = createConcept("A1", null);

      // WHEN
      manager.collectDesignationsFromFhirConcept(concept, CS_LANGUAGE);
      List<FhirDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should add all CS concept designations")
    void shouldAddAllCsConceptDesignations() {
      // GIVEN
      CodeSystem.ConceptDefinitionComponent concept = createConcept("A1", null);
      concept.addDesignation(
          createCsDesignation("de", DISPLAY_USE_CODE, DESIGNATION_USAGE_SYSTEM, "Anzeige A1"));
      concept.addDesignation(
          createCsDesignation("fr", "synonym", DESIGNATION_USAGE_SYSTEM, "Affichage A1"));

      // WHEN
      manager.collectDesignationsFromFhirConcept(concept, CS_LANGUAGE);
      List<FhirDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(2)
          .extracting(
              Designation::getSource, FhirDesignationManagerTest::languageOf, Designation::getValue)
          .containsExactly(
              tuple(DesignationSource.CS, "de", "Anzeige A1"),
              tuple(DesignationSource.CS, "fr", "Affichage A1"));
    }

    @Test
    @DisplayName("Should use CS language as fallback when CS designation has no explicit language")
    void shouldUseCsLanguageFallbackForDesignationWithoutLanguage() {
      // GIVEN
      CodeSystem.ConceptDefinitionComponent concept = createConcept("A1", null);
      concept.addDesignation(
          createCsDesignation(null, DISPLAY_USE_CODE, DESIGNATION_USAGE_SYSTEM, "Anzeige A1"));

      // WHEN
      manager.collectDesignationsFromFhirConcept(concept, CS_LANGUAGE);
      List<FhirDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      assertThat(languageOf(result.getFirst())).isEqualTo(CS_LANGUAGE);
    }

    @Test
    @DisplayName("Should preserve the original use coding of CS designations")
    void shouldPreserveOriginalUseCodingOfCsDesignations() {
      // GIVEN
      String customUseSystem = "http://example.org/use";
      CodeSystem.ConceptDefinitionComponent concept = createConcept("A1", null);
      concept.addDesignation(createCsDesignation("de", "preferred", customUseSystem, "Bevorzugt"));

      // WHEN
      manager.collectDesignationsFromFhirConcept(concept, CS_LANGUAGE);
      List<FhirDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      FhirDesignation designation = result.getFirst();
      assertThat(designation.getUse().getSystem()).isEqualTo(customUseSystem);
      assertThat(designation.getUse().getCode()).isEqualTo("preferred");
    }
  }

  @Nested
  @DisplayName("collectDesignations() Combined Source Tests")
  class CollectDesignationsCombinedTests {

    @Test
    @DisplayName("Should collect designations from both CS and VS sources in correct order")
    void shouldCollectFromBothSourcesInCorrectOrder() {
      // GIVEN
      CodeSystem.ConceptDefinitionComponent concept = createConcept("A1", "CS Display");
      concept.addDesignation(
          createCsDesignation("de", DISPLAY_USE_CODE, DESIGNATION_USAGE_SYSTEM, "CS Designation"));

      ValueSet.ConceptReferenceComponent vsRef =
          createVsConceptReference("VS Display", VS_LANGUAGE);
      vsRef.addDesignation(createVsDesignation("de", "VS Designation"));

      // WHEN
      manager.collectDesignationsFromFhirConcept(concept, CS_LANGUAGE);
      manager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<FhirDesignation> result = manager.getFullDesignationList();

      // THEN — order: CS display, CS designations, VS display, VS designations
      assertThat(result)
          .hasSize(4)
          .extracting(Designation::getSource, Designation::getValue)
          .containsExactly(
              tuple(DesignationSource.CS, "CS Display"),
              tuple(DesignationSource.CS, "CS Designation"),
              tuple(DesignationSource.VS, "VS Display"),
              tuple(DesignationSource.VS, "VS Designation"));
    }

    @Test
    @DisplayName("Should return empty manager when concept has no display and no references exist")
    void shouldReturnEmptyManagerForConceptWithoutDisplayAndNoReferences() {
      // GIVEN
      CodeSystem.ConceptDefinitionComponent concept = createConcept("A1", null);

      // WHEN
      manager.collectDesignationsFromFhirConcept(concept, CS_LANGUAGE);
      manager.collectDesignationsFromConceptReferenceComponents(List.of());
      List<FhirDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }
  }
}
